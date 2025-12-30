import com.mcafee.orbit.Credentials.CredentialStore
import java.util.regex.Pattern
/**
 * Query eCM API to get the attributes of a package job, matching the specified constraints
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * ecm.getPackageJobDetails(
 *     project: 'Syscore',
 *     version: '15.5.0',
 *     promotionStateNumber: '100', // or buildState: 'Package Passed'
 * )
 * </pre>
 * @param project [T:String] eCM project name
 * @param version [T:String] Project version
 * @param promotionStateNumber [T:String] [OPTIONAL] Promotional state to filter results on
 * @param buildState [T:String] [OPTIONAL] Build State to filter results on
 * @param buildNumber [T:String] [OPTIONAL] Build number to fetch
 * @param packageNumber [T:String] [OPTIONAL] Package number to fetch
 * @param autoBVT [T:String] [OPTIONAL] Allowed values:
 *     '0': Do not check for eCM AutoBVT flag,
 *     '1': Check or eCM AutoBVT flag
 * @return Object - Package details
 */
public getPackageJobDetails(arg) {
    arg.buildType = "1"
    return this.getBuildOrPackageJobDetails(arg).text
}

/**
 * Query eCM API to get the attributes of a build job, matching the specified constraints
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * ecm.getBuildJobDetails(
 *     project: 'Syscore',
 *     version: '15.5.0',
 *     promotionStateNumber: '100', // or buildState: 'Build Passed'
 * )
 * </pre>
 * @param project [T:String] eCM project name
 * @param version [T:String] Project version
 * @param promotionStateNumber [T:String] [OPTIONAL] Promotional state to filter results on
 * @param buildState [T:String] [OPTIONAL] Build state to filter results on
 * @param buildNumber [T:String] [OPTIONAL] Build number to fetch
 * @param autoBVT [T:String] [OPTIONAL] Allowed values:
 *      '0': Do not check for eCM AutoBVT flag,
 *      '1': Check or eCM AutoBVT flag
 * @return Object - Build details
 */
public getBuildJobDetails(arg) {
    arg.buildType = "0"
    return this.getBuildOrPackageJobDetails(arg).text
}

/**
 *  Orbit internal  method ,invoked by other orbit methods getBuildJobDetails and getPackageJobDetails
 *  <p><b> Sample Usage :</b></p>
 *  <pre>
 *      ecm.getBuildOrPackageJobDetails([
 *          project : "SecretCM",
 *          version : "1.0.0"
 *          buildType : "0"
 *      ])
 *  </pre>
 * @param project eCM project Name
 * @param version Version corresponding to project
 * @param buildType - 0 for build, 1 for package
 * @param promotionStateNumber (optional) Promotional state to filter results on
 * @param buildState (optional) Build State String to filter results on
 * @param buildNumber (optional) Build Number to fetch
 * @param packageNumber (optional) Package Number to fetch
 * @param releaseType (optional) Filer eCM builds on release type
 * @param autoBVT (optional) Check for eCM autoBVT flag
 * @return object
 */
private Map getBuildOrPackageJobDetails(arg) {
    log.debug(arg)
    assert arg.project: "ERROR: arg.project missing!  Specify project name"
    assert arg.version: "ERROR: arg.version missing!  Specify project version"
    assert arg.buildType == "0" || arg.buildType == "1": "ERROR: arg.buildType must be \"1\" or \"0\" , for package/buildjobs respectively"
    arg.project = arg.project.trim()
    arg.version = arg.version.trim()
    log.debug("arg.project.trim()'d: ${arg.project}")
    log.debug("arg.version.trim()'d: ${arg.version}")
    //set method variables
    String ecmProjectName = arg.project
    String ecmProjectVersion = arg.version
    String BuildType = arg.buildType
    String buildNumber = arg.buildNumber
    String packageNumber = arg.packageNumber
    String promotionStateNumber = arg.promotionStateNumber
    String buildState = arg.buildState
    String releaseType = arg.releaseType
    String autoBVT = arg.autoBVT
    String configType = arg.configType

    //todo :validate input
    //todo :validate promotionstate & buildState values

    String ecmURL
    withCredentials([
        usernamePassword(
            credentialsId: CredentialStore.CREDENTIAL_FOR_ECM_API_AUTHENTICATION,
            usernameVariable: 'USERNAME',
            passwordVariable: 'PASSWORD'
        )
    ]) {
        ecmURL = config('eCMAPIURL') + '?login=' + env.USERNAME + '&pass=' + env.PASSWORD + '&noauth=1&NOEX=1&BuildsCount=1&Type=' + BuildType + '&ProjectName=' + ecmProjectName + '&BuildVersion=' + ecmProjectVersion

        if (autoBVT) {
            ecmURL = ecmURL + '&AutoBVT=' + autoBVT
        }

        if (promotionStateNumber) {
            log.debug("orbitbuild: using promotionStateNumber: ${promotionStateNumber}")
            ecmURL = ecmURL + '&PS=' + promotionStateNumber
        }

        if (releaseType) {
            //encode to handle space in buildState string , eg: 'Package Passed'
            ecmURL = ecmURL + '&ReleaseType=' + URLEncoder.encode(releaseType, "UTF-8")
        }
// ORBIT-2583
        if (configType) {
            ecmURL = ecmURL + '&configType=' + configType
        }

        if (buildState) {
            //encode to handle space in buildState string , eg: 'Package Passed'
            ecmURL = ecmURL + '&ST=' + URLEncoder.encode(buildState, "UTF-8")
        }

        if (buildNumber) {
            ecmURL = ecmURL + '&BuildNumber=' + buildNumber
        }

        if (packageNumber) {
            ecmURL = ecmURL + '&PackageNumber=' + packageNumber
        }
        log.info("ecm ecmURL: " + ecmURL)

        String serverResponseText = new URL(ecmURL).getText()
        log.info("ecm serverResponseText: " + serverResponseText)

        def resultObj = [:]
        def lines = serverResponseText.split('\n')
        for (def i = 0; i < lines.size(); i++) {
            def keyVal = lines[i].split('=')
            if (keyVal.size() > 0) {
                def value = null
                if (keyVal.size() == 2) {
                    value = keyVal[1].trim()
                }
                resultObj.put(keyVal[0], value)
            }
        }

        resultObj.put("text", serverResponseText)

        String errorString = "ERROR: " +
                "ecm getBuildOrPackageJobDetails() no project found via the eCM API with: ${arg}" +
                "returning: ${resultObj}"
        assert !utils.isNullOrEmptyString(resultObj.PROJECT): utils.getErrorMessage(errorString)

        return resultObj
    }
}

/**
 * splitECMOverRide
 * Splits an eCM job dependency string into to get version, build number and optionally package number.
 * This is only meant to be used by override in the standard eCM dependency string format
 * D+.D+.D+.D+.D+, where the first 3 digits groups are the version and the 4th digit group is the build number
 * and optionally the 5th digital group to be the package number
 *
 * <h4>Sample usage:</h4>
 * <pre>
 *     ecm.splitECMOverRide(15.0.0.100.1)
 * </pre>
 * @String ecmOverRideString String used to describe the version, build and package details separated by a .
 * @return Obj A struct like object with version, buildNumber and (optionally) packageNumber fields
 */
private Object splitECMOverRide(String ecmOverRideString) {
    def majorVersionNumber, minorVersionNumber, patchVersionNumber, buildNumber, packageNumber
    def eCMDependency = [:]
    def spliteCMDependency = ecmOverRideString.split(/\./)
    Boolean isPackage = false

    //debug logging
    log.debug("ecmOverRideString: " + ecmOverRideString)
    log.debug("spliteCMDependency: " + spliteCMDependency)
    log.debug("spliteCMDependency.size(): " + spliteCMDependency.size())

    if (spliteCMDependency.size() >= 4 && spliteCMDependency.size() <= 5) {
        majorVersionNumber = spliteCMDependency[0]
        minorVersionNumber = spliteCMDependency[1]
        patchVersionNumber = spliteCMDependency[2]
        buildNumber = spliteCMDependency[3]
    } else {
        error "ERROR : ecm.splitECMOverRide() invalid eCM dependency string detected: " + ecmOverRideString
    }

    //deal with package
    if (spliteCMDependency.size() == 5) {
        packageNumber = spliteCMDependency[4]
        isPackage = true
    } else {
        isPackage = false
    }

    //building obj
    eCMDependency.put("version", majorVersionNumber + "." + minorVersionNumber + "." + patchVersionNumber)
    eCMDependency.put("buildNumber", buildNumber)
    if (isPackage) {
        eCMDependency.put("packageNumber", packageNumber)
        eCMDependency.put("isPackage", isPackage)
    }

    //not currently required, but could be useful at a later date
    eCMDependency.put("majorVersionNumber", majorVersionNumber)
    eCMDependency.put("minorVersionNumber", minorVersionNumber)
    eCMDependency.put("patchVersionNumber", patchVersionNumber)

    return eCMDependency
}

/**
 * Downloads the specified dependencies from \\buildmaster server to specified folders
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * downloadDependencyFromBuildMaster ([
 *     project: 'ESP_Installer',
 *     version: '11.0.0',
 *     language: 'Common',
 *     build: '216',
 *     files: [
 *         ('Object\\Release\\ESP*.zip') : 'dependencies\\espInstaller_packages\\'
 *     ]
 * ])
 * </pre>
 * @param project [T:String] Project name. The project name should be in \\buildmaster\project
 * @param version [T:String] Version of the project
 * @param language [T:String] Language of the project e.g. 'Common' or 'English'
 * @param build [T:String] Build number to download
 * @param files Map - '<source filepath>': '<target folder>' key, value pairs (Supports regex)
 */
private void downloadDependencyFromBuildMaster(arg) {
    //TODO: Make Version, Language, build number and files all optional.
    //TODO: Version defaults to highest number
    //TODO: Language defaults to common or english
    //TODO: Build number defaults to latest
    //TODO: Files default to all

    arg.server = "BUILDMASTER"
    this.downloadDependencyFromBuildMasterOrPackageMaster(arg)
}

/**
 * Downloads the specified dependencies from \\packagemaster server to specified folders
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * downloadDependencyFromPackageMaster([
 *     project: 'MA',
 *     version: '5.0.4',
 *     language: 'Common',
 *     build: '283',
 *     files: [
 *         ('Package_1\\SDK\\masdk_win.zip'): 'dependencies\\ma_packages\\',
 *         ('Package_1\\MA500SDK.zip'): 'dependencies\\ma_packages\\'
 *     ]
 * ])
 * </pre>
 * @param String project Name of the project ; The specified project name should be in \\buildmaster\project
 * @param String version version of the project
 * @param String language language of the project,eg: Common or English
 * @param String build buildnumber to download
 * @param dict[] files source=>targetfolder-mapping (Regexp of source):(target_folder_name)
 */
private void downloadDependencyFromPackageMaster(arg) {
    //TODO: Move out to eCM Class, merge with Buildmaster?
    //TODO: Make Version, Language, build number and files all optional.
    //TODO: Version defaults to highest number
    //TODO: Language defaults to common or english
    //TODO: Build number defaults to latest
    //TODO: Files default to all
    arg.server = "PACKAGEMASTER"
    this.downloadDependencyFromBuildMasterOrPackageMaster(arg)
}

/**
 Orbit internal private method, invoked by downloadDependencyFromBuildMaster or downloadDependencyFromPackageMaster
 */
private void downloadDependencyFromBuildMasterOrPackageMaster(arg) {
    if (arg.objectPath) {
        arg.objectPath = "\\" + arg.objectPath.replaceAll("\\\\\\\\", "\\\\")
    }
    if (arg.PACKAGE_PATH) {
        arg.PACKAGE_PATH = "\\" + arg.PACKAGE_PATH.replaceAll("\\\\\\\\", "\\\\")
    }
    log.debug(arg)
    //check validity of folder name

    assert arg.project ==~ /^[a-zA-Z0-9-_]+$/: "ERROR: this.downloadDependencyFromBuildMasterOrPackageMaster(): Provided project \"" + arg.project + "\" does not match regex /^[a-zA-Z0-9-_]+\$/:full arg" + arg

    def networkPath
    String creds
    String domain = ""
    def retries = 20
    def timeout = 30
    def sleep = 20
    if(arg.server == "BUILDMASTER") {
        creds = 'CREDENTIAL_FOR_BUILDMASTER_DRIVE_MAPPING'
        networkPath = arg.OBJECT_PATH
    }
    else if(arg.server == "PACKAGEMASTER") {
        creds = 'CREDENTIAL_FOR_PACKAGEMASTER_DRIVE_MAPPING'
        networkPath = arg.PACKAGE_PATH
    }
    else if(arg.server == "BEA-ECMFILE1") {
        creds = 'CREDENTIAL_FOR_BUILDMASTER_DRIVE_MAPPING'
        networkPath = (arg.OBJECT_PATH) ? arg.OBJECT_PATH : arg.PACKAGE_PATH
    }
    else {
        throw new IllegalArgumentException(
                'Invalid server specified for dependency download'
        )
    }
    // This is a workaround to ensure all URLS are converted ultimately to entsec.com
    networkPath = networkPath.replaceAll('corp.nai.org', 'corpzone.internalzone.com')
    networkPath = utils.formatPathForOS(networkPath.replaceAll('corpzone.internalzone.com', 'corp.entsec.com'))
    log.debug("networkPath: ${networkPath}")

    if (isUnix()) {
        def recurseVal = (arg.recursive == true) ? "" : " -m --exclude='/*/' "
        def mountName = "/tmp${networkPath}/"
        mountName = mountName.replaceAll('//', '/')

        log.debug("mountName: ${mountName}")
        log.debug("networkPath: ${networkPath}")

        withCredentials([
                usernamePassword(
                        credentialsId: CredentialStore[creds],
                        usernameVariable: 'USERNAME',
                        passwordVariable: 'PASSWORD'
                )
        ]) {
            env.current_task = 'mounting network location'
            try {

                if ( node.isMac() ) {
                    sh "sudo diskutil unmount " + mountName + " || exit 0"
                    sh "mkdir -pv \'${mountName}\'"
                    networkPath = networkPath.replaceAll("//","")
                    String user = env.USERNAME.split('@')[0]
                    String encoded_pass = URLEncoder.encode(env.PASSWORD, "UTF-8")
                    wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: encoded_pass]]]) { 
                        utils.exponentialRetry(retries, timeout, sleep) {
                            sh "sudo mount -v -t smbfs -o -f=0755,-d=0755  \'//" + domain + user + ":" + encoded_pass + "@" + networkPath + "\' ${mountName}"
                        }
                    }
                    
                    
                    
                    
                } else {
                    sh "sudo umount " + mountName + " || exit 0"
                    sh "mkdir -pv \'${mountName}\'"
                    String smbVer = "1.0"
                    if (networkPath.contains("bea-ecmfile1")){
                        smbVer="2.0"
                    }
                    utils.exponentialRetry(retries, timeout, sleep) {
                        sh "sudo mount -v -t cifs -r -o username=\'" + env.USERNAME + "\',password=\'" + env.PASSWORD + "\',vers=" +smbVer + ",sec=ntlmssp " + networkPath + " ${mountName} "
                    }
                    
                }

                log.debug(
                        'Listing mount:',
                        { sh(script: "sudo ls -lash /tmp; sudo ls -lsah ${mountName}", returnStdout: true) }
                )

                if (arg.files != null) {
                    def sourceFiles = arg.files.keySet().toArray()
                    for (def i = 0; i < sourceFiles.size(); i++) {
                        String sourceFile = sourceFiles[i]
                        String destinationDir = arg.files.get(sourceFile) ?: "*"
                        sourceFile = utils.formatPathForOS(sourceFile)
                        destinationDir = utils.formatPathForOS(destinationDir)
                        String sourceFileWithoutPath
                        String sourceDir
                        if (sourceFile.contains('/')) {
                            sourceFileWithoutPath = sourceFile.tokenize('/').last()
                            sourceDir = mountName + (sourceFile - sourceFileWithoutPath)
                        } else {
                            sourceFileWithoutPath = sourceFile
                            sourceDir = mountName
                        }

                        String rsyncCommand = "sudo rsync -az ${recurseVal} --include='${sourceFileWithoutPath}' --include='*/' --exclude='*' ${sourceDir} ${destinationDir}"
                        log.debug("Running rsync command: ${rsyncCommand}")
                        assert utils.silentCmdWithStatus(rsyncCommand) == 0 : "The rsync command failed."
                    }
                } else {
                    log.info("No files provided, assuming every file and folder under \"${mountName}\"")
                    String rsyncCommand = "sudo rsync -az ${recurseVal} ${mountName} ${env.WORKSPACE}"
                    log.debug("Running rsync command: ${rsyncCommand}")
                    assert utils.silentCmdWithStatus(rsyncCommand) == 0 : "The rsync command failed."
                }
                sh "sudo chown -R orbit ${env.WORKSPACE}"
            } catch (Throwable err) {
                radar.addBuildLog(
                    "Error",
                    "Ecm_download",
                    env.current_stage,
                    networkPath,
                    err.message,
                )
                rethrow(err)
            } finally {
                if ( node.isMac() ) {
                    sh "sudo diskutil unmount " + mountName + " || exit 0"
                } else {
                    sh "sudo umount " + mountName + " || exit 0"
                }
            }
        }
    } else {
        //check validity of folder name
        def recurseVal = (arg.recursive == true) ? " /S /E" : ""

        def temporaryDriveLetter = utils.getAnAvailableDriveLetter()
        def pathToBuild
        if (arg.server == "PACKAGEMASTER") {
            log.debug("pathToBuild = \"${arg.PACKAGE_PATH}\"")
            pathToBuild = "${arg.PACKAGE_PATH}"
        } else { //buildmaster
            log.debug("arg.objectPath = \"${arg.objectPath}\"")
            pathToBuild = "${arg.objectPath}"
        }

        pathToBuild = pathToBuild.replaceAll('corp.nai.org', 'corp.entsec.com')
        log.debug("pathToBuild: ${pathToBuild}")
        try {
            log.debug("temporaryDriveLetter for  " + arg.project + " is " + temporaryDriveLetter)
            def networkPathMap =
                    [
                            credentialName     : creds,
                            remoteServer       : arg.server.toLowerCase() + ".corp.entsec.com",
                            folderName         : ((arg.server.toLowerCase() == "bea-ecmfile1") ? "ECM_delivery" : arg.project),
                            driveLetter        : temporaryDriveLetter,
                            useScratchPadDrives: false,
                    ]
            utils.exponentialRetry(retries, timeout, sleep) {
                utils.mapNetworkFolder(networkPathMap)
            }

            // to prevent devs overwriting env.WORKSPAce AND ACCIDENTALLY modifying buildmaster content"
            assert (env.WORKSPACE.toString().take(2) ==~ /^[CceE]\:/): " WORKSPACE " + env.WORKSPACE + " not in C or E drive"

            def mountName = networkPath
            if (arg.files) {
                def sourceFiles = arg.files.keySet().toArray()
                for (def i = 0; i < sourceFiles.size(); i++) {
                    def sourceFile = sourceFiles[i]
                    def destinationFile = arg.files.get(sourceFile) ?: "*"
                    def fullPathOfDestinationFile = pwd() + "\\" + destinationFile
                    //def fullPathOfSourceFile = pathToBuild + "\\" + sourceFile //pathToBuild is undefined...
                    log.debug("mountName: " + mountName)
                    log.debug("sourceFile: " + sourceFile)
                    log.debug("destinationFile: " + destinationFile)
                    def fullPathOfSourceFile = utils.formatPathForOS(mountName + "\\" + sourceFile)
                    def pathToken = isUnix() ? "/" : "\\"
                    def formattedFile = fullPathOfSourceFile.tokenize(pathToken).last()
                    def formattedPath = fullPathOfSourceFile - formattedFile

                    log.debug("formattedFile: ${formattedFile}")
                    log.debug("formattedPath: ${formattedPath}")

                    assert !(destinationFile ==~ / /): "Specified destination " + destinationFile + " contains space character in path"

                    //todo: make calls download OS aware, isUnix() etc
                    try {
                        utils.runRobocopy([src: formattedPath, dest: fullPathOfDestinationFile, file: "\"${formattedFile}\"", replaceFlags: true, flags: "/mt /R:60 /W:60 /v" + recurseVal])
                    } catch (Throwable err) {
                        log.error("robocopy failed will retry with xcopy")
                        bat "xcopy " + recurseVal + " /Y /I \"" + fullPathOfSourceFile + "\" \"" + fullPathOfDestinationFile + "*\""
                    }
                }
            } else {
                log.info("No files provided, assuming every file and folder under \"${mountName}\"")
                utils.runRobocopy([src: mountName, dest: pwd(), file: "*", replaceFlags: true, flags: "/mt /R:60 /W:60 /v" + recurseVal])
            }
        } catch (Throwable e) {
            radar.addBuildLog(
                "Error",
                "Ecm_download",
                env.current_stage,
                networkPath,
                e.message,
            )
            log.error('Failed to copy dependency from network share')
            rethrow(e)
        } finally {
            utils.disconnectDriveMapping(temporaryDriveLetter)
        }
    }
}
