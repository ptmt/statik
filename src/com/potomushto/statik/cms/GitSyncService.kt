package com.potomushto.statik.cms

import com.potomushto.statik.config.CmsGitConfig
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.PushResult
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class GitSyncService(
    private val siteRoot: Path,
    private val config: CmsGitConfig,
    private val generatedOutputRoot: String? = null
) {
    fun pull(accessToken: String? = null): GitPullOutcome {
        if (!config.enabled) {
            return GitPullOutcome(
                repositoryChanged = false,
                message = null
            )
        }

        val repository = openRepository() ?: return GitPullOutcome(
            repositoryChanged = false,
            message = null
        )

        repository.use { repo ->
            ensureLocalExcludes(repo)
            Git(repo).use { git ->
                val pullResult = pullLatest(git, accessToken)
                check(pullResult.succeeded) {
                    pullResult.detail ?: "Pull from ${config.remote} could not be completed."
                }

                return GitPullOutcome(
                    repositoryChanged = pullResult.repositoryChanged,
                    message = pullResult.detail
                )
            }
        }
    }

    fun status(): CmsGitStatus {
        val repository = openRepository()
        return if (repository == null) {
            CmsGitStatus(
                available = false,
                enabled = config.enabled
            )
        } else {
            repository.use {
                ensureLocalExcludes(it)
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
        val shouldPush = push ?: config.pushOnSync
        if (sourcePaths.isEmpty()) {
            if (!shouldPush) {
                return GitSyncOutcome(
                    committed = false,
                    commitId = null,
                    message = "No CMS changes to sync.",
                    pushAttempted = false,
                    pushSucceeded = false,
                    files = emptyList(),
                    syncCompleted = false,
                    repositoryChanged = false
                )
            }

            val repository = openRepository() ?: return GitSyncOutcome(
                committed = false,
                commitId = null,
                message = "No CMS changes to sync.",
                pushAttempted = shouldPush,
                pushSucceeded = false,
                files = emptyList(),
                syncCompleted = false,
                repositoryChanged = false
            )

            repository.use { repo ->
                ensureLocalExcludes(repo)
                Git(repo).use { git ->
                    val pushResult = pushWithRecovery(git, accessToken)
                    return GitSyncOutcome(
                        committed = false,
                        commitId = null,
                        message = pushResult.detail ?: "No new commit was created.",
                        pushAttempted = true,
                        pushSucceeded = pushResult.succeeded,
                        files = emptyList(),
                        syncCompleted = pushResult.succeeded,
                        repositoryChanged = pushResult.repositoryChanged
                    )
                }
            }
        }

        val repository = openRepository()
            ?: throw IllegalStateException("No git repository found for CMS sync under $siteRoot")

        repository.use { repo ->
            ensureLocalExcludes(repo)
            Git(repo).use { git ->
                val repoRoot = repo.workTree.toPath().toAbsolutePath().normalize()
                val relativePaths = (
                    sourcePaths.map { toRepoRelativePath(repoRoot, it) } +
                        generatedOutputPaths(git, repoRoot)
                    ).distinct().sorted()
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
                val commit = if (stagedChanges.isNotEmpty()) {
                    git.commit().apply {
                        setMessage(commitMessage ?: defaultCommitMessage(relativePaths))
                        if (!config.authorName.isNullOrBlank() && !config.authorEmail.isNullOrBlank()) {
                            setAuthor(config.authorName, config.authorEmail)
                            setCommitter(config.authorName, config.authorEmail)
                        }
                    }.call()
                } else {
                    null
                }

                if (commit == null && !shouldPush) {
                    return GitSyncOutcome(
                        committed = false,
                        commitId = null,
                        message = "No git changes detected for CMS files.",
                        pushAttempted = false,
                        pushSucceeded = false,
                        files = relativePaths,
                        syncCompleted = false,
                        repositoryChanged = false
                    )
                }

                val pushResult = if (shouldPush) {
                    pushWithRecovery(git, accessToken)
                } else {
                    null
                }
                val pushSucceeded = pushResult?.succeeded ?: false
                val syncCompleted = if (shouldPush) {
                    pushSucceeded
                } else {
                    commit != null
                }

                val message = buildString {
                    if (commit != null) {
                        append("Committed ${relativePaths.size} file(s) to git.")
                    } else {
                        append("No new commit was created.")
                    }
                    pushResult?.detail?.takeIf { it.isNotBlank() }?.let {
                        append(' ')
                        append(it)
                    }
                }

                return GitSyncOutcome(
                    committed = commit != null,
                    commitId = commit?.id?.name,
                    message = message,
                    pushAttempted = shouldPush,
                    pushSucceeded = pushSucceeded,
                    files = relativePaths,
                    syncCompleted = syncCompleted,
                    repositoryChanged = pushResult?.repositoryChanged ?: false
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

    private fun generatedOutputPaths(git: Git, repoRoot: Path): List<String> {
        val outputRoot = generatedOutputRoot?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
        val relativeRoot = toRepoRelativePath(repoRoot, outputRoot)
        val status = git.status().call()
        return buildSet {
            addAll(status.added)
            addAll(status.changed)
            addAll(status.modified)
            addAll(status.removed)
            addAll(status.missing)
            addAll(status.untracked)
        }.filter { path ->
            path == relativeRoot || path.startsWith("$relativeRoot/")
        }.sorted()
    }

    private fun pullLatest(git: Git, accessToken: String?): BranchSyncResult {
        val branch = targetBranch(git.repository)
            ?: return BranchSyncResult(
                succeeded = true,
                repositoryChanged = false,
                detail = null
            )
        fetchRemote(git, accessToken)
        val rebaseResult = rebaseOntoRemote(git, branch)
        return if (rebaseResult.succeeded) {
            BranchSyncResult(
                succeeded = true,
                repositoryChanged = rebaseResult.repositoryChanged,
                detail = if (rebaseResult.repositoryChanged) {
                    "Pulled latest changes from ${config.remote}/$branch."
                } else {
                    null
                }
            )
        } else {
            BranchSyncResult(
                succeeded = false,
                repositoryChanged = false,
                detail = "Pull from ${config.remote}/$branch could not be completed automatically."
            )
        }
    }

    private fun fetchRemote(git: Git, accessToken: String?) {
        credentialsProvider(accessToken)?.let { credentials ->
            git.fetch()
                .setRemote(config.remote)
                .setCredentialsProvider(credentials)
                .call()
            return
        }

        git.fetch()
            .setRemote(config.remote)
            .call()
    }

    private fun rebaseOntoRemote(git: Git, branch: String): BranchSyncResult {
        val remoteTrackingRef = "refs/remotes/${config.remote}/$branch"
        val rebaseResult = git.rebase()
            .setUpstream(remoteTrackingRef)
            .call()

        return if (rebaseResult.status.name in SUCCESSFUL_REBASE_STATUSES) {
            BranchSyncResult(
                succeeded = true,
                repositoryChanged = rebaseResult.status.name in REPOSITORY_CHANGED_REBASE_STATUSES,
                detail = null
            )
        } else {
            runCatching {
                git.rebase()
                    .setOperation(RebaseCommand.Operation.ABORT)
                    .call()
            }
            BranchSyncResult(
                succeeded = false,
                repositoryChanged = false,
                detail = null
            )
        }
    }

    private fun pushWithRecovery(git: Git, accessToken: String?): PushAttemptResult {
        val branch = targetBranch(git.repository)
        val initialPush = executePush(git, accessToken, branch)
        if (initialPush.succeeded || !initialPush.rejectedByRemoteUpdate || branch == null) {
            return initialPush
        }

        fetchRemote(git, accessToken)
        val rebaseResult = rebaseOntoRemote(git, branch)

        return if (rebaseResult.succeeded) {
            val retriedPush = executePush(git, accessToken, branch)
            if (retriedPush.succeeded) {
                retriedPush.copy(
                    detail = "Push succeeded after rebasing onto ${config.remote}/$branch.",
                    repositoryChanged = rebaseResult.repositoryChanged
                )
            } else {
                retriedPush.copy(
                    detail = "Push still failed after rebasing onto ${config.remote}/$branch.",
                    repositoryChanged = rebaseResult.repositoryChanged
                )
            }
        } else {
            PushAttemptResult(
                succeeded = false,
                rejectedByRemoteUpdate = true,
                detail = "Push was rejected by ${config.remote}/$branch, and automatic rebase could not be completed.",
                repositoryChanged = false
            )
        }
    }

    private fun executePush(git: Git, accessToken: String?, branch: String?): PushAttemptResult {
        val pushCommand = git.push().setRemote(config.remote)
        branch?.let {
            pushCommand.setRefSpecs(RefSpec("HEAD:refs/heads/$branch"))
        }
        credentialsProvider(accessToken)?.let { pushCommand.setCredentialsProvider(it) }

        val results = pushCommand.call()
        val successful = results.all { result -> result.isSuccessful() }
        val rejectedByRemoteUpdate = results.any { result ->
            result.remoteUpdates.any { update ->
                update.status in setOf(
                    RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD,
                    RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED
                )
            }
        }

        return PushAttemptResult(
            succeeded = successful,
            rejectedByRemoteUpdate = rejectedByRemoteUpdate,
            detail = if (successful) summarizePushSuccess(results, branch) else summarizePushFailure(results, branch),
            repositoryChanged = false
        )
    }

    private fun targetBranch(repository: Repository): String? {
        return config.branch?.takeIf { it.isNotBlank() }
            ?: runCatching { repository.branch }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun summarizePushSuccess(results: Iterable<PushResult>, branch: String?): String? {
        val updates = results.flatMap { it.remoteUpdates }
        if (updates.isEmpty()) {
            return "Push completed."
        }

        val target = branch?.let { "${config.remote}/$it" } ?: config.remote
        return when {
            updates.any { it.status == RemoteRefUpdate.Status.OK } -> "Push to $target succeeded."
            updates.all { it.status == RemoteRefUpdate.Status.UP_TO_DATE } -> "Remote $target is already up to date."
            else -> "Push completed."
        }
    }

    private fun summarizePushFailure(results: Iterable<PushResult>, branch: String?): String {
        val updates = results.flatMap { it.remoteUpdates }
        if (updates.isEmpty()) {
            return "Push failed."
        }

        val target = branch?.let { "${config.remote}/$it" } ?: config.remote
        val statuses = updates.joinToString(", ") { update ->
            "${update.remoteName}: ${update.status.name.lowercase()}"
        }
        return "Push to $target failed ($statuses)."
    }

    private fun credentialsProvider(accessToken: String?): UsernamePasswordCredentialsProvider? {
        accessToken?.takeIf { it.isNotBlank() }?.let { token ->
            return UsernamePasswordCredentialsProvider("x-access-token", token)
        }
        val tokenEnv = config.tokenEnv?.takeIf { it.isNotBlank() } ?: return null
        val token = System.getenv(tokenEnv)?.takeIf { it.isNotBlank() } ?: return null
        return UsernamePasswordCredentialsProvider("x-access-token", token)
    }

    private fun openRepository(): Repository? {
        val builder = FileRepositoryBuilder().findGitDir(siteRoot.toFile())
        return builder.gitDir?.let { builder.build() }
    }

    private fun ensureLocalExcludes(repository: Repository) {
        val excludeFile = repository.directory.toPath().resolve("info").resolve("exclude")
        Files.createDirectories(excludeFile.parent)

        val existing = if (Files.exists(excludeFile)) {
            Files.readAllLines(excludeFile).map { it.trim() }.toSet()
        } else {
            emptySet()
        }
        val missing = LOCAL_EXCLUDE_PATTERNS.filter { it !in existing }
        if (missing.isEmpty()) {
            return
        }

        val needsLeadingNewline = Files.exists(excludeFile) &&
            Files.size(excludeFile) > 0 &&
            !Files.readString(excludeFile).endsWith("\n")
        val content = buildString {
            if (needsLeadingNewline) {
                append('\n')
            }
            missing.forEach { pattern ->
                append(pattern)
                append('\n')
            }
        }
        Files.writeString(
            excludeFile,
            content,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        )
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

    private companion object {
        private val LOCAL_EXCLUDE_PATTERNS = listOf(".statik/")
    }
}

data class GitSyncOutcome(
    val committed: Boolean,
    val commitId: String?,
    val message: String,
    val pushAttempted: Boolean,
    val pushSucceeded: Boolean,
    val files: List<String>,
    val syncCompleted: Boolean,
    val repositoryChanged: Boolean
)

data class GitPullOutcome(
    val repositoryChanged: Boolean,
    val message: String?
)

private data class PushAttemptResult(
    val succeeded: Boolean,
    val rejectedByRemoteUpdate: Boolean,
    val detail: String?,
    val repositoryChanged: Boolean
)

private data class BranchSyncResult(
    val succeeded: Boolean,
    val repositoryChanged: Boolean,
    val detail: String?
)

private val SUCCESSFUL_REBASE_STATUSES = setOf(
    "OK",
    "UP_TO_DATE",
    "FAST_FORWARD",
    "NOTHING_TO_COMMIT"
)

private val REPOSITORY_CHANGED_REBASE_STATUSES = setOf(
    "OK",
    "FAST_FORWARD",
    "NOTHING_TO_COMMIT"
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
