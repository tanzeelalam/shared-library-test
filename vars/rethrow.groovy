import com.mcafee.orbit.Credentials.CredentialStore

/**
 * Call this closure when you want to re-throw
 * an exception that you caught somewhere.
 *
 * @param e [T:Throwable] The exception to re-throw.
 * @param context [T:String] [OPTIONAL] A short message detailing what happened. [DEFAULT:null]
 * @param notify [T:boolean] [OPTIONAL] Send an email notification before re-throwing. [DEFAULT:false]
 */
void call(Throwable e, String context = null, boolean notify = false) {
    if (context != null) {
        log.error(context)
    }
    if (notify) {
        sendErrorNotification(e, context)
    }
    throw e
}

/**
 * Send error notification email to orbit core team
 *
 * @param e The error to notify about
 */
private void sendErrorNotification(Throwable e, String context) {
    global()
    String errorMessage = renderError(e)
    String body = """
        <p>Orbit has experienced an unexpected error.</p>
        <p>Build URL: ${env.BUILD_URL}</p>
        <p>Context: ${context ?: 'No context provided'}</p>
        <p>Error: $errorMessage</p>"
    """
    if (errorMessage != null && errorMessage.trim().size() > 0) {
        log.info("Sending email alert to: ${global.ORBIT_AUTOMATION_EMAIL}")
        log.debug("Error message in email: $errorMessage")
        emailext(
            to: global.ORBIT_AUTOMATION_EMAIL,
            replyTo: global.ORBIT_CORE_TEAM_EMAIL,
            subject: "ERROR in job: ${env.JOB_BASE_NAME} [${orbit.buildNumber()}]",
            body: body,
            attachLog: true,
            compressLog: true,
        )
    } else {
        log.error('Failed to send email notification. Error message is empty.')
    }
}

/**
 * Render an error to a string
 *
 * @param e The error to render
 * @return The rendered error
 */
@NonCPS
private String renderError(Throwable e) {
    StringWriter sr = new StringWriter()
    PrintWriter pr = new PrintWriter(sr)
    e.printStackTrace(pr)
    String errorMessage = sr.toString()
    sr = null
    pr = null
    return errorMessage
}