import com.mcafee.orbit.Credentials.CredentialStore
import org.codehaus.groovy.runtime.StackTraceUtils
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.net.URLEncoder
import com.mcafee.orbit.Deprecated

/**
 * Runs a closure with a given timeout, a certain number of retries with a sleep applied 
 * between each retry. The sleep is multiplied by the current retry count.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.exponentialRetry { taskThatMayFail() }
 * </pre>
 * <pre>
 * utils.exponentialRetry(3, 5, 30) { taskThatMayFail() }
 * </pre>
 * @param retries [T:int] [OPTIONAL] The number of retries to do. [DEFAULT:3]
 * @param time_out [T:int] [OPTIONAL] The timeout in minutes to apply. [DEFAULT:30]
 * @param sleep_time [T:int] [OPTIONAL] The sleep time in seconds between each closure execution. [DEFAULT:0]
 * @param closure [T:Closure] The closure function to execute with the retries, timeout and sleep.
 */
def exponentialRetry(retries=3, time_out=30, sleep_time=0, closure) {
    int count = 0
    timeout(time_out) {
        retry(retries) {
            sleep(sleep_time*count)
            count = count + 1
            closure.call()
        }
    }
}

/**
 * Performs an HTTP POST request with a JSON payload.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.postJSON(
 *     url: "http://example.com/api/endpoint/",
 *     file: "payload.json",
 * )
 * </pre>
 * @param url [T:String] The URL to which the POST request will be sent.
 * @param file [T:String] The path to the JSON file.
 * @param customHeader [T:String] [OPTIONAL] Custom header to use for the request.
 * @return [T:String] The body of the response.
 */
@Deprecated(['request()', 'request.post()'])
String postJSON(Map arg) {
    deprecated()
    assert arg.file: "ERROR: arg.file should be a valid JSON filepath"
    assert arg.url: "ERROR: arg.url should be a valid URL"
    // used for output testing.
    String tempFileName = "outJSON" + Math.abs(new Random().nextInt() % 1000) + 1 + ".tmp"

    String outputAsString = httpwrapper.orbitPostCurl(
        url: arg.url,
        fileName: arg.file,
        customHeader: arg.customHeader,
        outputFile: tempFileName,
    )
    return outputAsString
}

/**
 * Checks if a given string is <code>null</code> or empty.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.isNullOrEmptyString("not empty")
 * </pre>
 * @param str [T:String] The string to test.
 * @return [T:boolean] <code>true</code> if given string is <code>null</code> or empty, <code>false</code> otherwise.
 */
public boolean isNullOrEmptyString(String str) {
    if (str == null || str.toLowerCase() == "null" || str ==~ /^[ ]*$/ || str.size() == 0) {
        return true
    }
    return false
}

/**
 * Checks the value of each key/value pair is <code>null</code> or empty.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.isNullOrEmptyInMap([ key1: "", key2: null ])
 * </pre>
 * @param map [T:Map] The map to test.
 * @return [T:boolean] <code>true</code>  if any value in the map is <code>null</code> or empty, <code>false</code> otherwise.
 */
public boolean isNullOrEmptyInMap(Map map) {
    for (def item in map.entrySet()) {
        log.debug("key = ${item.key}, value = ${item.value}")
        if (item.value == null || item.value ==~ /^\s+$/) {
            return true
        }
    }
    return false
}

/**
 * Checks if the file extension provided by the user is supported by Orbit.
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.validateFileFormat("helloworld.tar")
 * </pre>
 * @param String providedFile The string which to check the extension against
 * @param String type The type of extension
 */
private void validateFileFormat(String providedFile, String type) {
    switch (type) {
        case "7z":
            if (!providedFile.matches(".*(.zip|.tar|.zipx|.wim|.swm|.7z)\$")) {
                error "validateFileFormat() --> Unsupported file extension..."
            }
            break
        case "xml":
            if (!providedFile.matches(".*(.xml)\$")) {
                error "validateFileFormat() --> The required MA signing .xml file not provided..."
            }
    }
}

/**
 * Calls <code>this.checkFilesExists(file)</code>
 * @param file [T:String] The file to be checked
 * @return [T:boolean] <code>true</code> if the file exists in the directory
 * </pre>
 */
@Deprecated
private boolean checkFilesOsCall(String file) {
    deprecated()
    return checkFilesExists(file)
}

/**
 * Checks if the provided file by the user exists on the build node.
 * <ol>
 *    <li>Checks if the file exists using the built-in fileExists function.</li>
 *    <li>Failing that it runs a dir command against a file/ folder to check if it exists.</li>
 *    <li>Failing 1 & 2, it returns false.</li>
 * </ol>
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.checkFilesExists("file.txt")
 * </pre>
 * @param file [T:String] The file to be checked
 * @return [T:boolean] <code>true</code> if the file exists, otherwise returns <code>false</code>.
 */
private boolean checkFilesExists(String file) {
    def fileCallReturnCode
    String fileToCheck = "\"" + file + "\""
    boolean returnStatus = true

    log.debug("checkFilesExists running on node name: " + env.NODE_NAME)
    if (!fileExists(fileToCheck)) {
        log.debug("${fileToCheck} was not detected via fileExists, checking via dir/ls call.")
        String existsCheck = "dir " + fileToCheck
        if (isUnix()) {
            existsCheck = "ls " + fileToCheck
        }
        fileCallReturnCode = this.silentCmdWithStatus(existsCheck)

        if (fileCallReturnCode != 0) {
            log.error(" --> dir/ls call failed...")
            returnStatus = false
        } else {
            log.debug("dir/ls found : " + fileToCheck)
        }
    }
    return returnStatus
}

/**
 * Creates a zip file using 7zip.
 */
@Deprecated(['ziputils.createZip()'])
public void createZip(Map arg) {
    ziputils.createZip(arg)
}
/**
 * Unzips a file to a target folder using 7zip.
 */
@Deprecated(['ziputils.unzipFile()'])
public void unzipFile(Map arg) {
    ziputils.unzipFile(arg)
}
/**
 * Parses a string as a JSON object.
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.convertTextToJSON('{ "sample": "data" }')
 * </pre>
 * @param jsonText [T:String] A string in JSON Format.
 * @return [T:JSONObject] The parsed JSON object.
 */
@NonCPS
private convertTextToJSON(jsonText) {
    def result
    // Workaround for https://issues.jenkins-ci.org/browse/JENKINS-35140
    // Using JSON slurper causes java.io.NotSerializableException: groovy.json.internal.LazyMap
    // therefore use groovy.json.JsonSlurperClassic instead
    try {
        def slurper = new groovy.json.JsonSlurperClassic()
        result = slurper.parseText(jsonText)
        // JSON & XML Slurper objects can cause java.io.NotSerializableException if not unset before leaving scope.
        slurper = null
    } catch (Throwable e) {
        log.error('Failed to parse JSON: ' + jsonText)
        rethrow(e)
    }
    return result
}

/**
 * URL encodes a string.
 *
 * This will encode the entire string, so a full url should not be passed in.
 * Consider a loop, splitting a url at <code>/</code> and special characters intended to remain in the url.
 * Also escapes any ampersands, as curl strips them off as escape characters unless there is two of them.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.encodeURL("non-encodedstring")
 * </pre>
 * @param url [T:String] The string to encode, not necessarily a URL
 * @return [T:String] The encoded string
 */
@Deprecated
public String encodeURL(String url) {
    deprecated()
    def rebuiltURL = ""
    for (def each in url.split("/")) {
        rebuiltURL = rebuiltURL + URLEncoder.encode(each, "UTF-8") + "/"
    }
    return cli.escapeCLI(rebuiltURL)
}

/**
 * Decodes a URL encoded string.
 *
 * This is safe to run on a non-encoded string, unless that string contains UTF-8 encoded special characters
 * Also escapes any &s, as curl strips them off as escape characters unless there is two of them.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.decodeURL("encodedstring")
 * </pre>
 * @param url [T:String] The string to decode, not necessarily a URL
 * @return [T:String] The decoded string
 */
@Deprecated
public String decodeURL(String url) {
    deprecated()
    def rebuiltURL = ""
    for (def each in url.split("/")) {
        rebuiltURL = rebuiltURL + URLDecoder.decode(each, "UTF-8") + "/"
    }
    return cli.escapeCLI(rebuiltURL)
}

/**
 * Returns name of the function that called the currently executing function.
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.getCallingMethodName()
 * </pre>
 * @return [T:String] The method name.
 */
private getCallingMethodName() {
    def marker = new Throwable()
    return StackTraceUtils.sanitize(marker).stackTrace[2].methodName
}

/**
 * This function can be used to set retrievable build details
 */
private void defineBuildParameters() {
    def params = this.splitJenkinsJobName(env.JOB_NAME)
    env.BUILD_PARAM_PRODUCT = params.product
    env.BUILD_PARAM_COMPONENT = params.component
    env.BUILD_PARAM_COMPONENT_VERSION = params.componentVersion
}

/**
 * Splits a Jenkins job Name to get Product and component Information
 * This is only meant on standard job name format product/component
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.splitJenkinsJobName()
 * </pre>
 * @param jobName [T:String] Default is picked up from env.JOB_NAME
 * @return
 */
private Map splitJenkinsJobName(String jobName = null) {
    def splitJobNameObj = [:]
    String fullJobName = jobName ? jobName : env.JOB_NAME
    def splitFullJobName = fullJobName.split('/')
    def product, component

    if (splitFullJobName.size() > 1) {
        product = splitFullJobName[0]
        component = splitFullJobName[1]
    } else {
        product = null
        component = splitFullJobName.join('_')
    }

    splitJobNameObj.put("product", product)
    splitJobNameObj.put("component", component)
    splitJobNameObj.put("componentVersion", env.BUILDVERSION)

    log.debug("Product : ${splitJobNameObj.product}")
    log.debug("Component : ${splitJobNameObj.component}")
    log.debug("Component Version : ${splitJobNameObj.componentVersion}")

    return splitJobNameObj
}

/**
 * Parses the buildnumber from ecm Json Object
 * @param eCMJSON eCM Json Object
 * @return
 */
private String ecmBuildNumberFromJSON(eCMJSON) {
    def buildNumber = eCMJSON.findAll(/\"EcmBuildNumber\".*(\d+)/)
    return (buildNumber.replaceAll("[^\\d]", "").trim())
}

/**
 * Parses the buildversion from ecm Json Object
 * @param eCMJSON eCM Json Object
 * @return
 */
private String ecmBuildVersionFromJSON(eCMJSON) {
    def buildVersion = eCMJSON.findAll(/\"EcmVersion\".*(\d+)/)
    return (buildVersion.replaceAll("[^(\\d.)]", "").trim())
}

/**
 * Converts a CSV string to a Map.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * csvStringToMap("key1,value1\nkey2,value2")
 * </pre>
 * @param csvString [T:String] The CSV string
 * @return [T:Map] The parsed map
 */
public Map csvStringToMap(String csvString) {
    try {
        def resultMap = [:]
        def line = csvString.split("\\r?\\n")
        for (def i = 0; i < line.size(); ++i) {

            line[i] = line[i].toString().replace("\t", "").trim()
            def entry = line[i].split(",")
            if (entry.size() > 1) {
                def key = entry[0].toString().trim()
                def value = entry[1].toString().trim()
                if (entry.size() > 2) {
                    log.warn("Line too long: " + entry + ". Only setting Key: " + key + ", and Value: " + value)
                }
                resultMap.put(key, value)
            }
        }
        return resultMap
    } catch (Throwable e) {
        rethrow(e)
    }
}

/**
 * Build email content and send using Jenkins email-ext plugin.
 *
 * See https://confluence.trellix.com/display/PDS/Email+Notifications for more details.
 *
 * @param target [T:String] Target email address e.g. Email DL.
 * @param source [T:String] Source email address e.g. Team DL.
 * @param subject [T:String] Email subject.
 * @param template [T:String] [OPTIONAL] Email HTML template.
 * @param logsAttached [T:boolean] [OPTIONAL] - True if log is attached to the email.
 * @param tokenMap [T:Map] The keys are placeholders and values are the substitutions.
 */
public void emailBuilder(Map arg) {
    try {
        def templateFile
        //Accepts direct variables containing the template
        if (arg.template.split("<").length > 1) {
            templateFile = arg.template

            //Or the path to the template file
        } else {
            templateFile = readFile encoding: 'UTF-8', file: arg.template
        }

        //Overwrite tokenMap with environment variable
        if (!isNullOrEmptyString(env.TOKEN_CSV)) {
            arg.put("tokenMap", csvStringToMap(env.TOKEN_CSV))
        }

        //TODO: Test CSS file token override:
        if (!isNullOrEmptyString(arg.css)) {
            String cssFile = readFile(arg.css)
            templateFile = templateFile.replaceAll("%CSS%", cssFile)
        }

        //Searches the template for key tokens and replaces them with values
        for (def item in arg.tokenMap.entrySet()) {
            if (isNullOrEmptyString(item.value)) {
                log.warn("The token replacement key: " + item.key + ' has a null or empty value of: ' + item.value)
                log.warn("Deleting matches of the key: " + item.key)
                templateFile = templateFile.replaceAll(item.key, "")
            } else {
                templateFile = templateFile.replaceAll(item.key, item.value)
            }
        }

        if (arg.logsAttached) {
            emailext(
                    mimeType: 'text/html',
                    replyTo: arg.source,
                    subject: arg.subject,
                    to: arg.target,
                    body: templateFile,
                    attachLog: true,
                    compressLog: true
            )
        } else {
            emailext(
                    mimeType: 'text/html',
                    replyTo: arg.source,
                    subject: arg.subject,
                    to: arg.target,
                    body: templateFile
            )
        }
        echo 'Build notification sent'
    } catch (Throwable e) {
        log.error('Failed to send email notification')
        rethrow(e)
    }
}

private void mapReporter(Map map) {
    def output = ""
    for (def item in map.entrySet()) {
        output += "\n${item.key} (${item.value?.class?.name}): ${item.value}"
    }
    log.debug(output)
}

/**
 * Obsolete logging. Please use the `log` step.
 */
@Deprecated(['log.error()', 'log.warning()', 'log.info()', 'log.debug()'])
private void printlnDebug(String debugString) {
    deprecated()
    this.printDebug([debugString: debugString])
}

/**
 * Obsolete logging. Please use the `log` step.
 */
@Deprecated(['log.error()', 'log.warning()', 'log.info()', 'log.debug()'])
private void printDebug(Map arg) {
    deprecated()
    def debugLevel = 4
    if (arg.debugLevel) {
        debugLevel = arg.debugLevel as Integer
    }
    switch (debugLevel) {
        case 0:
        case 1:
            log.error(arg.debugString)
            break
        case 2:
            log.warn(arg.debugString)
            break
        case 3:
            log.info(arg.debugString)
            break
        default:
            log.debug(arg.debugString)
            break
    }
}

/**
 * Converts Unix path to Windows or Windows paths to Unix.
 *
 * @param path [T:String] The file path.
 * @return [T:String] The converted file path.
 */
public String formatPathForOS(String path) {
    log.debug("checking path OS compatibility for: " + path)
    try {
        String output
        if (isUnix()) {
            output = path.replace("\\", "/")
        } else {
            output = path.replace("/", "\\")
        }
        log.debug("path converted to: " + output)
        return output
    } catch (Throwable e) {
        log.error("Failed to format path for OS")
        rethrow(e)
    }
}
/**
 * Obsolete logging. Please use the `log` step.
 */
@Deprecated(['log.error()', 'log.warning()', 'log.info()', 'log.debug()'])
private void log(def message, def printAtDebugLevel = 3) {
    deprecated()
    printDebug([debugString: message, debugLevel: printAtDebugLevel])
}

/**
 * Disconnects an existing drive mapped to an external network folder. Windows only.
 * <h4>Sample usage:</h4>
 * <pre>utils.disconnectDriveMapping("M")</pre>
 *
 * @param driveLetter [T:String] The drive letter e.g. "M".
 */
public void disconnectDriveMapping(String driveLetter) {
    global()
    if (isUnix()) {
        //applicable only in Windows
        log.warn('disconnectDriveMapping is only implemented for Windows nodes')
        return
    }
    // Isolating letter, unless already isolated:
    if (driveLetter?.length() > 1) {
        driveLetter = driveLetter.take(1)
    }

    if (fileExists(driveLetter + ':\\')) {
        log.debug("attempting to unmap driveLetter:" + driveLetter)
        env.current_task = 'net use'
        bat "net use ${driveLetter}: /d /yes"
        global.driveLettersBeingUsed.put(driveLetter, false)
    }
}

/**
 * Validates a disk drive letter.
 * <h4>Sample usage:</h4>
 * <pre>utils.getValidDriveLetterString("M")</pre>
 *
 * @param driveLetter [T:String] driveLetter to be tested e.g. "M".
 * @return [T:String] The <code>driveLetter</code> if valid, <code>null</code> if input is invalid.
 */
public String getValidDriveLetterString(String driveLetter) {
    global()

    String letterString = driveLetter
    if (driveLetter?.length() > 1) {
        letterString = driveLetter.take(1)  //take only the first letter
    }

    assert letterString ==~ /^[a-zA-Z]$/: "ERROR:  Extracted letter " + letterString + " from provided driveLetter " + driveLetter + " does not match regex /^[a-zA-Z]\$/"
    if (global.permittedDriveLetters.contains(driveLetter)) {
        return letterString
    }

    return null

}

/**
 * Validates a disk drive letter.
 * <h4>Sample usage:</h4>
 * <pre>utils.getValidScratchPadDriveLetterString("M")</pre>

 * @param driveLetter [T:String] driveLetter to be tested e.g. "M".
 * @return [T:String] The <code>driveLetter</code> if valid, <code>null</code> if input is invalid.
 */
private String getValidScratchPadDriveLetterString(String driveLetter) {
    global()

    String letterString = driveLetter
    if (driveLetter?.length() > 1) {
        letterString = driveLetter.take(1)  //take only the first letter
    }

    assert letterString ==~ /^[a-zA-Z]$/: "ERROR:  Extracted letter " + letterString + " from provided driveLetter " + driveLetter + " does not match regex /^[a-zA-Z]\$/"
    if (global.scratchPadDriveLetters.contains(letterString)) {
        return letterString
    }
    return null
}
/**
 * Invoked by other orbit methods to get a drive letter from
 * the permitted drive letters for temporary network folder mapping.
 */
private String getAnAvailableDriveLetter() {
    def next = global.permittedDriveLetters.find {
        !global.driveLettersBeingUsed.get(it, false) && !fileExists("$it:\\")
    }
    if (!next) {
        error "Failed to find a valid drive letter to use."
    }
    return next
}

/**
 * Invoked by other orbit methods to get a drive letter from
 * the scratch pad drive letters for temporary network folder mapping.
 */
private String getAnAvailableScratchPadDriveLetter() {
    def next = global.scratchPadDriveLetters.find {
        !global.driveLettersBeingUsed.get(it, false) && !fileExists("$it:\\")
    }
    if (!next) {
        error "Failed to find a valid drive letter to use."
    }
    return next
}

/**
 * Disconnect drive letter
 *
 * <p>Disconnects an existing drive mapped to an external network folder</p>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.disconnectScratchpadDriveLetters("A")
 * </pre>
 *
 * @param [T:String] Drive letter to be disconnected
 */
public void disconnectScratchpadDriveLetters() {
    global()

    for (def i = 0; i < global.scratchPadDriveLetters.size(); i++) {
        def letter = global.scratchPadDriveLetters[i].toString()
        this.disconnectDriveMapping(letter)
    }
}

/**
 * Map a remote network folder to a drive letter
 *
 * <h4>Sample Usage:</h4>
 * <pre>
 * mappedDriveLetter = utils.mapNetworkFolder([
 *     remoteServer: "myserver.corp.entsec.com",
 *     folderName: "folderToMap",
 *     credentialName: 'CREDENTIAL_FOR_MYSERVER_DRIVE_MAPPING', // configured in jenkins
 *     remoteDomain: 'corpzone',
 *     useScratchPadDrives: true
 * ])
 * </pre>
 * @param remoteServer [T:String] Remote server name
 * @param folderName [T:String] Folder name-or-path in the remote server to be mapped
 * @param credentialName [T:String] Named credential in the Jenkins server for creating this mapping
 * @param remoteDomain [T:String] [OPTIONAL] Remote domain name , eg: "nai-corp". Defaults to "corpzone"
 * @param driveLetter [T:String] [OPTIONAL] Specific drive letter to map to. Use driveletter from orbit.permittedDriveLetters
 *     or use useScratchPadDrives = true
 * @param useScratchPadDrives [T:boolean] [OPTIONAL] set to true for automatically setting a random drive letter
 * @return [T:String] drive letter mapped to the network drive
 */
def mapNetworkFolder(arg) {
    if (arg.remoteServer.toLowerCase() == 'buildmaster.corp.entsec.com' && arg.folderName.toLowerCase().contains('fixed-1')){
        log.warn("Direct mapping to Fixed-1 detected, this will be redirected to Fixed-1\\Orbit.")
        log.warn("The keystore is still in a relative BServer path.")
        arg.folderName = 'Fixed-1\\Orbit'
    }
    String remoteFolderName = arg.folderName
    String driveLetterArg = arg.driveLetter
    String remoteServer = arg.remoteServer
    String credentialName = arg.credentialName
    String remoteDomain = arg.remoteDomain ? arg.remoteDomain+"\\": ""
    def useScratchPadDrives = arg.useScratchPadDrives
    //  incase we use one of  this.scratchPadDriveLetters
    def driveLetter

    log.debug(arg)
    if (arg.useScratchPadDrives == true) {
        driveLetter = getAnAvailableScratchPadDriveLetter()
        assert driveLetter != null: "ERROR: Failed to get a valid drive letter."
    } else {
        driveLetter = getValidDriveLetterString(driveLetterArg)
        assert driveLetter != null: "ERROR: Provided driveLetterArg " + driveLetterArg + " not allowed, use one of: " + global.permittedDriveLetters.join(',')
    }

    //check validity folder name
    assert remoteFolderName ==~ /^[a-zA-Z0-9-_\\]+$/: "ERROR:  Provided remoteFolderName " + remoteFolderName + " does not match regex ^[a-zA-Z0-9-_\\\\]+\$/"
    disconnectDriveMapping(driveLetter)
    def status
    try {
        withCredentials([
            usernamePassword(
                credentialsId: CredentialStore[credentialName], // FIXME
                usernameVariable: 'USERNAME',
                passwordVariable: 'PASSWORD'
            )
        ]) {
            //todo: unix
            env.current_task = 'net use'
            exponentialRetry(10, 30, 20) {   //https://technet.microsoft.com/en-us/library/cc940100.aspx Error 53 NetBIOS connectivity
                String netUseCmd = "net use " + driveLetter + ": \\\\" + remoteServer + "\\" + remoteFolderName + " /PERSISTENT:NO /user:" + remoteDomain + env.USERNAME + " " + env.PASSWORD + " 2>&1"
                status = this.silentCmdWithStatus(netUseCmd)
                if (status != 0) {
                    throw new Exception("The command returned with non-zero exit code")
                }
            }
        }
    } catch (Throwable e) {
        rethrow(
            e,
            "Unable to map drive " + driveLetter + ":  to \\\\" + remoteServer + "\\" + remoteFolderName,
            true
        )
    }
    return driveLetter
}

/**
 * Executes a command silently in a shell on the build node and returns the standard out.
 * <h4>Sample usage:</h4>
 * <pre>utils.silentCmd("echo hello world")</pre>
 * @param cmd [T:String] The command to execute.
 * @return [T:String] The standard out returned by the command.
 */
String silentCmd(String cmd) {
    def wrappingTask = env.current_task
    env.current_task = cmd

    def returnVal = ""
    if (isUnix()) {
        returnVal = sh(
                script: "{ set +x; } 2>/dev/null; " + cmd + "; { set -x; } 2>/dev/null",
                returnStdout: true
        ).trim()
    } else {
        returnVal = bat(
                script: "@ " + cmd,
                returnStdout: true
        ).trim()
    }
    log.debug("Command: " + cmd)
    log.debug("Return: " + returnVal)
    env.current_task = wrappingTask
    return returnVal
}

/**
 * OS agnostic environment variable retrieval.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * def label = utils.getNodeVar('REUSE_LABEL')
 * </pre>
 * @param varName [T:String] The name of the variable to get the value of from the node.
 * @return [T:String] The value of the variable.
 */
@Deprecated
private def getNodeVar(String varName) {
    deprecated()
    if (varName){
        if(isUnix()){
            command = 'echo $' + varName
        } else {
            command = 'echo %' + varName + '%'
        }
        varName = this.silentCmd(command)
    }
    return varName
}

/**
 * Executes a command silently in a shell on the build node and returns the exit code.
 * <h4>Sample usage:</h4>
 * <pre>utils.silentCmdWithStatus("echo hello world")</pre>
 * @param cmd [T:String] The command to execute.
 * @return [T:String] The exit code returned by the command.
 */
int silentCmdWithStatus(String cmd) {
    env.current_task = cmd
    log.debug("Command: " + cmd)
    int returnStatus = 0
    if (isUnix()) {
        returnStatus = sh(
                script: "{ set +x; } 2>/dev/null; " + cmd + "; { set -x; } 2>/dev/null",
                returnStatus: true
        )
    } else {
        returnStatus = bat(
                script: "@ " + cmd,
                returnStatus: true
        )
    }
    log.debug("Return status: " + returnStatus)
    return returnStatus as int
}

/**
 * Checks the list of build nodes to compile a list of nodes that match the supplied label, are online, and are not busy.
 * Note that the busy count is time dependant and is only accurate for when it was recorded. Nodes may become busy between the execution of this method and the use of the returned list.
 * <h4>Sample usage:</h4>
 * <pre>
 * def nodes = utils.labelBusyCheck(nodeLabelParam)
 * </pre>
 * @param nodeLabel [T:String] The string supplied to the job as the desired node label
 * @return [T:List] The of build nodes ready to build at this time
 */
@NonCPS
@Deprecated
private List labelBusyCheck(String nodeLabel) {
    deprecated()
    def freeNodes = []
    for (def each in hudson.model.Hudson.instance.slaves) {
        if (each.getLabelString().equals(nodeLabel)) {
            if (!Jenkins.getInstance().getComputer(each).isOffline()) {
                if (Jenkins.getInstance().getComputer(each).countBusy() == 0) {
                    freeNodes.add(each.name)
                }
            }
        }
    }
    return freeNodes
}

/**
 * Throws an error with the provided message.
 * <p>Note this method has a misleading name, as it does not return anything.</p>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.getErrorMessage("Tests have failed")
 * </pre>
 * @param message [T:String] The error to throw.
 */
public void getErrorMessage(String message) {
    log.error(message)
    error message
}

/**
 * Replaces file contents with those described in a token map.
 *
 * <p>Replaces each matched key string in the map with its respective value string.</p>
 *
 * <h4>Sample usage:</h4>
 *
 * <pre>
 * utils.searchAndReplace(
 *     fileToReplace: "info.txt",
 *     input: ["@BUILDNUMBER@" : env.ORBIT_BUILD_NUMBER],
 * )
 * </pre>
 * <pre>
 * utils.searchAndReplace(
 *     fileToReplace: "info.txt",
 *     input: ["1\\.0": env.BUILDVERSION + "." + env.BUILD_NUMBER],
 *     replacementFile: "newInfoFile.txt",
 *     encoding: "UTF-8",
 * )
 * </pre>
 *
 * @param fileToReplace [T:String] File which contains the strings to be replaced.
 * @param input [T:Map] The map which contains keys value pairs to be replaced. Note: keys are regular expressions.
 * @param replacementFile [T:String] [OPTIONAL] New file which to output the updated contents to. [DEFAULT:fileToReplace]
 * @param encoding [T:String] [OPTIONAL] The character encoding to use.
 *                 Valid options include:
 *                 <code>"US-ASCII"</code>, <code>"ISO-8859-1"</code>, <code>"UTF-8"</code>, <code>"UTF-16BE"</code>, <code>"UTF-16LE"</code> or <code>"UTF-16"</code>.
 *                 [DEFAULT:"UTF-8"]
 */
void searchAndReplace(Map args) {
    log.debug(args)
    if (!args.input || args.input.size() == 0) {
        log.error('No input map provided')
        getErrorMessage('Failed to execute searchAndReplace()')
    } else if (!(args.input in Map)) {
        log.error('input is not a Map')
        getErrorMessage('Failed to execute searchAndReplace()')
    } else if (!fileExists(args.fileToReplace)) {
        log.error('fileToReplace does not exist')
        getErrorMessage('Failed to execute searchAndReplace()')
    }
    if (!args.encoding) {
        args.encoding = 'UTF-8'
    }
    if (!args.replacementFile) {
        args.replacementFile = args.fileToReplace
    }
    String content = readFile(file: args.fileToReplace, encoding: args.encoding)
    log.debug("Before replacement:\n$content")
    for (def item in args.input.entrySet()) {
        content = content.replaceAll(
            item.key.toString(),
            item.value.toString()
        )
    }
    log.debug("After replacement:\n$content")
    writeFile(file: args.replacementFile, text: content, encoding: args.encoding)
}

/**
 * Prints the contents of a file.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * displayFile("myFile.txt")
 * </pre>
 * @param fileName [T:String] The path to the file
 */
public void displayFile(String fileName) {
    fileName = this.formatPathForOS(fileName)
    if (!fileExists(fileName)) {
        log.warn("File " + fileName + " does not exist. Cannot print content.")
        return
    }
    if (isUnix()) {
        sh 'cat ' + fileName
    } else {
        bat 'type ' + fileName
    }
}

/**
 * Executes <code>robocopy</code> on the build node.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.runRobocopy(
 *     src: "path/to/source/folder",
 *     dest: "path/to/destination/folder",
 *     file: "*.zip",
 * )
 * </pre>
 * @param src [T:String] The source folder.
 * @param dest [T:String] The destination folder.
 * @param file [T:String] [OPTIONAL] File matching string. [DEFAULT:"*"]
 * @param flags [T:String] [OPTIONAL] Any custom flags to be appended to the command.
 *        [DEFAULT:"/mt /R:60 /W:60 /v /E"]
 * @param replaceFlags [T:boolean] [OPTIONAL] If <code>true</code> will replace the flags instead of appending to them. [DEFAULT:false]
 */
def runRobocopy(arg) {
    log.info("copying " + arg.src + " to " + arg.dest)
    if (isUnix()) {
        log.warn("RunRobocopy was run on a unix node, which does not support advanced copying strategies. Copy may have run unsuccessfully.")
        // If for some reason runRobocopy is called from a unix node, run a basic cp command and print a warning.
        // Expects a src and a dest. src defaults to *. There is not extensive testing of inputs here because this method should not be called from a non-windows node.
        if (!arg.file) {
            arg.file = "*"
        }
        sh "cp " + arg.src + "/" + arg.file + " " + arg.dest
        return
    }
    // This block is for removing characters that will mess things up. Edit as needed.
    for (int i=0 ; i < arg.size() ; i ++ ) {
        if(arg[i] instanceof String){
            arg[i].replaceAll('"', "")
        }
    }
    def file = '*'
    if (arg.file) {
        file = arg.file.replaceAll("\"","").replaceAll("\'","")
    }

    // If a file (or wildcard) is given instead of a directory for the src, fix the path of the src for use in robocopy
    // Overwrites the default of *
    // Including the file in the src directory name should not be encouraged, but this logic is here in case it is
    if (arg.src.contains("*") || new File(arg.src).isFile()) {
        file = arg.src.tokenize('\\').last().toString()
        arg.src = arg.src - file
    }

    def flags = '/mt /R:60 /W:60 /v /E'
    if (arg.flags) {
        flags = flags + " " + arg.flags
        if (arg.replaceFlags?.toBoolean()) {
            flags = arg.flags
        }
    }

    // If the given file is a path instead of a single name, split it up and append the path part to the src
    def filePath = file.tokenize("\\")
    if (filePath.size() > 1 && (file.charAt(file.size() - 1) == "\\")) {
        file = filePath.last()
        arg.src += "\\" + filePath.dropRight(1).join("\\")
    }

    // dest should not contain any asterisks
    String[] list =  arg.dest.tokenize("\\")
    list.each{
        if (it.contains("*")) {
            arg.dest -= it
        }
    }
    
    // Robocopy is particular about slashes on the ends of directories when quotes are also involved, so this is a check to make sure there's always a \. on the end.
    def formatPath = { original ->
        def clean = original.tokenize("\\").join("\\") + "\\"
        if(original.take(2) == "\\\\"){
            clean = "\\\\" + clean
        }
        return clean
    }

    def robocopyCommand = "robocopy \"${formatPath(arg.src)}.\" \"${formatPath(arg.dest)}.\" \"${file}\" ${flags}"
    log.debug("Robocopy command string: " + robocopyCommand)
    def roboOutput = bat returnStatus: true, script: robocopyCommand

    //For more information on odd hex (addition) error codes used by robocopy see: https://ss64.com/nt/robocopy-exit.html
    if (roboOutput > 7) {
        log.error("Robocopy returned critical exit code " + roboOutput)
        error "Robocopy returned exit code " + roboOutput
    }
    if (arg.rename) {
        bat "move /y " + arg.dest + "\\" + file + " " + arg.dest + "\\" + arg.rename
    }

}

/**
 * Appends a list of files into one file.
 * <ol>
 *   Takes in a list of files as an array and output file as a String.</li>
 *   Checks if the provided files exists on the build node.</li>
 *   Generates the required command for appending the list of files into a single file.</li>
 *   Creates and executes different appending commands based on the OS.</li>
 * </ol>
 * <h4>Sample usage:</h4>
 * <pre>
 *     def file1 = srcDir + '\appendFile2File\\hello1.txt'
 *     def file2 = srcDir + 'appendFile2File\\hello2.txt'
 *
 *     def fileList = [file1, file2]
 *     utils.appendListofFilesToFile(fileList,  "hello_World_Output.txt")
 * </pre>
 * @param requiredFiles [T:List] List of files which  are to be combined and outputted to the provided file.
 * @param outputFile [T:List] File which to output the new contents to.
 */
private void appendListofFilesToFile(List requiredFiles, String outputFile) {

    def requiredCommandString = ""
    def appendList
    def listAppendCommand

    for (int i = 1; i <= requiredFiles.size(); i++) {
        this.checkFilesExists(requiredFiles[i - 1])
        if (requiredFiles[i - 1] == requiredFiles[requiredFiles.size() - 1]) {
            requiredCommandString = requiredCommandString + requiredFiles[i - 1]
        } else {
            requiredCommandString = requiredCommandString + requiredFiles[i - 1] + " + "
        }
    }

    if (isUnix()) {
        requiredCommandString = requiredCommandString.replaceAll(" + ", " ")
        listAppendCommand = "cat ${requiredCommandString} > ${outputFile}"
        log.debug("ERROR: Running the following command:" + listAppendCommand)
        appendList = sh(returnStatus: true, script: listAppendCommand)

    } else {
        listAppendCommand = "copy /b ${requiredCommandString} ${outputFile} /y"
        log.debug("ERROR: Running the following command:" + listAppendCommand)
        appendList = bat(returnStatus: true, script: listAppendCommand)
    }
    if (appendList == 0) {
        log.debug(listAppendCommand + " command ran succesfully")
    } else {
        getErrorMessage("Failed on the following command: " + listAppendCommand)
    }
}

/**
 * Appends contents of one file to another file.
 * <h4>What it Does:</h4>
 * <ol>
 *   <li>Takes in fileToAppend and fileToAppendTo, both as String.</li>
 *   <li>Reads the contents of fileToAppend, adds those contents with contents of fileToAppendTo.</li>
 *   <li>Pushes the new contents to fileToAppendTo.</li>
 *   <li>Includes code and input validation.</li>
 * </ol>
 * <h4>Sample usage:</h4>
 * <pre>
 * def fileOfInterest = "old_contents.txt"
 * def searchAndReplaceFileString = "new_contents.txt"
 * utils.appendFiletoFile(fileOfInterest, fileToAppendTo)
 * </pre>
 * @param String fileToAppend, file which contents need to append.
 * @param String fileToAppendTo, file to which the fileToAppend contents need to be appended to.
 */
private void appendFiletoFile(String fileToAppend, String fileToAppendTo) {
    if (fileExists(fileToAppend)) {
        log.debug("Required file to append found.")
    } else {
        this.getErrorMessage("Could not find " + fileToAppend)
    }

    if (fileExists(fileToAppendTo)) {
        log.debug("Required file to append to found.")
    } else {
        this.getErrorMessage("Could not find " + fileToAppendTo)
    }

    def fileToAppendString = readFile file: fileToAppend
    def fileToAppendToString = readFile file: fileToAppendTo
    def fileToAppendArray = fileToAppendString.split("\n")
    def newFileContents = ""

    for (String item : fileToAppendArray) {
        newFileContents = newFileContents + (item + "\n")
    }
    fileToAppendToString = newFileContents + fileToAppendToString

    log.debug("The following contents will be written to " + fileToAppendTo)
    log.debug(fileToAppendToString)

    writeFile(file: fileToAppendTo, text: fileToAppendToString)

    def newContentsFile = readFile file: fileToAppendTo
    if (newContentsFile == fileToAppendToString) {
        log.debug("Contents of " + fileToAppend + " succesfully appended to " + fileToAppendTo)
    } else {
        this.getErrorMessage("Contents of " + fileToAppend + " succesfully appended to " + fileToAppendTo)
    }
}

/**
 * Generates a compressed <code>.cab</code> file and stores it in the provided location.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.runCabArcOnFiles(
 *    "hello_world_install.1.0.0.x86_64-Winqual.cab",
 *    "hello_world\\Install32Files",
 *    "hello_world\\TMPCABFILES"
 * )
 * </pre>
 * <pre>
 * utils.runCabArcOnFiles(
 *    "hello_world_install.1.0.0.x86_64-Winqual.cab",
 *    "hello_world\\Install32Files",
 *    "hello_world\\TMPCABFILES",
 *    "E:\\tools\\MSPlatformSDK_2003R2\\Bin\\CabArc.Exe"
 * )
 * </pre>
 * @param cabFileName [T:String] The name of the cab file.
 * @param fileLocation [T:String] directory which contains the files to to be compressed into the cab file.
 * @param tmpCabLocation [T:String] location which newly generated cab file will be outputted to.
 * @param cabArcLocation [T:String] [OPTIONAL] Alternative location for the <code>CabArc.exe</code>. [DEFAULT:"E:\tools\IExpress\CABARC.exe"]
 */
public void runCabArcOnFiles(String cabFileName, String fileLocation, String tmpCabLocation, String cabArcLocation = "E:\\tools\\IExpress\\CABARC.exe") {
    /*
        1. Checks if an optional location of CABARC.EXE has been provided
        2. Goes to a temporary location of the cab file
        3. Executes the cabarc.exe on the objectDir which is the location of the files to be compressed
        4. Validation and necessary verbose provided to the user
     */
    if (cabArcLocation != "E:\\tools\\IExpress\\CABARC.exe") {
        log.debug("Checking for provided cabArc location...")
        if (fileExists(cabArcLocation)) {
            log.debug("Found cabArc in " + cabArcLocation)
        } else {
            this.getErrorMessage("Unable to locate cab arc at " + cabArcLocation)
        }
    }
    def localTmpCabLocation
    log.debug("runCabArcOnFiles: Creating ${cabFileName}...")

    def relativePath = fileLocation
    log.debug("Relative Path starts as: ${relativePath}")

    relativePath = relativePath.substring(3)    //Remove drive letter
    log.debug("Relative Path is now: ${relativePath}")

    if (fileExists(pwd() + tmpCabLocation)) {
        localTmpCabLocation = tmpCabLocation
    } else {
        def generateTempCabLocation = bat(returnStatus: true, script: "mkdir " + pwd() + tmpCabLocation)
        if (generateTempCabLocation == 0) {
            log.debug("Provided temporary location for cab file not found but has been succesfully generated.")
        } else {
            getErrorMessage("Failed to generate directory for the provided temporary cab arc location.")
        }
        localTmpCabLocation = tmpCabLocation
    }

    dir(localTmpCabLocation) {
        if (fileExists(cabArcLocation)) {
            def runCabArc = bat(returnStatus: true, script: "${cabArcLocation} -m lzx:21 -s 6144 -r -p -P \"${relativePath}\"  n \"${cabFileName}\" \"${fileLocation}\\*\"")
            if (runCabArc == 0) {
                log.debug("Succesfully generated the required cab file.")
            } else {
                this.getErrorMessage("Failed to generate required cab arc file.")
            }
        } else {
            this.getErrorMessage("Unexpected: Could not locate the required CabArc.exe at ${cabArcLocation}. Can not create ${cabFileName}.")
        }
    }
}

/**
 * Return map with all the values type cast to String.
 * Note: keys are unaffected.
 *
 * @param randomHashMap [T:Map] A map which has values that need to be converted to type string.
 * @return [T:Map] The map provided but converted if needed to have a string type values.
 */
private Map convertMapValuesToTypeString(Map randomHashMap) {
    Map<Object, String> convertedMap = [:]
    if (randomHashMap.size() == 0) {
        error "ERROR: Map ${randomHashMap} provided to convertMapValuesToTypeString is empty."
    }
    log.debug("Map type is ${randomHashMap.getClass()}")
    log.debug("Listing Map and adding to temporary Map which will be returned")

    for (def n = 0; n < randomHashMap.entrySet().size(); n++) {
        def item = randomHashMap.entrySet()[n]
        log.debug(
            "key: ${item.key} key type : ${item.key.getClass()} \n " +
            "value: ${item.value} \t value type : ${item.value.getClass()}"
        )
        convertedMap.put(item.key, item.value.toString())
    }
    log.debug("The following value types should be of type string")

    for (def n = 0; n < convertedMap.entrySet().size(); n++) {
        def item = convertedMap.entrySet()[n]
        log.debug(
            "key: ${item.key} key type : ${item.key.getClass()} \n " +
            "value: ${item.value} \t value type : ${item.value.getClass()}"
        )
        assert item.value.getClass() == String: "Value ${item.value} for Key " +
                "${item.key} can not be convert to type string."
    }

    convertedMap
}

/**
* Send email notification email to dev team.
* <b>Sample usage:</b>
* <pre>
* utils.sendEmailNotification(
*    subject: "Email Notification",
*    messsage: "JIRA has been opened",
* )
* </pre>
* @param subject [T:String] Subject for the Email.
* @param message [T:String] Message body for the Email.
* @param emailDL [T:String] Email address to which mail is to be sent.
*/
private void sendEmailNotification(def arg) {
    log.info("Sending email alert to: ${arg.emailDL}")
    emailext(
        body: arg.message,
        replyTo: config('ORBIT_CORE_TEAM_EMAIL'),
        subject: arg.subject,
        to: arg.emailDL,
    )
}

/**
 * Accepts a build override comma seperated string for (Component_version,build,package)
 * Parses the build override and returns a map for component_version, build and package.
 *
 * <h4>Sample Usages:</h4>
 * <pre>
 *     def response = utils.splitOrbitOverride("2.0.0,3,0")
 *     assert response.build == "3"
 * </pre>
 *
 * @param buildOverride [T:String] Used to describe the version(s), build and package details.
 * @return [T:Map] Returns the set of data as a map with properties component version, build and package.
 */
private Map splitOrbitOverride(String buildOverride) {
    HashMap overrides = [:]
    List sortList = buildOverride.tokenize(",")
    int listSize= sortList.size()
    log.debug("listSize = " + listSize)
    log.debug("sortList = " + sortList)
    if (listSize == 3){
        overrides.component = sortList[0]
        overrides.build = sortList[1]
        overrides.package = sortList[2]
    } else if (listSize ==2) {
        overrides.component = sortList[0]
        overrides.build = sortList[1]
        overrides.package = null
    } else if(listSize ==1) {
        overrides.component = sortList[0]
        overrides.build = null
        overrides.package = null
    } else {
        overrides.component = null
        overrides.build = null
        overrides.package = null
    }

    log.debug("Overrides : ${overrides}")
    return overrides
}
/**
 * Get the Cloud template details
 * <h4>Sample usage:</h4>
 * <pre>
 * utils.getCloudTemplateDetails("mlos3_latest")
 * </pre>
 * @param labelName [T:String] The label
 * @return [T:Map] The cloud template details
 */
@NonCPS
def getCloudTemplateDetails(String labelName) {
    def template = Jenkins.getInstance()
            .clouds.find {it.name == 'kubernetes'}
            .templates.find{it.label.split(' ').contains(labelName)}
    return (template ? 
            [
                "NodeSelector": template.nodeSelector.replace('nodeType=','').toUpperCase(),
                "Image": template.image
            ] : null)
}
/**
 * Returns the Radar id of the current build node.
 * @return [T:int] The Radar id of the build node.
 */
public int getBuildNodeID() {
    log.debug("Getting build node id...")
    def id = orbit.buildNode(env.NODE_NAME)
    if (!id) {
        throw new RuntimeException(
            "Unknown build node '${env.NODE_NAME}' when getting build node id."
        )
    }
    return id
}

/**
 * This is a helper method to remove unnecessary files to reduce HDD requirements for builds.
 *
 * @param fileNames [T:List&lt;String&gt;] The list of files to remove.
 */
public void deleteFiles(List<String> fileNames) {
    String removeFiles = isUnix() ? "rm -r " : "del "
    removeFiles += fileNames.collect { "\"$it\"" }.join(' ')
    log.info("Removing $fileNames")
    int returnCode = this.silentCmdWithStatus(removeFiles)
    if(returnCode != 0) {
        log.error("Error removing files: $fileNames, continuing build")
    }
}

/**
 * Sets up Git Lfs before the checkout of a git repository.
 *
 * @param lfsRepo [T:String] The name of the lfs repository in artifactory.
 */
private void setupGitLfs(def lfsRepo) {
    if (lfsRepo) {
        global()
        def artifactoryInstance = radar.getArtifactoryInstanceByName(env.ARTIFACTORY_INSTANCE)
        withCredentials([
            usernamePassword(
                credentialsId: artifactoryInstance['CredentialId'],
                usernameVariable: 'USERNAME',
                passwordVariable: 'PASSWORD'
            )
        ]) {
            def credential = URLEncoder.encode(env.USERNAME, "UTF-8") + ':' + URLEncoder.encode(env.PASSWORD, "UTF-8")
            if (!isUnix()) {
                credential = credential.replaceAll('%', '%%')
            }
            def settings = request.get(
                url: global.radarUrl.ProxySettings,
                token: 'orbit_radar_cred',
                error: 'Failed to get the proxy settings from Radar.',
            )
            execute 'git init .'
            for (def domain in settings.NoProxy) {
                execute "git config --global http.https://${domain}.proxy \"\""
                execute "git config --global http.noproxy $domain"
            }
            execute 'git lfs install --skip-smudge'
            execute "git config --local lfs.url \"https://$credential@${artifactoryInstance['Domain']}/artifactory/api/lfs/$lfsRepo\""
        }
    }
}

/**
 * Returns the commit sha of the current build.
 * <p>This function removes the directory that the repository was checked out to, unless you specify the directory name parameter.</p>
 * @param directory Name of the directory to checkout the commit sha into.
 * @param bomtype The BomType to store. Defaults to 'SCM'.
 * @param lfsRepo The over-ride for the lfs repository in artifactory.
 * @return The commit sha
 */
private String checkoutBuildSHA(String directory, String bomtype = "SCM", String lfsRepo = null, boolean returnBom = false) {
    def scmVars
    List validBomTypes = ["Build", "SCM"]
    int lfsTimeout = env.ARTIFACTORY_TIMEOUT ? env.ARTIFACTORY_TIMEOUT.toInteger(): 45 
    int gitCloneTimeout = env.GIT_CLONE_TIMEOUT ? env.GIT_CLONE_TIMEOUT.toInteger(): 30
    try {
        if(!validBomTypes.any{it.contains(bomtype)}){ throw new Exception("BomType \'${bomtype}\' is not valid.") }
        dir(directory) {
            setupGitLfs(lfsRepo)
            scmVars = checkout([
                $class: 'GitSCM',
                branches: scm.branches,
                doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
                extensions: scm.extensions + [
                                [$class: 'CloneOption', timeout: gitCloneTimeout],
                                [$class: 'CheckoutOption', timeout: lfsTimeout]
                ],
                submoduleCfg: [],
                userRemoteConfigs: scm.userRemoteConfigs
            ])
        }
        log.debug("bomtype: "+bomtype)
        def branchName = scmVars.GIT_BRANCH.replaceFirst(/^origin\//, '')
        log.info("Checked out branch: ${branchName}")
        //Report checkout to BOM
        bomResponse = bom.scmBOM([
            bomType: bomtype,
            scmType: "GIT",
            scmCredential: 'UNKNOWN',
            scmVersion: git.getGitVersion(),
            scmCommit: scmVars.GIT_COMMIT,
            scmBranch: branchName,
            lfsRepository: lfsRepo,
            scmUrl: scmVars.GIT_URL
        ])

        if(bomtype.equals("Build")){
            if(isUnix()) {
                sh "rm -rf ${directory} ${directory}@tmp"
            }
            else {
                bat "rmdir /Q /S ${directory} ${directory}@tmp"
            }
        }
    }
    catch (Throwable e) {
        rethrow(e, 'Failed in checkoutBuildSha', true)
    }
    def sha = scmVars.GIT_COMMIT
    if (!orbit.buildSha()) {
        orbit.buildSha(sha)
    }
    if(returnBom) {
        log.debug("Returning entire BOM response which includes the commit sha.")
        return bomResponse
    }
    return sha
}

/**
 * Reporting for stage failures
 */
@Deprecated(['stage()'])
private void stageError(args) {
    deprecated()
    radar.addBuildLog(
        "Error",
        "Generic_Stage",
        args.stage,
        "None",
        args.error.message,
    )
    rethrow(args.error)
}

/**
 * Updates the stage result in radar
 */
@Deprecated
private void stageFinal(args) {
    deprecated(false)
}

@Deprecated
private void uploadSDKDocs(Map args) {
    deprecated(false)
}
