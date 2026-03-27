package com.potomushto.statik.cms

import com.potomushto.statik.config.CmsGitConfig
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.PushResult
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.nio.file.Files
import java.nio.file.Path

class GitSyncService(
    private val siteRoot: Path,
    private val config: CmsGitConfig
) {
    fun status(): CmsGitStatus {
        val repository = openRepository()
        return if (repository == null) {
            CmsGitStatus(
                available = false,
                enabled = config.enabled
            )
        } else {
            repository.use {
                CmsGitStatus(
                    available = true,
                    enabled = config.enabled,
                    repoRoot = it.workTree.absolutePath,
                    branch = runCatching { it.branch }.getOrNull(),
                    remote = config.remote,
                    remoteUrl = it.config.getString("remote", config.remote, "url")
                )
            }
        }
    }

    fun sync(sourcePaths: List<String>, commitMessage: String?, push: Boolean?, accessToken: String? = null): GitSyncOutcome {
        require(config.enabled) { "CMS git sync is disabled in config.json" }
        if (sourcePaths.isEmpty()) {
            return GitSyncOutcome(
                committed = false,
                commitId = null,
                message = "No CMS changes to sync.",
                pushAttempted = false,
                pushSucceeded = false,
                files = emptyList()
            )
        }

        val repository = openRepository()
            ?: throw IllegalStateException("No git repository found for CMS sync under $siteRoot")

        repository.use { repo ->
            Git(repo).use { git ->
                val repoRoot = repo.workTree.toPath().toAbsolutePath().normalize()
                val relativePaths = sourcePaths.map { toRepoRelativePath(repoRoot, it) }
                ensureNoUnrelatedStagedChanges(git, relativePaths)

                relativePaths.forEach { relativePath ->
                    val absolutePath = repoRoot.resolve(relativePath)
                    if (Files.exists(absolutePath)) {
                        git.add().addFilepattern(relativePath).call()
                    } else {
                        git.rm().addFilepattern(relativePath).call()
                    }
                }

                val stagedChanges = currentIndexedChanges(git).filter { it in relativePaths.toSet() }
                if (stagedChanges.isEmpty()) {
                    return GitSyncOutcome(
                        committed = false,
                        commitId = null,
                        message = "No git changes detected for CMS files.",
                        pushAttempted = false,
                        pushSucceeded = false,
                        files = relativePaths
                    )
                }

                val commit = git.commit().apply {
                    setMessage(commitMessage ?: defaultCommitMessage(relativePaths))
                    if (!config.authorName.isNullOrBlank() && !config.authorEmail.isNullOrBlank()) {
                        setAuthor(config.authorName, config.authorEmail)
                        setCommitter(config.authorName, config.authorEmail)
                    }
                }.call()

                val shouldPush = push ?: config.pushOnSync
                val pushSucceeded = if (shouldPush) {
                    push(git, accessToken)
                } else {
                    false
                }

                return GitSyncOutcome(
                    committed = true,
                    commitId = commit.id.name,
                    message = "Committed ${relativePaths.size} file(s) to git.",
                    pushAttempted = shouldPush,
                    pushSucceeded = pushSucceeded,
                    files = relativePaths
                )
            }
        }
    }

    private fun ensureNoUnrelatedStagedChanges(git: Git, relativePaths: List<String>) {
        val stagedChanges = currentIndexedChanges(git)
        val unrelated = stagedChanges.filter { it !in relativePaths.toSet() }
        check(unrelated.isEmpty()) {
            "Refusing CMS sync while other staged files exist: ${unrelated.joinToString(", ")}"
        }
    }

    private fun currentIndexedChanges(git: Git): Set<String> {
        val status = git.status().call()
        return buildSet {
            addAll(status.added)
            addAll(status.changed)
            addAll(status.removed)
        }
    }

    private fun push(git: Git, accessToken: String?): Boolean {
        val pushCommand = git.push().setRemote(config.remote)
        config.branch?.takeIf { it.isNotBlank() }?.let { branch ->
            pushCommand.setRefSpecs(RefSpec("HEAD:refs/heads/$branch"))
        }
        credentialsProvider(accessToken)?.let { pushCommand.setCredentialsProvider(it) }

        val results = pushCommand.call()
        return results.all { result -> result.isSuccessful() }
    }

    private fun credentialsProvider(accessToken: String?): UsernamePasswordCredentialsProvider? {
        accessToken?.takeIf { it.isNotBlank() }?.let { token ->
            return UsernamePasswordCredentialsProvider("oauth", token)
        }
        val tokenEnv = config.tokenEnv?.takeIf { it.isNotBlank() } ?: return null
        val token = System.getenv(tokenEnv)?.takeIf { it.isNotBlank() } ?: return null
        return UsernamePasswordCredentialsProvider("git", token)
    }

    private fun openRepository(): Repository? {
        val builder = FileRepositoryBuilder().findGitDir(siteRoot.toFile())
        return builder.gitDir?.let { builder.build() }
    }

    private fun toRepoRelativePath(repoRoot: Path, sourcePath: String): String {
        val normalized = siteRoot.resolve(sourcePath).toAbsolutePath().normalize()
        require(normalized.startsWith(repoRoot)) {
            "Content path $sourcePath is outside the git repository root $repoRoot"
        }
        return repoRoot.relativize(normalized).toString().replace('\\', '/')
    }

    private fun defaultCommitMessage(paths: List<String>): String {
        return if (paths.size == 1) {
            "cms: update ${paths.first()}"
        } else {
            "cms: sync ${paths.size} files"
        }
    }
}

data class GitSyncOutcome(
    val committed: Boolean,
    val commitId: String?,
    val message: String,
    val pushAttempted: Boolean,
    val pushSucceeded: Boolean,
    val files: List<String>
)

private fun PushResult.isSuccessful(): Boolean {
    return remoteUpdates.all {
        when (it.status) {
            org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK,
            org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE -> true
            else -> false
        }
    }
}
