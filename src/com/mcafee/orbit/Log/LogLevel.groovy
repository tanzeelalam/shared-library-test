package com.mcafee.orbit.Log

import com.cloudbees.groovy.cps.NonCPS

/**
 * Represents a logging level.
 *
 * See {@link com.mcafee.orbit.LogLevels} class for canned instances of LogLevel.
 *
 * FATAL is required for backwards compatibility, but is not
 * implemented in the Logger and just short-circuits to ERROR.
 */
final class LogLevel implements Comparable<LogLevel> {
    /**
     * Value for Debug log level
     */
    final static int DEBUG = 4
    /**
     * Value for Info log level
     */
    final static int INFO = 3
    /**
     * Value for Warning log level
     */
    final static int WARN = 2
    /**
     * Value for Error log level
     */
    final static int ERROR = 1
    /**
     * Value for Fatal log level
     */
    final static int FATAL = 0
    /**
     * Internal storage for the log level
     */
    private final int level
    /**
     * Default constructor
     *
     * @param level The logging level as an integer
     */
    LogLevel(int level) {
        this.level = level
        if (level < 0 || level > 4) {
            throw new IllegalArgumentException(
                "Invalid log level '$level'"
            )
        }
    }
    /**
     * Standard implementation of Comparable interface
     *
     * @param other The other LogLevel to compare this LogLevel to
     * @return The result of the comparison
     */
    @NonCPS
    @Override
    int compareTo(LogLevel other) {
        this.level <=> other.level
    }
    /**
     * Returns the string representation of this LogLevel
     *
     * @return String representation of this LogLevel
     */
    @NonCPS
    @Override
    String toString() {
        switch (level) {
            case 4:
                return 'DEBUG'
            case 3:
                return 'INFO'
            case 2:
                return 'WARN'
            case 1:
            case 0:
                return 'ERROR'
            default:
                return 'INFO'
        }
    }
    /**
     * Returns the integer value of the log level
     *
     * @return log level as int
     */
    @Deprecated
    int asInt() {
        level
    }
}
