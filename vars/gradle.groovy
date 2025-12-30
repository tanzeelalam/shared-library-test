import com.mcafee.orbit.Credentials.CredentialStore

/**
 * Run Gradle Builds in Orbit
 *
 * <p>
 *     This method supports Gradle Build using Gradle Wrappers.
 *     If wrapper path is not provided, default will be used.
 * </p>
 *
 * <p>
 * Only the Trellix Artifactory Enterprise Repository is allowed,
 * If any other repository is used in your gradle scripts then it will be removed and
 * replaced with the Enterprise Repository URL.
 * </p>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * gradle(
 *    wrapperPath: "gradle/wrapper/gradle-wrapper.properties",
 *    buildTask: "hello showRepositories",
 *    option: "exclude-task hello",
 * )
 * </pre>
 * <pre>
 * gradle(
 *    wrapperPath: "gradle/wrapper/gradle-wrapper.properties",
 *    buildTask: "hello showRepositories",
 *    option: "exclude-task hello --info",
 * )
 * </pre>
 * @param buildTask [T:String] At least one build task should be provided otherwise build will fail.
 * @param wrapperPath [T:String] [OPTIONAL] Provide the path to wrapper.
 * @param option [T:String] [OPTIONAL] Command line options (multiple) to be run with the build.
 *   exclude-task 'taskname' excludes the mentioned task from being executed. eg., --option1 param1 --option2
 */
def call (Map arg){
    assert !utils.isNullOrEmptyString(arg.buildTask): utils.getErrorMessage("Build Task is required")
    this.gradle(arg)
}

private void gradle(Map arg) {
    global()
    env.current_task = 'gradle'
    String buildLog = "GradleBuild.log"
    String errorString = " FAILURE"
    //WrapperBin : Parameter to hold shell script or batch script necessary for the execution of the build with wrapper
    //gradleBinPath: Path to gradle binary.
    String wrapperBin = "gradlew.bat"
    String gradleBinPath = global.path.WIN_GRADLE_PATH
    String initScript = utils.formatPathForOS(env.WORKSPACE + '\\initScript.gradle')
    if(isUnix()) {
        wrapperBin = "./gradlew"
        gradleBinPath = global.path.UNIX_GRADLE_PATH
    }
    String gradleTask = arg.buildTask
    String option = !utils.isNullOrEmptyString(arg.option) ? arg.option : ""
    Closure preBuildClosure = {
        //Closure body
        String initFileLocation = libraryResource 'org/mcafee/gradle/init.gradle'
        String initScriptString = initFileLocation.replaceFirst('ArtifactoryURL', config('ENTERPRISE_REPOSITORY_URL'))
        writeFile(file: initScript, text: initScriptString)
        try{
            String gradleWrapper = !utils.isNullOrEmptyString(arg.wrapperPath) ? arg.wrapperPath : ""
            if(!utils.isNullOrEmptyString(gradleWrapper)) {
                def fileAttr = jenkins.getFileAsObject(fileName :"${gradleWrapper}")
                gradleWrapper = fileAttr.ABSOLUTE_PATH
            }

            if (!utils.isNullOrEmptyString(gradleWrapper) && fileExists("${gradleWrapper}")) {
                log.info("Wrapper found at ${gradleWrapper}")
            } else {
                log.info("Wrapper not found, applying default wrapper")
                String url = config('GRADLE_DISTRIBUTION_URL')
                //gradleWrapperCmd will create wrapper which generates gradlew,gradlew.bat
                // gradlew and gradlew.bat are the shell script and windows batch script respectively to execute the build with the wrapper
                String gradleWrapperCmd = "${gradleBinPath} wrapper --gradle-distribution-url=${url}"
                log.info("gradleWrapperCmd: ${gradleWrapperCmd}")
                int returnCode = utils.silentCmdWithStatus(gradleWrapperCmd)
                if(returnCode!=0) {
                    log.error("Failed to run gradle wrapper: ${gradleWrapperCmd}")
                    throw new Exception("Build Failed, see logs")
                }
            }
        } catch (Throwable e) {
            rethrow(e)
        }
        if(isUnix()) {
            sh "chmod +x ${wrapperBin}"
        }
    }
    withCredentials([
        usernamePassword(
            credentialsId: 'artifactory_trellix_cred',
            usernameVariable: 'USERNAME',
            passwordVariable: 'PASSWORD'
        )
    ]) {
        /* -D defines system properties which can be alternatively defined in gradle.properties
            -P defines commandline parameter
            Here proxy is added in gradleCmd so that it can download plugins
        */
        String gradleCmd = "${wrapperBin} -Dgradle.wrapperUser=${USERNAME} -Dgradle.wrapperPassword=${PASSWORD} " +
                "-Dhttps.proxyHost=${global.HTTPS_PROXY} -Dhttps.proxyPort=${global.HTTPS_PROXY_PORT} " +
                "-Part_dep_username=${USERNAME} -Part_dep_password=${PASSWORD} --init-script ${initScript} " +
                "${gradleTask} ${option}"
        log.info("gradleCmd: ${gradleCmd}")
        buildKPIs(
            [
                buildCommand: gradleCmd,
                enableBullseye: false,
                buildDir: pwd(),
                errorString: errorString,
                buildLog: buildLog,
            ],
            preBuildClosure()
        )
    }
}
