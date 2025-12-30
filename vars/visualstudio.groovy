import java.text.SimpleDateFormat

/**
 * Visual Studio compilation.
 *
 * <p>Consolidates all of the Visual Studio <code>devenv</code> calls into a single method.</p>
 *
 * <p>Check <code>buildKPIs()</code> for further details.</p>
 *
 * <h4>Sample usage: </h4>
 * <pre>
 * int buildStatus = visualStudio(
 *    version: "14",
 *    solutionFile: buildRoot + "\\project.sln",
 *    targets: "RELEASE|Win32",
 *    logName: stdLogFile,
 *    enableBullseye: false,
 *    standardBuild: true,
 * )
 * </pre>

 * <pre>
 * int buildStatus = visualStudio(
 *    version: "14",
 *    solutionFile: buildRoot + "\\project.sln",
 *    targets: "RELEASE|Win32",
 *    logName: stdLogFile,
 *    enableBullseye: true,
 *    testFileToRun: "tests\\unit_tests.exe",
 *    exclusionsFile: "exclusions.txt",
 * )
 * </pre>
 *
 * @param version [T:String] VS version to use
 *                         e.g. 2017, 15, 15.0.0 - will all refer to VS 15 Enterprise
 *                         e.g. 2008, 9, 9.0 - will all refer to VS 9 Professional
 * @param solutionFile [T:String] VS solution file location
 * @param targets [T:String] targets to build within the VS project
 * @param project [T:String] [OPTIONAL] VS project to build
 * @param projectconfig [T:String] [OPTIONAL] VS projectConfig settings
 * @param logName [T:String] [OPTIONAL] User specified logfile for the build command.
 *          [DEFAULT:projName + ".log"]
 * @param preBuildClosure [T:Closure] [OPTIONAL] A closure containing the logic needed to set up a compile
 *          environment for parallel activity e.g., NFS Drive mapping.
 * @param enableBullseye [T:boolean] [OPTIONAL] flag to indicate whether or not to run bullseye.
 *      [DEFAULT:false]
* @param filesToBackup [T:String] [OPTIONAL] - [Bullseye option] Comma separated list of files to keep from bullseye build.
 *          These are required by the development team to execute bullseye. If not provided, there
 *          is no reason to perform a bullseye build
 * @param bullseyeCovFile [T:String] [OPTIONAL] - [Bullseye option] Location & name of Bullseye cov file to generate.
 *          [DEFAULT:env.WORKSPACE + "\\bullseye.cov"]
 * @param testFileToRun [T:String|List&lt;String&gt;] [Bullseye option] The test file to execute once the Project is built to get the Bullseye Coverage. This is
 *          required as without executing it, the test coverage results will be 0%. This path should be relative to the root of the repository/build path.
 * @param exclusionsFile [T:String] [OPTIONAL] - [Bullseye option] The exclusions file for Bullseye. This is optional and is used to exclude regions from
 *          the coverage report if any by importing the file into bullseye. This path should be relative to the root of the repository/build path.
 * @param standardBuild [T:boolean] [OPTIONAL] Default VS build task is set to "rebuild". Set this to true to set the VS build task to "build".
 * @param vsEdition [T:String] [OPTIONAL] Defaults to <code>"Professional"</code> (backward compatibility). Only relevant for later versions of VS.
 *                               Used to specify Enterprise/Professional from 2017 onwards.
 * @param x64 [T:boolean] [OPTIONAL] We will default to 32 bit, with vcvars32.bat.
 *                                Used to specify 64 bit toolset from 2017 onwards.
 *                               https://docs.microsoft.com/en-us/cpp/build/building-on-the-command-line?view=msvc-170
 * @param errorString [T:String] [OPTIONAL] User specified error string to check the logfile for.
 *          This can be a string, string containing a regex, etc.
 *          If provided, buildStatus will be set if the buildLog contains the error string
 * <header>QwietAI options:</header>
 * @param qwietAIPathToCode [T:String][OPTIONAL] path to the source code to scan. Defaults to the current working directory.
 * @param qwietAILanguage [T:String][OPTIONAL] The source code language to scan (java,javasrc, c, csharp, go, python, js,multi(language), default is NOT Specified
 * @param qwietAIAppName [T:String][OPTIONAL] app Name for Qwiet AI project, defaults to Component.
 * @param qwietAIAppGroup [T:String][OPTIONAL] app Group to organise Qwiet AI projects, defaults to Product.
  * @param sourceCodeId [T:String][OPTIONAL] The SourceCodeId returned from the git step when <code>returnBom</code> is <code>true</code>. Required when reporting
 *      QwietAI scan to Radar
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
 */
def call(Map arg, Closure preBuildClosure = null) {
    utils.defineBuildParameters()
    //args: version, solutionFile, targets["RELEASE", "Release_wchar_non_native"], platforms["Win32", "x64"], buildType("standard", "bullseye")
    assert !utils.isNullOrEmptyString(arg.version): "ERROR: version arguement is required."
    int buildStatus = this.visualStudio(arg, preBuildClosure)
    return buildStatus
}

/*
Product Version table:
https://en.wikipedia.org/wiki/Microsoft_Visual_Studio#History

Various devenv (Use the dropdown "Other Versions")
https://msdn.microsoft.com/en-us/library/s2h6xst1(v=vs.71).aspx

Product Name             Version
Visual Studio 2008       9.0    devenv SolutionName /rebuild SolnConfigName [/project ProjName] [/projectconfig ProjConfigName]
Visual Studio 2010       10.0   devenv SolutionName /rebuild SolnConfigName [/project ProjName] [/projectconfig ProjConfigName]
Visual Studio 2012       11.0   devenv SolutionName /rebuild SolnConfigName [/project ProjName] [/projectconfig ProjConfigName]
Visual Studio 2013       12.0   devenv SolutionName /rebuild SolnConfigName [/project ProjName] [/projectconfig ProjConfigName]
Visual Studio 2015       14.0   devenv SolutionName /rebuild SolnConfigName [/project ProjName] [/projectconfig ProjConfigName]
Visual Studio 2017       15.0   devenv SolutionName /rebuild SolnConfigName [/project ProjName] [/projectconfig ProjConfigName]
Visual Studio 2019       16.0   devenv SolutionName /rebuild SolnConfigName [/project ProjName] [/projectconfig ProjConfigName]
Visual Studio 2022       17.0   devenv SolutionName /rebuild SolnConfigName [/project ProjName] [/projectconfig ProjConfigName]

*/

private visualStudio(Map arg, Closure preBuildClosure = null) {
    global()
    env.current_task = 'Visual Studio'

    if (isUnix()) {
        log.error(env.current_task + " not currently supported for non windows OS")
        return 1 // Return error code to identify as a failure.
    }
    //TODO: Add handling for if they only provide a string rather than a map?
    assert !utils.isNullOrEmptyString(arg.solutionFile): "ERROR: solutionFile arguement is required."
    assert fileExists(arg.solutionFile): "ERROR: cannot locate solutionFile: " + arg.solutionFile
    assert !utils.isNullOrEmptyString(arg.targets): "ERROR: Need to provide VS targets to build."
    assert !utils.isNullOrEmptyString(arg.version): "ERROR: Need to provide VS version to use"

    String buildTask = arg.standardBuild?.toBoolean() ? "/build" : "/rebuild"

    String vsExePath = ''
    String precommand = ''
    String vsEdition = arg.vsEdition ?: 'Professional'
    String vcVars = arg.x64 ? 'vcvars64.bat' : 'vcvars32.bat'

    switch (arg.version.toUpperCase()) {
        case ["9", "9.0", "2008", "VISUAL STUDIO 2008"]:
            vsExePath = global.path.WIN_VS9_DIR
            break
        case ["10", "10.0", "2010", "VISUAL STUDIO 2010"]:
            vsExePath = global.path.WIN_VS10_DIR
            break
        case ["11", "11.0", "2012", "VISUAL STUDIO 2012"]:
            vsExePath = global.path.WIN_VS12_DIR
            break
        case ["12", "12.0", "2013", "VISUAL STUDIO 2013"]:
            vsExePath = global.path.WIN_VS12_DIR
            break
        case ["14", "14.0", "2015", "VISUAL STUDIO 2015"]:
            vsExePath = global.path.WIN_VS14_DIR
            break
        case ["15", "15.0.0", "15.5.6", "2017", "VISUAL STUDIO 2017"]:
            vsExePath = global.path.WIN_VS_BASE_DIR + '\\2017\\' + vsEdition
            break
        case ["16", "16.0", "2019", "VISUAL STUDIO 2019"]:
            vsExePath = global.path.WIN_VS_BASE_DIR + '\\2019\\' + vsEdition
            break
        case ["17", "17.0", "2022", "VISUAL STUDIO 2022"]:
            vsExePath = global.path.VS_BASE_DIR + '\\2022\\' + vsEdition
            break
        default:
            error(arg.version.toUpperCase() + " is not supported at this time.")
            break
    }
    Boolean checkPath = utils.checkFilesExists(vsExePath)
    assert checkPath : "Error : vsExePath does not exist on build node :\n" + vsExePath

    precommand = vsExePath
    vsExePath += global.path.WIN_DEVENV_EXE
    String buildTargets = arg.targets
    String buildTarget = buildTargets
    def splitTarget = buildTargets.tokenize('|')
    // Seperate the string "Release|x64" to get a valid target-arch to use as the name.
    if (splitTarget.size() > 0) {
        buildTarget = splitTarget[0] + "-" + splitTarget[1] // target-architecture
    }
    switch (arg.version.toUpperCase()) {
        case ["9", "9.0", "2008", "VISUAL STUDIO 2008"]:
        case ["10", "10.0", "2010", "VISUAL STUDIO 2010"]:
        case ["11", "11.0", "2012", "VISUAL STUDIO 2012"]:
        case ["12", "12.0", "2013", "VISUAL STUDIO 2013"]:
        case ["14", "14.0", "2015", "VISUAL STUDIO 2015"]:
            precommand = '\"' + precommand + "\\Common7\\Tools\\vsvars32.bat " + '\"'
            break
        default:
            precommand = '\"' + precommand + "\\VC\\Auxiliary\\Build\\" + vcVars + '\"'
            break
    }
    String stdCommand = ' \"' + vsExePath + '\" \"' + arg.solutionFile + '\" ' + buildTask + ' \"' + arg.targets + '\"'
    if (!utils.isNullOrEmptyString(arg.project)) {
        stdCommand += ' /project \"' + arg.project + '\"'
        if (!utils.isNullOrEmptyString(arg.projectConfig)) {
            stdCommand += ' /projectconfig \"' + arg.projectConfig + '\"'
        }
    }
    // This is where you setup extra params to be passed to buildKPIs()
    // e.g.
    //    arg.put("preCommand", precommand)
    // Pass through all args to buildKPIs.
    // This will allow us to add paramaters to buildKPIs without affecting the calling method.
    // This is essentially a wrapper method to allow us to call buildKPI's in multiple fashions.

    stdCommand = precommand + ' && ' +  stdCommand

    log.debug("\nvsExePath : " + vsExePath +
        "\nvsEdition : " + vsEdition +
        "\nversion : " + arg.version +        
        "\nstdCommand : " + stdCommand +
        "\nbuildTarget : " + buildTarget
    )
    
    arg.put("buildCommand", stdCommand)
    arg.put("vsBuild", true)
    arg.put("buildTarget", buildTarget)
    arg.put("buildLog", arg.logName)

    int buildStatus = buildKPIs(arg, preBuildClosure)
    return buildStatus
}

/**
 * Updates the version information in the input file with the provided values Map.
 *
 * <p>
 *     Replaces the following strings, if they exist, with their respective current values:
 *     <ul>
 *         <li>_DATE_ is replaced by the current date, formatted as MM/DD/YYYY</li>
 *         <li>_CURRENT_YEAR_ is replaced by the current year, formatted as YYYY</li>
 *         <li>_COPYRIGHT_ is replaced by
 *              <ul>
 *                  <li>Either 'Copyright(c) CURRENT_YEAR McAfee LLC'</li>
 *                  <li>Or 'Copyright(c) startCopyrightYear-CURRENT_YEAR McAfee LLC'</li>
 *                  <li>Depending on the value of startCopyrightYear parameter. See below.</li>
 *              </ul>
 *         </li>
 *     </ul>
 * </p>
 * <h4>Sample Usage:</h4>
 * <pre>
 * visualstudio.updateVersionInfo(
 *     "version.int",
 *     [
 *         "key1": "value1",
 *         "key2": "value2",
 *         "_COPYRIGHT_": "Custom copyright string here", // You can over-ride the default Orbit value
 *     ],
 *     "version.ver",
 *     'UTF-8',
 *     2017
 * )
 * </pre>
 * @param inputFile [T:String] The path to the input file.
 * @param tokenReplacement [T:Map] A map of key-value pairs to use in replacements.
 * @param outputFile [T:String] [OPTIONAL] The output file. [DEFAULT:"version.ver"]
 * @param encoding [T:String] [OPTIONAL] The character encoding of input file.
 *                 Valid options include:
 *                 <code>"US-ASCII"</code>, <code>"ISO-8859-1"</code>, <code>"UTF-8"</code>, <code>"UTF-16BE"</code>, <code>"UTF-16LE"</code> or <code>"UTF-16"</code>.
 *                 [DEFAULT:"UTF-8"]
 * @param startCopyrightYear [T:int] [OPTIONAL] start year for the copyright notice.
 */
void updateVersionInfo(
    String inputFile,
    Map tokenReplacement,
    String outputFile = "version.ver",
    String encoding = 'UTF-8',
    int startCopyrightYear = 2017
) {
    int currentYear = Calendar.getInstance().get(Calendar.YEAR)
    tokenReplacement._CURRENT_YEAR_ = currentYear
    if (!tokenReplacement._COPYRIGHT_) {
        String copyrightYears = currentYear.toString()
        if (currentYear != startCopyrightYear) {
            copyrightYears = "$startCopyrightYear-$currentYear"
        }
        tokenReplacement._COPYRIGHT_ = "Copyright(c) $copyrightYears McAfee LLC"
    }
    if (!tokenReplacement._DATE_) {
        def formatter = new SimpleDateFormat('MM/dd/yyyy', Locale.ENGLISH)
        tokenReplacement._DATE_ = formatter.format(new Date())
    }
    utils.searchAndReplace(
        fileToReplace: inputFile,
        input: tokenReplacement,
        replacementFile: outputFile,
        encoding: encoding
    )
}
