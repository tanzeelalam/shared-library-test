package com.mcafee.orbit.Log

import com.cloudbees.groovy.cps.NonCPS
import com.cloudbees.groovy.cps.SerializableScript
import com.mcafee.orbit.LogLevels
import com.mcafee.orbit.Pipeline.Context

/**
 * Logger class for writing to the console output
 */
final class Logger {
    private final Context context
    private final SerializableScript script
    private LogLevel level
    /**
     * Default constructor
     *
     * @param context The pipeline context
     */
    Logger(Context context, SerializableScript script) {
        this.context = context
        this.script = script
        this.level = LogLevels.INFO
        script.env.DEBUGLEVEL = '3'
    }
    /**
     * Logs a message regardless of logging level
     *
     * @param message The message to log
     */
    void echo(Object message) {
        context.println("$dateTime ${message.toString()}")
    }
    /**
     * Logs a debug message
     *
     * @param message The message to log
     */
    void debug(Object message) {
        if (level >= LogLevels.DEBUG) {
            context.println("$dateTime DEBUG  ${message.toString()}")
        }
    }
    /**
     * Logs an info message
     *
     * @param message The message to log
     */
    void info(Object message) {
        if (level >= LogLevels.INFO) {
            context.println("$dateTime INFO  ${message.toString()}")
        }
    }
    /**
     * Logs a warning message
     *
     * @param message The message to log
     */
    void warn(Object message) {
        if (level >= LogLevels.WARN) {
            context.println("$dateTime WARNING  ${message.toString()}")
        }
    }
    /**
     * Logs an error message
     *
     * @param message The message to log
     */
    void error(Object message) {
        if (level >= LogLevels.ERROR) {
            context.println("$dateTime ERROR  ${message.toString()}")
        }
    }
    /**
     * Returns the current log level
     *
     * @return The current log level
     */
    LogLevel getLevel() {
        level
    }
    /**
     * Sets the current log level to a new value
     *
     * @param level The new log level
     */
    void setLevel(LogLevel level) {
        this.level = level
        script.env.DEBUGLEVEL = level.asInt().toString() // lol
    }
    /**
     * Returns the current date/time as a formatted string
     *
     * @return The current date/time
     */
    @NonCPS
    private static String getDateTime() {
        new Date().format('HH:mm:ss.SSS')
    }
}