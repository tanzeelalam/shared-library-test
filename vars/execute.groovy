/**
 * Executes an OS agnostic command in the correct shell for the build node.
 *
 * <h4>Sample usage:</h4>
 * <pre>execute 'git --version'</pre>
 *
 * @param command [T:String] The command to execute.
 */
void call(def command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}
/**
 * Executes an OS agnostic command in the correct shell for the build node.
 * Prints an error message before re-throwing.
 *
 * <h4>Sample usage:</h4>
 * <pre>execute('git --version', 'Failed to get the git version.')</pre>
 *
 * @param command [T:String] The command to execute.
 * @param message [T:String] The message to print in case of error.
 */
void call(def command, def message) {
    try {
        call(command)
    } catch (Throwable e) {
        log.error(message)
        throw e
    }
}
