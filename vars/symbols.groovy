/**
 * Uploads symbol files from the build workspace to Orbit's symbol server.
 *
 * <p>Also archives the symbols log and stashes it for subsequent Artifactory upload</p>
 * <p>Symbol transaction IDs are uploaded to Radar</p>
 * <p>Fails the build if a symbol upload fails</p>
 * <p>Only supported on Windows</p>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * def localSymbolLocations = [
 *    "Release Binaries": "sdk\\release\\*",
 *    "Debug Binaries": "sdk\\debug\\*",
 * ]
 * symbols.uploadSymbols(localSymbolLocation)
 * </pre>
 * @param localSymbolsLocation [T:Map] A map of upload comments and local symbol locations.
 **/
public void uploadSymbols(def localSymbolsLocation) {
    /*
        1. Takes in a list of symbol locations which are used to upload to the symbol share
        2. Validates the variables, sets variable defaults, runs a series of validation assertions tests
        3. Generates the required command using symbol server for copying the list of files into a single
            network file share
        4. Returns functions results in correct order to setup server, upload to symbols to the server and
            upload the transaction id to Radar
        5. If all the functions have run succesfully, archive full symbols log and stash it for Artifactory upload
        6. Validates the return values of each function and either continues or fails the build based on boolean
            values returned from the functions
     */
    global()

    def symbolLocations = localSymbolsLocation
    String buildEnviroment = config('ORBIT_BUILD_ENV')
    boolean serverSetUpResult = false
    boolean symStoreUploadResult = false
    boolean returnUploadRadar = false
    List fullJobName = env.JOB_NAME.split("/")
    // focuses on the .size(4) since thats the component name.
    String componentName = fullJobName[1]
    String jobName = componentName.toUpperCase()
    Boolean testJob = env.ORBIT_TEST_JOB?.toBoolean() ?: false
    def driveLetterSymbols = utils.getAnAvailableDriveLetter()
    def commandContainer = []
    String uploadCommand = ""
    String symbolsUploadLogFile = "orbitUploadSymbolsResult.log"
    String symbolsUploadLogFull = "orbitUploadSymbolsComplete.log"
    // Logic will be negated later, need it not to fail for dev and qa while we are testing. Remove before its merged to pipeline-common master.
    if (buildEnviroment.toUpperCase() == "PROD" || testJob) {
        log.info("Upload sysmbols running in ${env.ORBIT_BUILD_ENV} enviroment.")
    } else {
        log.info("Skipping upload of symbols as this is not a Production system.")
        return
    }


    if (isUnix()) {
        // should fail on unix until we support it.
        utils.getErrorMessage("uploadSymbols: Currently not supported on UNIX systems.")
    }
    assert fileExists(global.path.WIN_SYMBOL_STORE_EXE): utils.getErrorMessage("ERROR: uploadSymbols, Unable to find the symstore.exe :  " + global.path.WIN_SYMBOL_STORE_EXE)

    log.info("Listing locally generated symbols to be uploaded and upload commands: ")
    // Creating the dynamic command which will be used to upload symbols to the share.

    for (def item in symbolLocations.entrySet()) {
        assert !utils.isNullOrEmptyString("${item.key}"): utils.getErrorMessage("Empty or null value provided for the symbols comment : ${item.key}.")
        assert !utils.isNullOrEmptyString("${item.value}"): utils.getErrorMessage("Empty or null value provided for the symbols location : ${item.value}.")
        log.info("Comment = ${item.key}, Symbols Location = ${item.value}")
        uploadCommand = global.path.WIN_SYMBOL_STORE_EXE + ' add /compress /f ' + item.value + ' /o /r /s \"' + driveLetterSymbols + ':\\' + jobName + '\" /t ' + jobName + ' /v ' + env.BUILDVERSION + ' /c \"' + item.key + '\" > ' + symbolsUploadLogFile
        log.info("symbolsUploadCommand: " + uploadCommand)
        commandContainer.add(uploadCommand)
    }

    log.debug("Build version : " + env.BUILDVERSION)
    log.debug("Job name : " + jobName)

    // Only send transaction ID if symbols set up has completed succesfully.
    try {
        serverSetUpResult = this.symbolsServerSetUp(driveLetterSymbols, jobName)
        if (serverSetUpResult) {
            log.info("Symbols server set up ran succesfully, uploading the provided symbol locations and comments to the symbol store.")

            for (i = 0; i < commandContainer.size(); i++) {
                symStoreUploadResult = this.symStoreUpload(commandContainer[i], symbolsUploadLogFile)
                if (symStoreUploadResult) {
                    radar.sendSymbolsTransactionID(symbolsUploadLogFile)
                    // Appending single transaction log to log containing all the upload transactions
                    // Clear the single transaction log for next transaction to populate its output
                    utils.silentCmdWithStatus("type ${symbolsUploadLogFile} >> ${symbolsUploadLogFull} && break>${symbolsUploadLogFile}")
                }
            }
        } else {
            utils.getErrorMessage("Symbols server set up failed, please check provided parameters.")
        }

    } catch (Throwable e) {
        log.error("Error detected during the symbol upload")
        rethrow(e)
    } finally {
        utils.disconnectDriveMapping(driveLetterSymbols)
    }

    if (symStoreUploadResult && serverSetUpResult) {
        log.info("symbols.uploadSymbol() : Completed succefully for ${jobName}.")
        log.info("symbols.uploadSymbol() : Archiving and stashing files : ")
        archiveArtifacts symbolsUploadLogFull
        stash includes: '**', name: symbolsUploadLogFull

    } else {
        utils.getErrorMessage("symbols.uploadSymbol() : Failed for ${jobName}.")
    }
}

/**
 * Uploads the provided symbol folders to the symbols share and returns boolean to the user.
 * <p><b>What it Does:</b></p>
 * <pre>
 * 1. Takes in symbolsUploadCommand as parameter.
 * 2. Runs the upload command.
 * 3. Uploads the symbols to the symbol share configured in radar.
 * 4. Checks if the log generated by the symbolstore.exe on the client contains any errors and fail if so, otherwise return true to the calling function.
 * </pre>
 * <p><b>Example Code Usage:</b></p>
 * <code>
 *  this.symStoreUpload( "symstore.exe ...", "singleSymbolTransaction.log")
 * </code>
 * <p><b>Method Details:</b></p>
 * @param symbolUploadCommand : A String item in an Array which is the dynamic command for uploading the symbols.
 * @param logFile : Name of the file to which the symbolstore.exe output will be redirected.
 * @return boolean : Success or Failure based if all the symbols have been successfully uploaded to the symbols server. If any commands in the array fail, fail the build.
 * </pre>
 **/
private boolean symStoreUpload(symbolsUploadCommand, logFile) {
    global()
    try {
        // Only return true if symbols and transaction ID have been uploading succesfully
        def symbolCommadReturn = utils.silentCmdWithStatus(symbolsUploadCommand)
        if (symbolCommadReturn == 0) {
            // Check if last transaction upload file contains errors before continue to Radar upload.
            // orbitUploadSymbolsResult.txt output of transaction server
            dir(env.WORKSPACE) {
                def checkSymbolLog = utils.silentCmdWithStatus("${global.path.GNU_GREP} -q \"Number of errors = 0\" ${logFile}")
                assert (checkSymbolLog == 0): "Error: uploaded symbol files failed using the following command : ${symbolsUploadCommand}."
            }
        }
    } catch (Throwable err) {
        log.warn("Failed to upload symbols to symbol store using the following command: ${symbolsUploadCommand}.")
        return false
    }
    return true
}

/**
 * Checks to see if the project which is the current build job contains the required folders on the symbols server and generates them if they dont exist.
 * For now its just the project name. However in the future we might need to generate more dynamic folders.
 * <p><b>What it Does:</b></p>
 * <pre>
 * 1. Takes in the mapped drive letter, jobName and dynamically creates up the project folder on the symbol share after mapping the network drives.
 * 2. Sets up the remote project folder if it does not exist already.
 * 3. Sends back a response to the calling function if it failed or not (boolean)
 * </pre>
 * <p><h3><b>Example Code Usage:</b></h3> </p>
 * <code>
 *  this.symbolsServerSetUp([
 *  driveLetterSymbols : "D",
 *  jobName : "VSCORE"
 * ])
 * </code>
 * </pre>
 * <p><h3><b>Method Details:</b></h3> </p>
 * <pre>
 * @param driveLetterSymbols : The drive letter mapped to the remote file share.
 * @param jobName : Name of the project for which to store symbols.
 * @return boolean :  True - Success or False - Failure
 * </pre>
 **/
private boolean symbolsServerSetUp(String driveLetterSymbols, String jobName) {
    String symbolShare = config('DEFAULT_SYMBOLS_SHARE')
    String componentSymbolShare = "${symbolShare}\\ORBIT\\${jobName}"
    String projectName = symbolShare + "\\" + jobName
    int createProjectReturn
    boolean returnStatus = false

    try {
        String symbolServerDriveLetter = utils.mapNetworkFolder([
                remoteServer       : symbolShare,
                credentialName     : "CREDENTIAL_FOR_BUILDMASTER_DRIVE_MAPPING",
                folderName         : "ORBIT",
                driveLetter        : driveLetterSymbols,
                remoteDomain       : '',
                useScratchPadDrives: false,
        ])

        if (!fileExists(symbolServerDriveLetter + ":\\${jobName}")) {
            log.info("Creating remote project: " + symbolServerDriveLetter + ":\\${jobName}")
            createProjectReturn = utils.silentCmdWithStatus("md ${symbolServerDriveLetter}:\\${jobName}")
        } else {
            log.info("Project for symbols upload has already been created on the symbol share: ${componentSymbolShare}. ")
            createProjectReturn = 0
        }

        if (createProjectReturn == 0) {
            log.info("symbolsServerSetUp: Succesfully generated required symbol folder for: ${jobName}.")
            returnStatus = true
        } else {
            log.info("Failed to generate required folder for symbol upload for ${jobName}.")
            returnStatus = false
        }

    } catch (Throwable e) {
        log.error('Failed to set up symbols server')
        rethrow(e)
    } 
    return returnStatus
}

