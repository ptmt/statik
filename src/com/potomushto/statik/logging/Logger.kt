package com.potomushto.statik.logging

import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

enum class LogLevel(val priority: Int) {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4);

    companion object {
        fun from(value: String?): LogLevel? {
            val normalized = value?.trim()?.uppercase() ?: return null
            return values().firstOrNull { it.name == normalized }
        }
    }
}

class Logger internal constructor(private val name: String) {
    fun trace(message: String) = logImmediate(LogLevel.TRACE, message)
    fun trace(message: () -> String) = log(LogLevel.TRACE, message)
    fun trace(message: String, throwable: Throwable) = logImmediate(LogLevel.TRACE, message, throwable)

    fun debug(message: String) = logImmediate(LogLevel.DEBUG, message)
    fun debug(message: () -> String) = log(LogLevel.DEBUG, message)
    fun debug(message: String, throwable: Throwable) = logImmediate(LogLevel.DEBUG, message, throwable)

    fun info(message: String) = logImmediate(LogLevel.INFO, message)
    fun info(message: () -> String) = log(LogLevel.INFO, message)
    fun info(message: String, throwable: Throwable) = logImmediate(LogLevel.INFO, message, throwable)

    fun warn(message: String) = logImmediate(LogLevel.WARN, message)
    fun warn(message: () -> String) = log(LogLevel.WARN, message)
    fun warn(message: String, throwable: Throwable) = logImmediate(LogLevel.WARN, message, throwable)

    fun error(message: String) = logImmediate(LogLevel.ERROR, message)
    fun error(message: () -> String) = log(LogLevel.ERROR, message)
    fun error(message: String, throwable: Throwable) = logImmediate(LogLevel.ERROR, message, throwable)

    private fun logImmediate(level: LogLevel, message: String, throwable: Throwable? = null) =
        log(level, { message }, throwable)

    private inline fun log(level: LogLevel, message: () -> String, throwable: Throwable? = null) {
        if (!LoggerFactory.shouldLog(level)) return

        val output = LoggerFactory.outputFor(level)
        val timestamp = LoggerFactory.timestamp()
        val levelLabel = LoggerFactory.levelLabel(level)
        val text = message()

        output.println("$timestamp [$levelLabel] [$name] $text")
        if (throwable != null) {
            output.println(throwable.stackTraceToString())
        }
    }
}

object LoggerFactory {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val loggers = ConcurrentHashMap<String, Logger>()
    private val maxLevelLength = LogLevel.values().maxOf { it.name.length }
    @Volatile private var level: LogLevel = detectInitialLevel()

    fun getLogger(name: String): Logger = loggers.computeIfAbsent(name) { Logger(it) }

    fun getLogger(clazz: Class<*>): Logger {
        val candidate = clazz.simpleName
        val resolvedName = if (candidate.isNotEmpty()) candidate else clazz.name
        return getLogger(resolvedName)
    }

    fun getLogger(clazz: KClass<*>): Logger {
        val candidate = clazz.simpleName
        val resolvedName = candidate ?: clazz.qualifiedName ?: clazz.toString()
        return getLogger(resolvedName)
    }

    fun getLevel(): LogLevel = level

    fun setLevel(logLevel: LogLevel) {
        level = logLevel
    }

    fun setLevel(levelName: String?) {
        LogLevel.from(levelName)?.let { setLevel(it) }
    }

    internal fun shouldLog(level: LogLevel): Boolean = level.priority >= this.level.priority

    internal fun timestamp(): String = LocalDateTime.now().format(formatter)

    internal fun levelLabel(level: LogLevel): String = level.name.padEnd(maxLevelLength)

    internal fun outputFor(level: LogLevel): PrintStream =
        if (level.priority >= LogLevel.WARN.priority) System.err else System.out

    private fun detectInitialLevel(): LogLevel {
        val candidates = sequenceOf(
            systemProperty("statik.logLevel"),
            systemProperty("statik.log.level"),
            env("STATIK_LOG_LEVEL")
        )
        return candidates.mapNotNull { LogLevel.from(it) }.firstOrNull() ?: LogLevel.INFO
    }

    private fun systemProperty(key: String): String? =
        try {
            System.getProperty(key)
        } catch (_: SecurityException) {
            null
        }

    private fun env(key: String): String? =
        try {
            System.getenv(key)
        } catch (_: SecurityException) {
            null
        }
}
