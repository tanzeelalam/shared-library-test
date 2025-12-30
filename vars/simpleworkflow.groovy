/**
 * Simple Orbit build workflow.
 *
 * <p>The build KPIs can be executed:</p>
 * <ol>
 *     <li>Standard Build</li>
 *     <li>Bullseye Build</li>
 * </ol>
 *
 * <p>The return value from simpleworkflow needs to be verified.</p>
 *
 * <p>Check the <code>buildKPIs()</code> method for further details.</p>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * int buildStatus = simpleworkflow(
 *    buildCommand: "npm run create-prod-build",
 * )
 * </pre>
 * <pre>
 * int buildStatus = simpleworkflow(
 *    buildCommand: "npm run create-prod-build",
 *    enableBullseye: true,
 *    testFileToRun: "tests\\unit_tests.exe",
 *    exclusionsFile: "exclusions.txt",
 * )
 * </pre>
 * @param preBuildClosure [T:Closure] [OPTIONAL] A closure containing the logic needed to setup a compile
 *          environment for parallel activity e.g., NFS Drive mapping.
 * @param buildCommand [T:String] A standard call to a build script or command,
 *          e.g., Ant, MSBuild, NPM, build.bat (etc)
 * @param buildDir [T:String] [OPTIONAL] User specified directory to run the build command.
 *          [DEFAULT:env.WORKSPACE]
 * @param buildLog [T:String] [OPTIONAL] User specified logfile for the build command
 * @param buildTarget [T:String] [OPTIONAL] User specified target for compilation purposes.
 *          [DEFAULT:"release"]
 * @param enableBullseye [T:boolean] [OPTIONAL] Flag to indicate whether or not to run bullseye
 *      [DEFAULT:false]
 * @param bullseyeCovFile [T:String] [OPTIONAL] Location & name of Bullseye cov file to generate.
 *          [DEFAULT:env.WORKSPACE] + "\bullseye.cov"
 * @param filesToBackup [T:String] [OPTIONAL] Comma separated list of files to keep from bullseye
 *          build. These are required by the development team to execute bullseye. If not provided,
 *          there is no reason to perform a bullseye build
 * @param testFileToRun [T:String] The test file to execute once the Project is built to get the Bullseye Coverage. This is
 *          required as without executing it, the test coverage results will be 0%. This path should be relative to the root of the repository/build path.
 * @param exclusionsFile [T:String] [OPTIONAL] The exclusions file for Bullseye. This is optional and is used to exclude regions from
 *          the coverage report if any by importing the file into bullseye. This path should be relative to the root of the repository/build path.
 * @param errorString [T:String] [OPTIONAL] User specified error string to check the logfile for.
 *          This can be a string, string containing a regex, etc.
 *          If provided, buildStatus will be set if the buildLog contains the error string
 * <header>QwietAI options:</header>
 * @param qwietAIPathToCode [T:String] path to the source code to scan
 * @param qwietAILanguage [T:String][OPTIONAL] The source code language to scan (java,javasrc, c, csharp, go, python, js,multi(language), default is NOT Specified
 * @param qwietAIAppName [T:String][OPTIONAL] app Name for Qwiet AI project, defaults to Component.
 * @param qwietAIAppGroup [T:String][OPTIONAL] app Group to organise Qwiet AI projects, defaults to Product.
 * @param qwietAIBranch [T:String] [OPTIONAL] SCM Branch for Static Analysis code tool, defaulting to "Default"
 * @param qwietAICustomOptions [T:String] [OPTIONAL] Static Analysis custom options
 *      in the form of <code>"--exclude <path-1>,<path-2>,..."</code>
 *      https://docs.shiftleft.io/cli/reference/analyze
 * @param qwietAIErrorString [T:String] [OPTIONAL] User specified error string to check the Static Analysis logfile for.
 *      This can be a string, string containing a regex, etc.
 *      If provided, buildStatus will be set if the buildLog contains the error string
 *      e.g. <code>"My Error String"</code>
 * @param qwietAITimeout [T:String] [OPTIONAL] Custom timeout value for analysis-timeout and cpg-generation-timeout
 *      e.g. <code>"240m0s"</code>
 * @return [T:int] <code>0</code> if build succeeds.
 */
def call(Map arg, Closure preBuildClosure = null) {
    log.debug(arg)

    assert !utils.isNullOrEmptyString(arg.buildCommand): "ERROR: arg.buildCommand not passed, " +
            "this is mandatory for simpleworkflow."
    assert !utils.isNullOrEmptyString(utils.getNodeVar('REUSE_LABEL')): "ERROR: REUSE_LABEL not set, " +
            "please ensure you are using node.groovy"


    // This is where you setup extra params to be passed to buildKPIs()
    // e.g.
    //    arg.put("preCommand", precommand)
    // Pass through all args to buildKPIs.
    // This will allow us to add paramaters to buildKPIs without affecting the calling method.
    // This is essentially a wrapper method to allow us to call buildKPI's in multiple fashions.
    int returnStatus = buildKPIs(arg, preBuildClosure)
    return returnStatus
}
