/**
 * <h4>Sample usage:</h4>
 * <pre>
 * maven.mavenMain(
 *    option: "clean install",
 *    mavenDir: "/home/orbit/.m2",
 *    mavenHomePath: "/usr/local/apache-maven-3.5.4",
 *    parameter: "Unit Tests",
 * )
 * </pre>
 * @param option [T:String] (default: 'clean install').
 * @param mavenDir [T:String] (Unix default: "/home/orbit/.m2" , Windows default: "mvn").
 * @param mavenHomePath [T:String] (Unix default: /usr/local/apache-maven-3.5.4, Windows: blank).
 * @param parameter [T:String] [OPTIONAL] Parameter to append to the command.
 */
private void mavenMain(arg) {
    env.current_task = 'mvn'

    //Validation
    log.debug("Maven:mavenMain: Starting...")

    def mvn
    def returnStatus
    try {
        if (isUnix()) {
            def mvnHome =  utils.isNullOrEmptyString(arg.mavenHomePath) ? '/usr/local/apache-maven-3.5.4' : arg.mavenHomePath
            def mvnRepo = utils.isNullOrEmptyString(arg.mavenDir) ? '/home/orbit/.m2' : arg.mavenDir
            def mvnRepoArg = "-Dmaven.repo.local=\"${mvnRepo}\""
            def unixMavenPath = "/bin/mvn ${mvnRepoArg}"
            mvn = mvnHome + unixMavenPath
        } else {
            mvn = utils.isNullOrEmptyString(arg.mavenHomePath) ? 'mvn' : arg.mavenHomePath
        }

        log.debug("MAVEN:mavenMain() : Maven Version ")
        returnStatus = utils.silentCmdWithStatus(mvn + " --version")
        assert returnStatus == 0: "Error mavenMain()"

    } catch (Throwable e) {
        log.error("Unexpected response from maven")
        rethrow(e)
    }

    //Clean install maven default

    if (!utils.isNullOrEmptyString(arg.option) && !arg.option.contains('clean install')) {
        returnStatus = utils.silentCmdWithStatus(mvn + " clean install")
        assert returnStatus == 0: "Error Maven Clean Install"

    }

    this.mavenOption(arg.option, mvn, arg.parameter)

}

private void mavenOption(String option, String mvn, String parameter) {
    env.current_task = 'mvn'

    log.debug("MAVEN:mavenOption() :  ENTERING MAVENOPTION")
    log.debug("MAVEN:mavenOption() :  OPTION INPUT : " + option)
    log.debug("MAVEN:mavenOption() : PARAMETER INPUT : " + parameter)

    def optionCommand = mvn + ' ' + option + parameter

    def returnStatus = utils.silentCmdWithStatus(optionCommand)
    assert returnStatus == 0: "Error Maven Clean Install"

    log.debug("MAVEN:mavenOption() :  EXITING MAVENOPTION")
}
