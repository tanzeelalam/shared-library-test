import com.mcafee.orbit.Log.LogLevel
import com.mcafee.orbit.LogLevels
import org.codehaus.groovy.runtime.StackTraceUtils

/**
 * Logs a message unconditionally.
 *
 * @param message [T:Object] The message to log
 * @param closure [T:Closure] [OPTIONAL] The closure to evaluate and append to the message. [DEFAULT:null]
 */
void echo(Object message, Closure closure = null) {
    log(null, message, closure)
}
/**
 * Logs a debug message.
 *
 * @param message [T:Object] The message to log
 * @param closure [T:Closure] [OPTIONAL] The closure to evaluate and append to the message. [DEFAULT:null]
 */
void debug(Object message, Closure closure = null) {
    if (systemLevel >= LogLevels.DEBUG) {
        log(LogLevels.DEBUG, message, closure)
    }
}
/**
 * Logs an info message.
 *
 * @param message [T:Object] The message to log
 * @param closure [T:Closure] [OPTIONAL] The closure to evaluate and append to the message. [DEFAULT:null]
 */
void info(Object message, Closure closure = null) {
    if (systemLevel >= LogLevels.INFO) {
        log(LogLevels.INFO, message, closure)
    }
}
/**
 * Logs a warning message.
 *
 * @param message [T:Object] The message to log
 * @param closure [T:Closure] [OPTIONAL] The closure to evaluate and append to the message. [DEFAULT:null]
 */
void warn(Object message, Closure closure = null) {
    if (systemLevel >= LogLevels.WARN) {
        log(LogLevels.WARN, message, closure)
    }
}
/**
 * Logs an error message
 *
 * @param message [T:Object] The message to log
 * @param closure [T:Closure] [OPTIONAL] The closure to evaluate and append to the message. [DEFAULT:null]
 */
void error(Object message, Closure closure = null) {
    if (systemLevel >= LogLevels.ERROR) {
        log(LogLevels.ERROR, message, closure)
    }
}
/**
 * Returns the system log level.
 *
 * @return [T:LogLevel] The system log level
 */
LogLevel getSystemLevel() {
    try {
        new LogLevel(env.DEBUGLEVEL as Integer)
    } catch (Throwable e) {
        LogLevels.INFO
    }
}
/**
 * Logs a message at the specified level
 *
 * @param level The level to log at
 * @param message The message to log
 * @param closure The optional closure to evaluate and append to the message
 */
private void log(LogLevel level, Object message, Closure closure) {
    if (message != null) {
        println(formatMessage(level, message, closure))
    }
}
/**
 * Returns the current date/time as a formatted string
 *
 * @return The current date/time
 */
private String getDateTime() {
    new Date().format('HH:mm:ss.SSS')
}
/**
 * Returns the trace information to be included in the formatted log message
 *
 * @return trace information to be included in the formatted log message
 */
private String getTrace() {
    def stackTrace = StackTraceUtils.sanitize(new Throwable()).stackTrace
    if (stackTrace.size() > 5) {
        StackTraceElement element = stackTrace[5]
        String method = element.methodName
        //Native methods do not have a fileName
        //Use className instead
        String origin = element.nativeMethod ? element.className : element.fileName.tokenize('/').last()
        if (origin.endsWith('.groovy')) {
            origin = origin.substring(0, origin.size() - 7)
        }
        if (method == 'call') {
            return "[$origin()]"
        } else {
            return "[$origin.$method()]"
        }
    } else {
        return ""
    }
}
/**
 * Formats a message to be logged
 *
 * @param level The level to log at
 * @param message The message to log
 * @param closure The optional closure to evaluate and append to the message
 *
 * @return The formatted message to be logged
 */
private String formatMessage(LogLevel level, Object message, Closure closure) {
    String formattedMessage
    if (level == null) {
        formattedMessage = "[$dateTime] $trace ${message.toString()}"
    } else {
        formattedMessage = "[$dateTime] ${level.toString()} $trace ${message.toString()}"
    }
    if (closure != null) {
        formattedMessage += '\n'
        formattedMessage += closure().toString()
    }
    formattedMessage
}