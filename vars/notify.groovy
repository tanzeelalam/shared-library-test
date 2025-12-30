import com.mcafee.orbit.Utils.StringUtils
import com.mcafee.orbit.Utils.EmailUtils
/**
 * Send out email notification regarding the build status.
 *
 * Email templates can be customised.
 * See https://confluence.trellix.com/display/PDS/Email+Notifications
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * orbit.notifyBuild("FAILED")
 * </pre>
 * @param buildStatus [T:String] Status of the build: <code>"STARTED"</code>, <code>"SUCCESSFUL"</code>, <code>"FAILED"</code>.
 */
public void notifyBuild(String buildStatus) {
    this.notifyBuild([buildStatus: buildStatus])
}

/**
 * Send out email notification regarding the build status.
 *
 * Email templates can be customised.
 * See https://confluence.trellix.com/display/PDS/Email+Notifications
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * orbit.notifyBuild("FAILED")
 * </pre>
 * @param buildStatus [T:String] Status of the build: <code>"STARTED"</code>, <code>"SUCCESSFUL"</code>, <code>"FAILED"</code>.
 * @param tokenMap [T:Map] [OPTIONAL] mapping to the template file to use for custom notifications.
 * @param templateLocation [T:String] [OPTIONAL] the template file to use for custom notifications.
 * @param emailToList [T:String] [OPTIONAL] List of email recepients.
 * @param emailSubject [T:String][OPTIONAL] The subject line for the email.
 */
public void notifyBuild(Map arg) {
    global()
    Boolean usingCustomTemplate = false
    Boolean includeLogs = true
    String orbitBuildNumber = orbit.buildNumber()
    log.debug("Args passed to notifyBuild : \n" + arg)
    if (utils.isNullOrEmptyString(arg.buildStatus)) {
        log.debug("Using default buildStatus " + buildStatus)
    }
    if (utils.isNullOrEmptyString(arg.templateLocation)) {
        log.debug("Using default templateLocation ")
    } else {
        usingCustomTemplate = true
        log.debug("Using passed templateLocation " + arg.templateLocation)
    }
    // build status of null means passed
    def buildStatus = arg.buildStatus ?: 'passed'
    buildStatus = buildStatus.toUpperCase()

    Closure encodeUriComponent = {
        URLEncoder.encode(it.toString(), 'UTF-8').replace('+', '%20')
    }

    String fullJobName = env.JOB_NAME
    def splitFullJobName = fullJobName.split('/')

    def radarURL = [
            Product         : splitFullJobName[0].replace("_", " "),
            Component       : encodeUriComponent(splitFullJobName[1]),
            ComponentVersion: encodeUriComponent(env.BUILDVERSION),
            BuildNumber     : orbitBuildNumber,
            URL             : env.BUILD_URL,
            Branch          : encodeUriComponent(env.BRANCH_NAME),
            BuildMilestone  : "Queued"
    ]

    String reformattedEmail = EmailUtils.reformatEmails(env.EMAIL_DL);

    def artifactoryServer = "${env.ARTIFACTORY_URL}"
    String email_DL = arg.emailToList ?: "${reformattedEmail}"
    log.debug("email_DL: " + email_DL)
    String subject = ""

    try {
        // Default values
        String css = ''' table {
                    border-collapse: collapse;
                border-spacing: 0;
                }
                td {
                    font-family: Arial, sans-serif;
                    font-size: 14px;
                    padding: 10px 5px;
                    border-style: solid;
                    border-width: 1px;
                    overflow: hidden;
                    word-break: normal;
                    vertical-align: top;
                }
                
                #name {
                    font-weight: normal;
                }'''

        Boolean sendStarted = true
        if (env.EMAIL_ON_STARTED != null) {
            sendStarted = env.EMAIL_ON_STARTED.toBoolean()
        }
        Boolean sendOnlyOnFailure = false
        if (env.EMAIL_ONLY_ON_FAILURE != null) {
            sendOnlyOnFailure = env.EMAIL_ONLY_ON_FAILURE.toBoolean()
        } else if (env.EMAILONLYONFAILURE != null) {
            sendOnlyOnFailure = env.EMAILONLYONFAILURE.toBoolean()
        }
        Boolean sendCustomEmails = false
        if (env.SEND_CUSTOM_EMAILS != null) {
            sendCustomEmails = env.SEND_CUSTOM_EMAILS.toBoolean()
        }

        // Check buildStatus is Started or Failed and env.EMAIL_ON_STARTED or env.EMAIL_ONLY_ON_FAILURE is set.
        // If both env.EMAIL_ONLY_ON_FAILURE and env.EMAIL_ON_STARTED are not set but build is failed or started status, it will send email notification by default.
        // Override default values based on build status
        // Send email notification only if buildStatus is Successful and current Stage is Delivery.
        boolean failNotify = buildStatus == 'FAILED'
        boolean startNotify = buildStatus == 'STARTED' && sendStarted && !sendOnlyOnFailure
        boolean successNotify = buildStatus == 'PASSED' && !sendOnlyOnFailure
        boolean sendNotification = ((failNotify || startNotify) || (successNotify)) || sendCustomEmails
        if (email_DL == null) {
            log.error("No email DL or address has been specified in the JenkinsFile.")
            sendNotification = false
        } else {
            log.debug("email_DL: " + email_DL)
        }

        if (usingCustomTemplate && !arg.tokenMap) {
            sendNotification = false
            log.error('Trying to use custom template without providing tokenMap')
        }
        def emailJobName = env.JOB_BASE_NAME

        //if env.BRANCH_NAME is set, assuming build is a mutlibranch pipeline build, where the JOB_BASE_NAME is the branch.
        if (env.BRANCH_NAME) {
            emailJobName = emailJobName.replaceAll('%2F', '/')
            emailJobName = radarURL.Component + ":" + emailJobName
        }

        if (sendNotification) {
            def tokenMap = []
            // set default value for templateString.
            String templateString = ''
            String templateStringLocation = "org/mcafee/templates/"

            String notifyRadarURL = "${env.defaultRadarURLBase}/Product" +
                    "/${radarURL.Product}/Builds/${radarURL.Component}/${radarURL.ComponentVersion}" +
                    "/${radarURL.Branch}/${orbitBuildNumber}"
            String artifactoryURL = "${artifactoryServer}/webapp/#/builds/${radarURL.Component}_${env.BUILDVERSION}/${orbitBuildNumber}"

            switch (buildStatus) {
                case "STARTED":
                case "FAILED":
                    templateString = libraryResource templateStringLocation + 'default.template'
                    log.debug("Set failed or started notification")
                    tokenMap = ["%STATUS%"        : buildStatus,
                                "%JOBNAME%"       : env.JOB_BASE_NAME,
                                "%BUILDNUMBER%"   : orbitBuildNumber,
                                "%VERSION%"       : env.BUILDVERSION,
                                "%CURRENTBRANCH%" : env.BRANCH_NAME,
                                "%BUILDURL%"      : env.BUILD_URL,
                                "%RADARURL%"      : notifyRadarURL,
                                "%CSS%"           : css,
                    ]
                    break
                case "PASSED":
                    templateString = libraryResource templateStringLocation + 'success.template'
                    log.debug("Set successful notification")

                    tokenMap = ["%STATUS%"        : "SUCCESSFUL",
                                "%JOBNAME%"       : env.JOB_BASE_NAME,
                                "%BUILDNUMBER%"   : orbitBuildNumber,
                                "%VERSION%"       : env.BUILDVERSION,
                                "%CURRENTBRANCH%" : env.BRANCH_NAME,
                                "%BUILDURL%"      : env.BUILD_URL,
                                "%RADARURL%"      : notifyRadarURL,
                                "%ARTIFACTORYURL%": artifactoryURL,
                                "%CSS%"           : css,
                    ]
                    break
                default:
                    templateString = readFile(arg.templateLocation.replace("/", "\\"))
                    log.debug("Using custom template file for ${buildStatus}\n ${templateString}")
                    break
            }
            // Construct subject based on Orbit environment.
            // If it is NOT 'Prod' it will append the value of ORBIT_BUILD_ENV.
            subject = "${buildStatus}" +": ${emailJobName} ${env.BUILDVERSION} [${orbitBuildNumber}]"
            def environment = config('ORBIT_BUILD_ENV').toUpperCase()
            if (environment != "PROD") {
                subject += " (${environment})"
            }

            // If provided, overwrite the subject with the provided one. This is specific for custom emails.
            subject = arg.emailSubject ?: subject

            log.debug("subject: " + subject)

            //Copy template file to workspace
            def templateFile = env.WORKSPACE + "\\notifyEmail.html.template"
            writeFile(file: templateFile, text: templateString)

            log.debug("failNotify: " + failNotify)
            log.debug("startNotify: " + startNotify)
            log.debug("successNotify: " + successNotify)
            log.debug("buildStatus: " + buildStatus)

            if (arg.tokenMap) {
                tokenMap = arg.tokenMap
                log.debug("Using Custom tokenMap == " + tokenMap)
            }

            log.debug('sendNotification')
            log.debug(templateFile)
            log.debug('notifyBuild templateFile = ' + templateFile)

            tokenMap += ["%CAUSECHAIN%": env.CAUSECHAIN]

            utils.emailBuilder([
                    target      : email_DL,
                    source      : global.ORBIT_AUTOMATION_EMAIL,
                    subject     : subject,
                    template    : templateFile,
                    logsAttached: includeLogs,
                    tokenMap    : tokenMap
            ])
        } else {
            log.debug('DO NOT NOTIFY')
        }
    } catch (Throwable e) {
        log.error('Failed to send notification email')
        rethrow(e)
    }
}
/**
 * Send out email notification regarding build trigger failure
 *
 * Email templates can be customised.
 * See https://confluence.trellix.com/display/PDS/Email+Notifications
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * notify.NotifyOnTriggerFailure("Radar", "Radar_Backend", "MB_Job", "master", "2", "The product does not exist.")
 * </pre>
 * @param product [T:String] The product name
 * @param component [T:String] The component name
 * @param job [T:String] The job name
 * @param branch [T:String] The branch name
 * @param parameters [T:String] The number of parameters
 * @param error [T:String] The error message
 */
void NotifyOnTriggerFailure(String product, String component, String job, String branch, String parameters, String error) {
    try {
        String reformattedEmail = EmailUtils.reformatEmails(env.EMAIL_DL);
        String templateString = libraryResource "org/mcafee/templates/triggerfailure.template"
        def templateFile = env.WORKSPACE + "\\" + StringUtils.getPaddedRandomNumber() + "triggerfailure.html.template"
        writeFile(file: templateFile, text: templateString)
        global()
        if (env.EMAIL_DL == null) {
            log.info("Environment variable EMAIL_DL is not set. Skipping sending an email notification.")
            return
        }
        def tokenMap = ["%PRODUCT%"   : product,
                    "%COMPONENT%" : component,
                    "%JOB%"       : job,
                    "%BRANCH%"    : branch,
                    "%PARAMETERS%": parameters,
                    "%BUILDURL%"  : env.BUILD_URL,
                    "%ERROR%"     : error
        ]
        utils.emailBuilder([
                target      : reformattedEmail,
                source      : global.ORBIT_AUTOMATION_EMAIL,
                subject     : 'Failed to trigger remote job from Orbit pipeline',
                template    : templateFile,
                logsAttached: null,
                tokenMap    : tokenMap
        ])
        log.info("Email notification has been sent.")
    }
    catch(Throwable e) {
        log.info("Failed to send an email notification: " + e.message)
    }
}
