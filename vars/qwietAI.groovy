import com.mcafee.orbit.Utils.StringUtils

/**
 * Performs a QwietAI static analysis. Full documentation on https://docs.shiftleft.io/sast/analyzing-applications/oss-vulnerabilities
 *
 * Sample usage:
 * qwietAI([language:'java',pathToCode:''])
 *
 * @param pathToCode [T:String] path to the source code to scan
 * @param language [T:String][OPTIONAL] The source code language to scan (java,javasrc, c, csharp, go, python, js,multi(language), default is NOT Specified
 * @param appName [T:String][OPTIONAL] app Name for Qwiet AI project, defaults to Component.
 * @param appGroup [T:String][OPTIONAL] app Group to organise Qwiet AI projects, defaults to Product.
 * @param sourceCodeId [T:Integer] [OPTIONAL] The SCM Source Code ID to link to this scan in Radar
 * @param branch [T:String] [OPTIONAL] SCM Branch for Static Analysis code tool, defaulting to "Default"
 * @param timeout [T:String] [OPTIONAL] Custom timeout value for analysis-timeout and cpg-generation-timeout
 *      e.g. <code>"240m0s"</code>
 * @param customOptions [T:String] [OPTIONAL] Static Analysis custom options that are passed before the <code>--</code> 
 *      in the form of <code>"--team <teamName>..."</code>
 *      https://docs.shiftleft.io/cli/reference/analyze
 * @param additionalFlags [T:String] [OPTIONAL] Static Analysis additional flags that can be passed at the end of the command after the <code>--</code>
 *      in the form of <code>"--exclude <path-1>,<path-2>,..."</code>
 *      https://docs.shiftleft.io/cli/reference/analyze
 * @param errorString [T:String] [OPTIONAL] User specified error string to check the Static Analysis logfile for.
 *      This can be a string, string containing a regex, etc.
 *      If provided, buildStatus will be set if the buildLog contains the error string
 *      e.g. <code>"My Error String"</code>
 * @return [T:int] <code>0</code> if build succeeds.
 */
int call(Map options) {
    int returnStatus = 0
    log.debug("Starting QwietAI static analysis.")
    utils.mapReporter(options)
    Boolean skipQwietAI = config('DISABLEQWIETAI')?.toBoolean() ? true : false

    if (skipQwietAI) {
        log.info('Skipping Static Analysis as per configuration settings')
        return returnStatus
    }
    utils.defineBuildParameters()
    arguments(options) {
        addString('appName',env.BUILD_PARAM_COMPONENT)
        addString('appGroup',env.BUILD_PARAM_PRODUCT)
        addString('language','')
        addString('pathToCode', '.')
        addString('branch', null)
        addInteger('sourceCodeId', null)
        addString('customOptions','')
        addString('additionalFlags', '')
        addString('errorString','')
        addString('error', 'Qwiet AI Static Analysis Failed.')
        parse()
    }
    
    if(!options.sourceCodeId) {
        log.warn("Parameter sourceCodeId must be provided to link the scan its Build in Radar. It is available on setting returnBom=true in the addGit/checkoutBuildSHA/git.gitMain API calls.")
    }

    String errorString = options.errorString ?: ""
    String logFileName = "qwietAI_${options.appName}_${options.branch}_${options.language}.log".replaceAll("/", "_")
    String jsonFileName = "qwietAI_${options.appName}_${options.branch}_${options.language}.json".replaceAll("/", "_")
    String qwietAILog = utils.formatPathForOS(env.WORKSPACE + "\\.orbit\\" + logFileName)
    String qwietAIJsonOutput = utils.formatPathForOS(env.WORKSPACE + "\\.orbit\\" + jsonFileName)
    String pathToCode = utils.formatPathForOS(options.pathToCode)

    if(fileExists(qwietAILog)) {
        utils.deleteFiles([qwietAILog])
    }
    if(fileExists(qwietAIJsonOutput)) {
        utils.deleteFiles([qwietAIJsonOutput])
    }
    log.info("Perform QwietAI Static Analysis for " + options.appName)
    
    try {
        log.info("QwietAI Static Analysis")
        def bin_path = install()
        String executeQwietAI = bin_path + " analyze --verbose --app \"${options.appName}\""
        executeQwietAI += " --structured-output --structured-output-format JSON --structured-output-file-path \"${qwietAIJsonOutput}\""
        executeQwietAI += " --tag app.group=\"${options.appGroup}\""
        if(options.branch) {
            executeQwietAI += " --tag branch=\"${options.branch}\""
        }
        String QWIET_ENABLE_MULTI_LANGUAGE_ANALYSIS = 'false'
        String timeout = options.timeout ?: config("QWIETAI_TIMEOUT")

        auth(bin_path, options.appName)
        if (options.language == 'multi') {
            QWIET_ENABLE_MULTI_LANGUAGE_ANALYSIS = 'true'
        } else if (options.language == '') {
            log.info('No language specified')
        } else {
            executeQwietAI += ' --' + options.language
        }
        if (!StringUtils.isNullOrEmpty(options.customOptions)) {
            executeQwietAI += ' ' + options.customOptions
        }
        executeQwietAI += ' --analysis-timeout ' + timeout + ' --cpg-generation-timeout ' + timeout
        executeQwietAI += ' --strict'
        executeQwietAI += ' ' + pathToCode
        if (!StringUtils.isNullOrEmpty(options.additionalFlags)) {
            executeQwietAI += ' -- ' + options.additionalFlags
        }
        executeQwietAI += ' > ' + qwietAILog + ' 2>&1'
        log.info("Performing QwietAI Static Analysis : ")
        withEnv(["QWIET_ENABLE_MULTI_LANGUAGE_ANALYSIS=${QWIET_ENABLE_MULTI_LANGUAGE_ANALYSIS}"]) {
            int qwietAIStatus = utils.silentCmdWithStatus(executeQwietAI)
            if(env.DEBUGLEVEL=='4' && qwietAIStatus!=0) {
                log.debug("Printing QwietAI log file...")
                utils.displayFile(qwietAILog)
                log.debug("Printing QwietAI JSON log file...")
                utils.displayFile(qwietAIJsonOutput)
            }
            artifacts.upload([files: [qwietAILog, qwietAIJsonOutput]])
            String errorCheck = "invalid, or incomplete access token found"
            if (qwietAIStatus == 0) {
                qwietAIStatus = buildKPIs.verifyBuild(qwietAILog, errorCheck)
                errorCheck='Command has exceeded timeout'
                qwietAIStatus -= buildKPIs.verifyBuild(qwietAILog, errorCheck)
                errorCheck='failed to determine language'
                qwietAIStatus -= buildKPIs.verifyBuild(qwietAILog, errorCheck)
                errorCheck='Incorrect Usage:'
                qwietAIStatus -= buildKPIs.verifyBuild(qwietAILog, errorCheck)
                qwietAIStatus -= buildKPIs.verifyBuild(qwietAILog, errorString)
                uploadShiftLeftScanDetails(qwietAIJsonOutput, pathToCode, options.sourceCodeId, options.branch)
            }
            
            if (qwietAIStatus != 0) {
                throw new Exception("QwietAI Static Analsis Scan Failed, see logs")
            }
        }
        log.info("QwietAI Static Analysis Complete")
        return returnStatus
    } catch (Throwable e) {
        rethrow(e)
    }
}

/**
 * Autheticates to the QwietAI application 
 * @param bin_path [T:String] The path where the sl Binary is located
 * @param appName [T:String] The name of the App being authenticated to.
 */
private void auth(String bin_path, String appName) {
    log.info("Authenticating QwietAI static analysis")
    String ORG = config('SHIFTLEFT_ORG_ID')
    withCredentials([string(credentialsId: 'shiftleft_token', variable: 'shiftleft')]) {
        String authenticateQwietAI = bin_path + ' auth --org ' + ORG +' --token '+ shiftleft +' --no-diagnostic'
        int authStatus = utils.silentCmdWithStatus(authenticateQwietAI)
        if (authStatus != 0) {
            throw new Exception("QwietAI Authentication Failed, see logs")
        }
    }
}

/**
 * Downloads QwietAI files from the Fileshare and copies it to the correctlocation
 * The latest SL executable is made available in orbit via the following job : https://jenkins.orbit.corp.entsec.com/product/orbit_pipelines/job/Orbit_Pipelines/job/pipeline-common/job/qwietai-upload-ci/
 * In case of outdated SL executables, please check if tha above job is functioning properly. 
 * @return The location/path of the binary executable
 */
private String install() {
    log.info('Installing Qwiet AI')
    if (isUnix()) {
        if(fileExists('/usr/local/bin/sl')) {
            log.info("QwietAI already installed!")
            return '/usr/local/bin/sl'
        }
        
        utils.silentCmdWithStatus('curl https://cdn.shiftleft.io/download/sl >/tmp/sl && chmod a+rx /tmp/sl && sudo mv /tmp/sl /usr/local/bin/sl')
        return '/usr/local/bin/sl'
    }
    else {
        if(fileExists('C:\\Tools\\QwietAI\\sl.exe')) {
            log.info("QwietAI already installed!")
            return 'C:\\Tools\\QwietAI\\sl.exe'
        }        
        utils.silentCmdWithStatus('mkdir -p C:\\Tools\\qwietai')
        utils.silentCmdWithStatus('curl -o C:\\Tools\\sl-latest-windows-x64.zip https://files.orbit.corp.entsec.com/qwietai/windows/latest/sl-latest-windows-x64.zip')
        ziputils.unzipFile(
            fileName: "C:\\Tools\\sl-latest-windows-x64.zip",
            targetDir: "C:\\Tools\\qwietai",
        )
        return 'C:\\Tools\\qwietai\\sl.exe'
    }
}

/**
 * Generates QwietAI scan details from the the scan log created
 * And uploads them to Radar.
 * <p><b>Example Code Usage:</b></p>
 * <pre>
 * qwietAI.uploadShiftLeftScanDetails("qwietAIscan.json")
 * </pre>
 * @param qwietAIJsonLog [T:String] The log file which was generated by shiftLeft scan
 * @param location [T:String] The path being scanned
 * @param sourceCodeId [T:Integer] [OPTIONAL] The source Code ID returned from the BOM Response [DEFAULT:null]
 * @param branch [T:String][OPTIONAL] The branch of the repository for which the scan is performed [DEFAULT:null]
 * @return The scan link generated
 */
private def uploadShiftLeftScanDetails(String qwietAIJsonLog, String location, Integer sourceCodeId = null, String branch = null) {
    def data = readJSON file : qwietAIJsonLog
    String timestamp = data.timestamp
    String scanId = -1
    String appId = ""
    if (data.compound_name && data.polyglot_scan_id) {
        appId = data.compound_name
        scanId = data.polyglot_scan_id
    } else if (!data.scans || !(data.scans instanceof List) || data.scans.isEmpty()) {
        log.debug("Single Scan Details not found. Printing QwietAI JSON log file...")
        utils.displayFile(qwietAIJsonLog)
        throw new Exception("QwietAI Scan Details not found in JSON log- ${qwietAIJsonLog}, see output logs and/or consult QwietAI support.")
    } else {
        scanId = scans[0].id
        appId = scans[0].app_id
    }
    log.debug("Scan ID : " + scanId)
    log.debug("App ID : " + appId)
    String createdLink = "https://app.shiftleft.io/apps/${appId}/summary?scan=${scanId}"
    log.info("The Shift Left Scan dashboard Link : " + createdLink)
    radar.uploadShiftLeftScanDetails(timestamp, appId, scanId, location, sourceCodeId, branch)
}