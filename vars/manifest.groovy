/**
 * Returns a List of filenames based on matching variables in a manifest file.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * manifest.processManifestList("BuildScripts\\signing_manifest.pl", env.WORKSPACE + " Release")
 * </pre>
 * @param requiredManifest [T:String] Manifest file name.
 * @param enviromentVariables [T:String] Variables used to retrieve the required data from the manifest file.
 * @return [T:List] The list of filenames
 */
public List processManifestList(String requiredManifest, String enviromentVariables) {

    this.runManifestDriver(requiredManifest, enviromentVariables)

    def outputDataList = []

    try {
        def outputDataString = readFile file: requiredManifest + "_output.txt"
        outputDataList = outputDataString.split("\n")
        (outputDataList) ?: utils.getErrorMessage("Required values did not generate for " + requiredManifest)

        outputDataList = outputDataList.collect { it.trim() }
    } catch (Throwable e) {
        log.error('Failed to process manifest file')
        rethrow(e)
    }
    return outputDataList
}

/**
 * Returns a Map of src/dest filenames based on matching variables in a manifest file.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * manifest.processManifestSource("BuildScripts\\signing_manifest.pl", env.WORKSPACE + " Release")
 * </pre>
 * @param requiredManifest [T:String] Manifest file name
 * @param enviromentVariables [T:String] Variables used to retrieve the required data from the manifest file
 * @return [T:Map] Where key=first string (usually src) & value=second string (usually destination)
 */
public Map processManifestSource(String requiredManifest, String enviromentVariables) {
// Set up
    this.runManifestDriver(requiredManifest, enviromentVariables)
    def outputDataMap = [:]
// Processing
    try {
        def outputDataString = readFile file: requiredManifest + "_output.txt"
        def outputDataList = outputDataString.split("\n")
        (outputDataList) ?: utils.getErrorMessage("Required values did not generate for " + requiredManifest)
// Casting
        outputDataList.each {
            def individualValue = it.split(',')
            individualValue[0] = individualValue[0].trim()
            individualValue[1] = individualValue[1].trim()
            outputDataMap.put(individualValue[0], individualValue[1])
        }
    } catch (Throwable e) {
        log.error('Failed to process manifest file')
        rethrow(e)
    }
    log.debug("Returning the following map to the user:")
    log.debug(outputDataMap)
    return outputDataMap
}

/**
 * Returns a multi-dimensional array of values based on matching variables in a manifest file.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * manifest.processManifestMultidimensional("BuildScripts\\signing_manifest.pl", env.WORKSPACE + " Release")
 * </pre>
 * @param requiredManifest [T:String] Manifest file name.
 * @param enviromentVariables [T:String] Variables used to retrieve the required data from the manifest file.
 * @return First array contains the line number in the manifest file, second array contains the
 *     matching strings for that line
 */
public String[][] processManifestMultidimensional(String requiredManifest, String enviromentVariables) {

    this.runManifestDriver(requiredManifest, enviromentVariables)

    def outputDataList
    String[][] manifestData

    try {
        def outputDataString = readFile file: requiredManifest + "_output.txt"
        outputDataList = outputDataString.split("\n")
        (outputDataList) ?: utils.getErrorMessage("Required values did not generate for " + requiredManifest)

        /*
        The following code, loops through data from the manifest file, sets the the first dimension of manifestData
        to the number of lines in the files and the second dimension is set based on number of values in each line.
        A nested loop is executed to populate the first and second dimensions of the manifestData array.
         */
        def splittedSecondDimensions = outputDataList[0].split(",")
        manifestData = new String[outputDataList.size()][splittedSecondDimensions.size()]

        for (int i = 0; i < outputDataList.size(); i++) {
            def secondDimensionArray = outputDataList[i].split(",")
            for (int y = 0; y < secondDimensionArray.size(); y++) {
                manifestData[i][y] = secondDimensionArray[y]
            }
        }
    } catch (Throwable e) {
        log.error('Failed to process manifest file')
        rethrow(e)
    }
    log.debug("Returning the following map to the user:")
    log.debug(manifestData)

    return manifestData
}

/**
 * Runs the Manifest driver against the required manifest file.
 * <ol>
 * <li>Takes in 2 mandatory parameters (String requiredManifest, String envs).</li>
 * <li>Loads the required perl manifest driver via libraryResource step.</li>
 * <li>Runs a perl script to generate the required output file.</li>
 * </ol>
 * <h4>Sample usage:</h4>
 * <pre>
 * manifest.runManifestDriver("signing_manifest.pl", "sign1 sign2 sign 3")
 * </pre>
 * <pre>
 * @param requiredManifest [T:String] file which contents to replace.
 * @param enviromentVariables [T:String] the variables used to retrive the required data from requiredManifest.
 * </pre>
 */
private void runManifestDriver(String requiredManifest, String enviromentVariables) {

    (!(utils.isNullOrEmptyString(requiredManifest)) || !fileExists(requiredManifest)) ?: utils.getErrorMessage(requiredManifest + " not found in the workspace...")
    global()
    def currentStage = env.STAGE_NAME

    enviromentVariables = utils.isNullOrEmptyString(enviromentVariables) ?  "" : enviromentVariables

    try {
        def processManifestString = libraryResource 'org/mcafee/manifest/manifestDriver.pl'
        def processManifestDriver = env.WORKSPACE + "\\manifestJob\\manifestDriver.pl"
        writeFile(file: processManifestDriver, text: processManifestString)

        def generateManifestCommand = global.path.WIN_PERL_EXE + ' ' + processManifestDriver + ' ' + requiredManifest + ' ' + enviromentVariables
        log.debug("Retrieving required output from :" + requiredManifest + " " + enviromentVariables)
        log.debug(generateManifestCommand)

        def returnStatus = bat(returnStatus: true, script: generateManifestCommand)
        if (returnStatus != 0) {
            utils.getErrorMessage("Failed to generate required manifest. Process exited with non-zero status.")
        }
    } catch (Throwable e) {
        log.error('Failed to process manifest file')
        rethrow(e)
    }
}
