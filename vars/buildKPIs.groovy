import com.mcafee.orbit.Utils.StringUtils

/**
 * Build KPIs (Standard build, Bullseye, static analysis).
 *
 * <ul>
 *     <li>Standard build command will be executed on the current build node.</li>
 *     <li>Launches a separate build which under goes code coverage analysis using Bullseye.</li>
 *     <li>For Bullseye, the settings required, are filesToBackup and testFileToRun. Without these bullseye will not run.</li>
 * </ul>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * buildKPIs(buildCommand: "npm run create-prod-build")
 * </pre>
 * <pre>
 * buildKPIs(
 *    buildCommand: "npm run create-prod-build",
 *    enableQwietAI: true,
 *    enableBullseye: false,
 * )
 * </pre>
 * <pre>
 * int buildStatus = buildKPIs(
 *    preCommand: "vsvars32.bat",
 *    buildCommand: "npm run create-prod-build",
 *    buildDir: env.WORKSPACE+"\\"+buildRoot,
 *    buildLog: stdLogFile.log,
 *    buildTarget: "release32",
 *    bullseyeCovFile: bullseyeCovFile,
 *    enableBullseye: true,
 *    vsBuild: true,
 *    filesToBackup: filesToBackup,
 *    testFileToRun: testFileToRun,
 * )
 * </pre>
 * <pre>
 * Closure preBuildClosure = {
 *     return {
 *         log.info("Testing from inside of preBuildClosure.")
 *         sh 'env'
 *     }
 * }
 * int buildStatus = buildKPIs(
 *    [
 *        buildCommand: "npm run create-prod-build",
 *        enableBullseye: true,
 *    ],
 *    preBuildClosure
 * )
 * </pre>
 *
 * @param buildCommand [T:String] A standard call to a build script or command,
 *          e.g., Ant, MSBuild, NPM, build.bat (etc)
 * @param preCommand [T:String] [OPTIONAL] A standard command to execute with the build script. Mainly used for VS.
 *          e.g., VS  - vsvars32.bat needs to be executed.
 * @param buildDir [T:String] [OPTIONAL] User specified directory to run the build command.
 *          [DEFAULT:env.WORKSPACE]
 * @param projName [T:String] [OPTIONAL] User specified projName.
 *          Defaults to the component name
 * @param buildLog [T:String] [OPTIONAL] User specified logfile for the build command.
 *          Defaults to projName + ".log".
 * @param errorString [T:String] [OPTIONAL] User specified error string to check the logfile for.
 *          This can be a string, string containing a regex, etc.
 *          If provided, buildStatus will be set if the buildLog contains the error string
 * @param buildTarget [T:String] [OPTIONAL] User specified target for logging and archiving purposes.
 *          Defaults to "release".
 * <header>Control params:</header>
 * @param enableQwietAI [T:boolean] [OPTIONAL] Flag to indicate whether or not to run QwietAI Static Anlysis.
 *                                  [DEFAULT:false]
 * @param enableBullseye [T:boolean] [OPTIONAL] Flag to indicate whether or not to run bullseye. [DEFAULT:false]
 * @param vsBuild [T:boolean] [OPTIONAL] Flag to indicate if its a VS build, used for logging.
 *      [DEFAULT:false]
 * <header>Bullseye options:</header>
 * @param bullseyeCommand [T:String] [OPTIONAL] A specific call to a build script for Bullseye
 * @param bullseyeCovFile [T:String] [OPTIONAL] Location & name of Bullseye cov file to generate.
 *          [DEFAULT:env.WORKSPACE] + "\\bullseye.cov"
 * @param filesToBackup [T:String] [OPTIONAL] Comma separated list of files to keep from bullseye
 *          build. These are required by the development team to execute bullseye. If not provided,
 *          there is no reason to perform a bullseye build
 * @param testFileToRun [T:String|List&lt;String&gt;] [OPTIONAL] The test file to execute once the Project is built to get the Bullseye Coverage. This is
 *          required as without executing it, the test coverage results will be 0%. This path should be relative to the root of the repository/build path.
 * @param exclusionsFile [T:String] [OPTIONAL] The exclusions file for Bullseye. This is optional and is used to exclude regions from
 *          the coverage report if any by importing the file into bullseye. This path should be relative to the root of the repository/build path.
 * @param coverageType [T:String][OPTIONAL] The Coverage type under which results will be posted to Radar. Defaults to 'Bullseye'. 
 * When set, it changes the CoverageType under which CodeCoverage results are posted in Radar. It is recommended to set this value 
 * when doing more than one bullseye run in a single build.
 * @param preBuildClosure [T:Closure] [OPTIONAL] A closure containing the logic needed to setup a compile
 *          environment for parallel activity e.g., NFS Drive mapping.
 * <header>QwietAI options:</header>
 * @param qwietAIPathToCode [T:String][OPTIONAL] path to the source code to scan. Defaults to the current working directory.
 * @param qwietAILanguage [T:String][OPTIONAL] The source code language to scan (java,javasrc, c, csharp, go, python, js,multi(language), default is NOT Specified
 * @param qwietAIAppName [T:String][OPTIONAL] app Name for Qwiet AI project, defaults to Component.
 * @param qwietAIAppGroup [T:String][OPTIONAL] app Group to organise Qwiet AI projects, defaults to Product.
 * @param sourceCodeId [T:String][OPTIONAL] The SourceCodeId returned from the git step when <code>returnBom</code> is <code>true</code>. Required when reporting
 *      QwietAI scans to Radar
 * @param qwietAIBranch [T:String] [OPTIONAL] SCM Branch for Static Analysis code tool, defaulting to "Default"
 * @param qwietAICustomOptions [T:String] [OPTIONAL] Static Analysis custom options
 *      in the form of <code>"--team <teamname>..."</code>
 *      https://docs.shiftleft.io/cli/reference/analyze
 * @param qwietAIAdditionalFlags [T:String] [OPTIONAL] Static Analysis language specific additional flags passed after --
 *      in the form of <code>"--exclude <path-1>,<path-2>,..."</code>
 *      https://docs.shiftleft.io/cli/reference/analyze
 * @param qwietAIErrorString [T:String] [OPTIONAL] User specified error string to check the Static Analysis logfile for.
 *      This can be a string, string containing a regex, etc.
 *      If provided, buildStatus will be set if the buildLog contains the error string
 *      e.g. <code>"My Error String"</code>
 * @param qwietAITimeout [T:String] [OPTIONAL] Custom timeout value for analysis-timeout and cpg-generation-timeout
 *      e.g. <code>"240m0s"</code>
 * @return [T:int] <code>0</code> if build succeeds.
 *        <p>
 *            Otherwise, the build will check on build status and throw an error to fail the build in the event
 *            of a compilation failure in:
 *            <ol>
 *                <li>Standard build:
 *                    <ol>
 *                        <li>- preclosure failure</li>
 *                        <li>- compile failure</li>
 *                        <li>- errorString found in log file</li>
 *                   </ol>
 *               </li>
 *               <li>Bullseye build:
 *                   <ol>
 *                       <li>- preclosure failure</li>
 *                       <li>- compile failure</li>
 *                       <li>- errorString found in log file</li>
 *                   </ol>
 *               </li>
 *           </ol>
 *       </p>
 */
def call(Map arg, Closure preBuildClosure = null) {
    global()
    utils.mapReporter(arg)

    assert !utils.isNullOrEmptyString(arg.buildCommand): "ERROR: arg.buildCommand not passed, " +
            "this is mandatory for buildKPIs."
    assert !utils.isNullOrEmptyString(utils.getNodeVar('REUSE_LABEL')): "ERROR: REUSE_LABEL not set, " +
            "please ensure you are using node.groovy"

    utils.defineBuildParameters()
    String projName = arg.projName ?: env.BUILD_PARAM_COMPONENT

    int stdBuildStatus = 0
    int returnStatus = 0
    String buildCommand = arg.buildCommand
    String buildDir = arg.buildDir ?: env.WORKSPACE
    String buildLog = arg.buildLog ?: projName + ".log"
    String buildTarget = arg.buildTarget ?: "release"
    String preCommand = arg.preCommand ?: ""
    String errorString = arg.errorString ?: ""

    // Indicators for running KPIs
    Boolean runQwietAI = arg.enableQwietAI?.toBoolean() ? true : false
    Boolean runBullseye = arg.enableBullseye?.toBoolean() ? true : false
    Boolean vsBuild = arg.vsBuild?.toBoolean() ?: false

    // Bullseye parameters
    String bullseyeLogFile = env.WORKSPACE + "\\bullseye_" + buildLog
    String bullseyeCommand = arg.bullseyeCommand ?: arg.buildCommand
    String bullseyeCovFile = arg.bullseyeCovFile ?: env.WORKSPACE + "\\bullseye.cov"
    String filesToBackup = arg.filesToBackup ?: null
    def bullseyeRunTest = arg.testFileToRun ?: null
    String bullseyeExclusions = arg.exclusionsFile ?: null
    String coverageType = arg.coverageType ?: 'BuildKPIs_Bullseye'

    // Check global Orbit Variables and jenkins environment variables
    // to determine whether to run KPI's or not.
    runQwietAI =  config('DISABLEQWIETAI')?.toBoolean() ? false : runQwietAI
    runBullseye = config('DISABLEBULLSEYE')?.toBoolean() ? false : runBullseye

    buildLog = env.WORKSPACE + "\\" + buildLog
    if (isUnix()) {
        bullseyeCovFile = arg.bullseyeCovFile ?: env.WORKSPACE + "/bullseye.cov"
    }
    buildLog = utils.formatPathForOS(buildLog)
    bullseyeLogFile = utils.formatPathForOS(bullseyeLogFile)
    bullseyeCovFile = utils.formatPathForOS(bullseyeCovFile)

    if (vsBuild) {
        buildCommand = buildCommand + ' /Out \"' + buildLog + '\"'
        bullseyeCommand = bullseyeCommand + ' /Out \"' + bullseyeLogFile + '\"'
        log.debug("Visual Studio build. Log file generated by /out instead of redirect")
    } else {
        buildCommand = buildCommand + " > " + buildLog + " 2>&1"
        bullseyeCommand = bullseyeCommand + " > " + bullseyeLogFile + " 2>&1"
    }
    String stashNumber = "${buildTarget}_" + Math.abs(new Random().nextInt() % 1000) + 1
    String stashName = projName + "_" + stashNumber + "_buildKPIsStash"
    String zipName = stashName + ".zip"

    // If standard build is all we are doing, we don't need to create
    // a stash, as it will not be used anywhere.
    if (runBullseye) {
        dir(env.WORKSPACE) {
            ziputils.createZip([
                    fileName: zipName,
                    input   : "."
            ])
            stash includes: zipName, name: stashName
            utils.deleteFiles([zipName])
        }
    }
    log.debug("All params setup for KPI's")
    log.debug("buildCommand : " + buildCommand)
    log.debug("preCommand : " + preCommand)
    log.debug("buildLog : " + buildLog)
    log.debug("buildDir : " + buildDir)
    log.debug("buildTarget : " + buildTarget)
    log.debug("projName : " + projName)
    log.debug("bullseyeCommand : " + bullseyeCommand)
    log.debug("bullseyeRunTest : " + bullseyeRunTest)
    log.debug("bullseyeExclusions : " + bullseyeExclusions)
    log.debug("bullseyeLogFile : " + bullseyeLogFile)
    log.debug("filesToBackup : " + filesToBackup)
    log.debug("coverageType : " + coverageType)

    parallel('buildKPIs Standard Build': {
        dir(buildDir) {
            try {
                executepreBuild(preBuildClosure)
                if (!StringUtils.isNullOrEmpty(preCommand)) {
                    execute preCommand
                }
                stdBuildStatus = utils.silentCmdWithStatus(buildCommand)
                if (stdBuildStatus == 0) {
                    stdBuildStatus = this.verifyBuild(buildLog, errorString)
                }
                if (stdBuildStatus != 0){
                    radar.addBuildLog(
                        "Error",
                        "buildKPIs_Standard",
                        env.current_stage,
                        "buildKPIs_Standard",
                        "Standard Build Failed, see logs",
                    )
                    throw new Exception("Standard Build Failed, see logs")
                }
            }
            catch (Throwable e) {
                radar.addBuildLog(
                    "Error",
                    "buildKPIs_Standard",
                    env.current_stage,
                    "buildKPIs_Standard",
                    e.message,
                )
                log.error("Standard build failed")
                rethrow(e)
            }
            finally {
                if (!utils.isNullOrEmptyString(buildLog)) {
                    log.debug("archiving logfile " + buildLog)
                    artifacts.upload([files: buildLog])
                } else {
                    log.debug("No logfile to archive")
                }
            }
        }
    }, 'buildKPIs Bullseye Build': {
        if (runBullseye) {
            node(utils.getNodeVar('REUSE_LABEL')) {
                setBuildNode(zipName, stashName)
                try {
                    executepreBuild(preBuildClosure)
                    dir(buildDir) {
                        if (!StringUtils.isNullOrEmpty(preCommand)) {
                            execute preCommand
                        }
                        bullseye([
                                sourceDir       : buildDir,
                                covFile         : bullseyeCovFile,
                                buildCmd        : bullseyeCommand,
                                testFileToRun   : bullseyeRunTest,
                                exclusionsFile  : bullseyeExclusions,
                                filesToBackup   : filesToBackup,
                                projName        : projName + "_" + buildTarget,
                                logfile         : bullseyeLogFile,
                                coverageType    : coverageType
                        ])
                        this.verifyBuild(bullseyeLogFile, errorString)
                    }
                }
                catch (Throwable e) {
                    radar.addBuildLog(
                        "Error",
                        "buildKPIs_Bullseye",
                        env.current_stage,
                        "buildKPIs_Bullseye",
                        e.message,
                    )
                    log.error("Bullseye build failed.")
                    rethrow(e)
                }
            }
        } else {
            log.info("Running buildKPIs without Bullseye.")
        }
    }, failFast: true
    )
    if (runQwietAI) {
        String qwietAIPathToCode = arg.qwietAIPathToCode ?: buildDir
        String qwietAITimeout = arg.qwietAITimeout ?: config("QWIETAI_TIMEOUT")
        String qwietAIBranch = arg.qwietAIBranch.replaceAll('/$', '')
        returnStatus= qwietAI([language: arg.qwietAILanguage,
                               pathToCode: qwietAIPathToCode,
                               customOptions: arg.qwietAICustomOptions,
                               additionalFlags: arg.qwietAIAdditionalFlags,
                               branch: qwietAIBranch,
                               appName: arg.qwietAIAppName,
                               appGroup: arg.qwietAIAppGroup,
                               sourceCodeId: arg.sourceCodeId,
                               timeout: qwietAITimeout,
                               errorString: arg.qwietAIErrorString
        ])
    }
    return returnStatus
}

/**
 * helper method to execute prebuild closure for each build node.
 * @param preBuildClosure
 */
private void executepreBuild(Closure preBuildClosure) {
    try {
        preBuildClosure?.call()
    }
    catch (Throwable e) {
        log.error("Execution of the preBuild closure failed")
        rethrow(e)
    }
}

/**
 * helper method to unstash for each build node.
 * This is used for each buildnode for each seperate build KPI
 * @param String zipName - name of zipfile to unzip
 * @param String stashName - name of stash to unstash
 */
private void setBuildNode(String zipName, String stashName) {
    unstash stashName
    ziputils.unzipFile([
            fileName : zipName,
            targetDir: env.WORKSPACE
    ])
    utils.deleteFiles([zipName])
}

/**
 * <p><b>verifyBuild</b></p>
 * <p>This is a helper method to allow us to verify if a build was successful.
 * It will execute egrep on the build output file (fileName), for the string, errorString,
 * and return success if it doesn't find a match.</p>
 * <code>
 *     String compileErrorRegex = "[1-9][0-9]* failed"
 *     String myFailString = "Build failed"
 *     int result = verifyBuild(fileName,compileErrorRegex)
 *     int result2 = verifyBuild(fileName,myFailString)
 * </code>
 * @param String fileName - this is the file to test
 * @param String error[T:String] this is the string to grep for. This can be a string or a regex.
 * @return int - success or fail.
 */
private int verifyBuild(String fileName, String errorString) {
    int returnStatus = 0
    if (utils.isNullOrEmptyString(errorString)) {
        log.info("No errorString provided, skipping verification")
        return returnStatus
    }
    if (utils.isNullOrEmptyString(fileName)) {
        log.info("Logfile not setup, skipping verification")
        return returnStatus
    }
    log.info("INFO: Parsing the logfile: \"${fileName}\" for the error string: \"${errorString}\"")
    String verifyCmd = "E:\\tools\\GNU\\egrep -i -q "
    if (isUnix()) {
        verifyCmd = "egrep -i -q "
    }
    verifyCmd = verifyCmd + "\"" + errorString + "\" \"" + fileName + "\""
    log.warn("Checking build log :::: " + verifyCmd)

    //egrep returns 0 if provided string is found and 1 if provided string is not found
    returnStatus = utils.silentCmdWithStatus(verifyCmd)
    if (returnStatus == 0) {
        log.error("Build failed, [" + errorString + "] found in logfile : " + fileName + " Failing the build...")
    } else {
        log.info("Build Ok, [" + errorString + "] not found in logfile : " + fileName)
    }

    return 1 - returnStatus
}
