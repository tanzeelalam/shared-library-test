import org.codehaus.groovy.runtime.StackTraceUtils

/**
 * Called from deprecated methods.
 */
private void call(boolean hasEffect = true) {
    def marker = new Throwable()
    def stack = StackTraceUtils.sanitize(marker).stackTrace
    def trace = stack[1]
    if (trace.fileName == "deprecated.groovy") {
        trace = stack[2]
    }
    def fileName = trace.fileName
    if (fileName.endsWith('.groovy')) {
        fileName = fileName.substring(0, trace.fileName.size() - 7)
    }
    def name = fileName + "." + trace.methodName + "()"
    if (trace.methodName == "call") {
        name = fileName + "()"
    }
    def warning = "[DEPRECATED] Method $name is deprecated"
    if (hasEffect) {
        warning += ". Please see https://docs.orbit.corp.entsec.com/orbit/ for alternatives."
    } else {
        warning += " and has no effect."
    }
    log.warn(warning)
}
