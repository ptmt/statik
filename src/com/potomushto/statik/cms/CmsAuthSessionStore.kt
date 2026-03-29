package com.potomushto.statik.cms

import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.sql.Types

class CmsAuthSessionStore(
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
                    create table if not exists cms_auth_session (
                        session_id text primary key,
                        login text not null,
                        access_token text not null,
                        created_at integer not null,
                        installation_id integer
                    )
                    """.trimIndent()
                )
            }
        }
    }

    fun upsert(session: CmsAuthSession) {
        withConnection { connection ->
            connection.prepareStatement(
                """
                insert into cms_auth_session (
                    session_id, login, access_token, created_at, installation_id
                ) values (?, ?, ?, ?, ?)
                on conflict(session_id) do update set
                    login = excluded.login,
                    access_token = excluded.access_token,
                    created_at = excluded.created_at,
                    installation_id = excluded.installation_id
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, session.id)
                statement.setString(2, session.login)
                statement.setString(3, session.accessToken)
                statement.setLong(4, session.createdAt)
                if (session.installationId != null) {
                    statement.setLong(5, session.installationId)
                } else {
                    statement.setNull(5, Types.BIGINT)
                }
                statement.executeUpdate()
            }
        }
    }

    fun find(sessionId: String): CmsAuthSession? {
        return withConnection { connection ->
            connection.prepareStatement(
                """
                select session_id, login, access_token, created_at, installation_id
                from cms_auth_session
                where session_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, sessionId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) resultSet.toSession() else null
                }
            }
        }
    }

    fun delete(sessionId: String) {
        withConnection { connection ->
            connection.prepareStatement("delete from cms_auth_session where session_id = ?").use { statement ->
                statement.setString(1, sessionId)
                statement.executeUpdate()
            }
        }
    }

    fun deleteExpired(createdAtCutoff: Long) {
        withConnection { connection ->
            connection.prepareStatement("delete from cms_auth_session where created_at < ?").use { statement ->
                statement.setLong(1, createdAtCutoff)
                statement.executeUpdate()
            }
        }
    }

    private fun <T> withConnection(block: (java.sql.Connection) -> T): T {
        DriverManager.getConnection("jdbc:sqlite:${databasePath.toAbsolutePath()}").use { connection ->
            return block(connection)
        }
    }

    private fun java.sql.ResultSet.toSession(): CmsAuthSession {
        return CmsAuthSession(
            id = getString("session_id"),
            login = getString("login"),
            accessToken = getString("access_token"),
            createdAt = getLong("created_at"),
            installationId = getLong("installation_id").takeIf { !wasNull() }
        )
    }
}
