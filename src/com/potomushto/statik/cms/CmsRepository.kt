package com.potomushto.statik.cms

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

class CmsRepository(
    private val databasePath: Path
) {
    init {
        databasePath.parent?.let { Files.createDirectories(it) }
    }

    fun initialize() {
        withConnection { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    create table if not exists cms_content (
                        source_path text primary key,
                        content_type text not null,
                        output_path text not null,
                        title text not null,
                        extension text not null,
                        frontmatter text not null,
                        body text not null,
                        metadata_json text not null,
                        published_at text,
                        dirty integer not null default 0,
                        updated_at integer not null,
                        last_synced_at integer
                    )
                    """.trimIndent()
                )
            }
        }
    }

    fun replaceFromScan(entries: List<CmsContentEntry>) {
        withConnection { connection ->
            connection.autoCommit = false
            try {
                val statuses = loadStatuses(connection)
                entries.forEach { entry ->
                    val status = statuses[entry.sourcePath]
                    upsert(
                        connection,
                        entry.copy(
                            dirty = status?.dirty ?: entry.dirty,
                            lastSyncedAt = status?.lastSyncedAt ?: entry.lastSyncedAt
                        )
                    )
                }

                val sourcePaths = entries.map { it.sourcePath }.toSet()
                deleteMissing(connection, sourcePaths)
                connection.commit()
            } catch (error: Exception) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun upsert(entry: CmsContentEntry) {
        withConnection { connection ->
            upsert(connection, entry)
        }
    }

    fun find(sourcePath: String): CmsContentEntry? {
        return withConnection { connection ->
            connection.prepareStatement(
                """
                select source_path, content_type, output_path, title, extension, frontmatter, body,
                       metadata_json, published_at, dirty, updated_at, last_synced_at
                from cms_content
                where source_path = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, sourcePath)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) resultSet.toEntry() else null
                }
            }
        }
    }

    fun list(type: CmsContentType? = null): List<CmsContentEntry> {
        val sql = buildString {
            append(
                """
                select source_path, content_type, output_path, title, extension, frontmatter, body,
                       metadata_json, published_at, dirty, updated_at, last_synced_at
                from cms_content
                """.trimIndent()
            )
            if (type != null) {
                append(" where content_type = ?")
            }
            append(" order by content_type asc, source_path asc")
        }

        return withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                if (type != null) {
                    statement.setString(1, type.name)
                }
                statement.executeQuery().use { resultSet ->
                    buildList {
                        while (resultSet.next()) {
                            add(resultSet.toEntry())
                        }
                    }
                }
            }
        }
    }

    fun dirtySourcePaths(): List<String> {
        return withConnection { connection ->
            connection.prepareStatement(
                """
                select source_path
                from cms_content
                where dirty = 1
                order by source_path asc
                """.trimIndent()
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    buildList {
                        while (resultSet.next()) {
                            add(resultSet.getString("source_path"))
                        }
                    }
                }
            }
        }
    }

    fun markSynced(sourcePaths: List<String>, timestamp: Long) {
        if (sourcePaths.isEmpty()) return
        withConnection { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(
                    """
                    update cms_content
                    set dirty = 0,
                        last_synced_at = ?
                    where source_path = ?
                    """.trimIndent()
                ).use { statement ->
                    sourcePaths.forEach { sourcePath ->
                        statement.setLong(1, timestamp)
                        statement.setString(2, sourcePath)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
                connection.commit()
            } catch (error: Exception) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun count(): Int = withConnection { connection ->
        connection.prepareStatement("select count(*) as total from cms_content").use { statement ->
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getInt("total")
            }
        }
    }

    fun dirtyCount(): Int = withConnection { connection ->
        connection.prepareStatement("select count(*) as total from cms_content where dirty = 1").use { statement ->
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getInt("total")
            }
        }
    }

    fun lastSyncedAt(): Long? = withConnection { connection ->
        connection.prepareStatement("select max(last_synced_at) as last_synced_at from cms_content").use { statement ->
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getLong("last_synced_at").takeIf { !resultSet.wasNull() }
            }
        }
    }

    private fun upsert(connection: Connection, entry: CmsContentEntry) {
        connection.prepareStatement(
            """
            insert into cms_content (
                source_path, content_type, output_path, title, extension, frontmatter, body, metadata_json,
                published_at, dirty, updated_at, last_synced_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict(source_path) do update set
                content_type = excluded.content_type,
                output_path = excluded.output_path,
                title = excluded.title,
                extension = excluded.extension,
                frontmatter = excluded.frontmatter,
                body = excluded.body,
                metadata_json = excluded.metadata_json,
                published_at = excluded.published_at,
                dirty = excluded.dirty,
                updated_at = excluded.updated_at,
                last_synced_at = excluded.last_synced_at
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, entry.sourcePath)
            statement.setString(2, entry.type.name)
            statement.setString(3, entry.outputPath)
            statement.setString(4, entry.title)
            statement.setString(5, entry.extension)
            statement.setString(6, entry.frontmatter)
            statement.setString(7, entry.body)
            statement.setString(8, CmsMetadataJson.encode(entry.metadata))
            statement.setString(9, entry.publishedAt)
            statement.setInt(10, if (entry.dirty) 1 else 0)
            statement.setLong(11, entry.updatedAt)
            if (entry.lastSyncedAt != null) {
                statement.setLong(12, entry.lastSyncedAt)
            } else {
                statement.setNull(12, java.sql.Types.BIGINT)
            }
            statement.executeUpdate()
        }
    }

    private fun deleteMissing(connection: Connection, sourcePaths: Set<String>) {
        if (sourcePaths.isEmpty()) {
            connection.prepareStatement("delete from cms_content").use { it.executeUpdate() }
            return
        }

        val placeholders = sourcePaths.joinToString(", ") { "?" }
        connection.prepareStatement("delete from cms_content where source_path not in ($placeholders)").use { statement ->
            sourcePaths.forEachIndexed { index, sourcePath ->
                statement.setString(index + 1, sourcePath)
            }
            statement.executeUpdate()
        }
    }

    private fun loadStatuses(connection: Connection): Map<String, EntryStatus> {
        connection.prepareStatement(
            """
            select source_path, dirty, last_synced_at
            from cms_content
            """.trimIndent()
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                return buildMap {
                    while (resultSet.next()) {
                        put(
                            resultSet.getString("source_path"),
                            EntryStatus(
                                dirty = resultSet.getInt("dirty") == 1,
                                lastSyncedAt = resultSet.getLong("last_synced_at").takeIf { !resultSet.wasNull() }
                            )
                        )
                    }
                }
            }
        }
    }

    private fun <T> withConnection(block: (Connection) -> T): T {
        DriverManager.getConnection("jdbc:sqlite:${databasePath.toAbsolutePath()}").use { connection ->
            return block(connection)
        }
    }

    private fun java.sql.ResultSet.toEntry(): CmsContentEntry {
        return CmsContentEntry(
            type = CmsContentType.valueOf(getString("content_type")),
            sourcePath = getString("source_path"),
            outputPath = getString("output_path"),
            title = getString("title"),
            frontmatter = getString("frontmatter"),
            body = getString("body"),
            extension = getString("extension"),
            metadata = CmsMetadataJson.decode(getString("metadata_json")),
            publishedAt = getString("published_at"),
            dirty = getInt("dirty") == 1,
            updatedAt = getLong("updated_at"),
            lastSyncedAt = getLong("last_synced_at").takeIf { !wasNull() }
        )
    }

    private data class EntryStatus(
        val dirty: Boolean,
        val lastSyncedAt: Long?
    )
}
