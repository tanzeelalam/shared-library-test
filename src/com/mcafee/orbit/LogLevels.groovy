package com.mcafee.orbit

import com.mcafee.orbit.Log.LogLevel

/**
 * Canned instances of the {@link LogLevel} class.
 */
final class LogLevels {
    /**
     * Debug log level
     */
    final static LogLevel DEBUG = new LogLevel(LogLevel.DEBUG)
    /**
     * Info log level
     */
    final static LogLevel INFO = new LogLevel(LogLevel.INFO)
    /**
     * Warning log level
     */
    final static LogLevel WARN = new LogLevel(LogLevel.WARN)
    /**
     * Error log level
     */
    final static LogLevel ERROR = new LogLevel(LogLevel.ERROR)
    /**
     * Fatal log level
     */
    final static LogLevel FATAL = new LogLevel(LogLevel.FATAL)
}