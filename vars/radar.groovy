import com.mcafee.orbit.Credentials.CredentialStore
import com.mcafee.orbit.Utils.EmailUtils
import com.mcafee.orbit.Utils.StringUtils
import com.mcafee.orbit.Deprecated

/**
 * Creates the build record in Radar.
 *
 * Should not be invoked from user pipelines.
 */
private def call() {
    global()
    log.debug("Creating the build record in Radar...")
    def user = ''
    try {
        // Get the name of the user who triggered the build.
        user = currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
    } catch (Throwable e) {
        log.warn("Could not find the user info, and it will not be updated in radar.")
    }
    def branch = 'in-line script'
    try {
        branch = scm.branches[0].name
    } catch (Throwable _) {
        log.debug("Could not get the branch name, assuming in-line script.")
    }
    if (!env.BRANCH_NAME) {
        env.BRANCH_NAME = branch
    }
    def splitFullJobName = env.JOB_NAME.split('/')
    def payload = [
        Branch: branch,
        BugTrackingComponents: env.BugTrackingComponents,
        BugTrackingProjectName: env.BugTrackingProjectName,
        BugTrackingProjectVersion: env.BugTrackingProjectVersion,
        BuildMilestone: 'Queued',
        BuildSystemJobName: splitFullJobName[2],
        BuildSystemNumber: env.BUILD_NUMBER,
        Component: splitFullJobName[1],
        ComponentVersion: env.BUILDVERSION,
        Guid: env.orbitGUID,
        KickOffComment: env.KickOffComment,
        PackageToBuildNumber: StringUtils.nullableInteger(env.buildToPackage),
        PackageToLatestBuildMilestone: env.buildMileStoneOfBuildToPackage,
        PackageType: env.packageType,
        Parameters: params,
        Product: splitFullJobName[0].replace("_", " "),
        Type: env.ORBIT_JOB_TYPE ?: "Default",
        Url: env.BUILD_URL,
        UserInfo: user,
    ]
    utils.mapReporter(payload)
    def response = request.post(
        url: global.radarUrl.BuildRecord,
        error: 'Failed to create the build record in Radar.',
        token: 'orbit_radar_cred',
        master: true,
        json: payload,
    )
    orbit.buildRecordId(response.BuildRecordID)
    if (!env.ORBIT_JOB_TYPE) {
        env.ORBIT_JOB_TYPE = "Default"
    }
    switch (env.ORBIT_JOB_TYPE?.toUpperCase()) {
        case [null, "", "DEFAULT", "BUILD"]:
            orbit.buildNumber(response.BuildNumber)
            orbit.buildNumberOnly(response.BuildNumber)
            currentBuild.description = "Version: " + env.BUILDVERSION + '\n'
            currentBuild.description += "Build: " + response.BuildNumber + '\n'
            break

        case "PACKAGE":
            orbit.buildNumber("${response.BuildNumber}.${response.PackageNumber}")
            orbit.buildNumberOnly(response.BuildNumber)
            orbit.packageNumber(response.PackageNumber)
            currentBuild.description =  "Version: " + env.BUILDVERSION + '\n'
            currentBuild.description += "Repackage of build: " + response.BuildNumber + '\n'
            currentBuild.description += "Package: " + response.PackageNumber + '\n'
            break

        case "BUILDANDPACKAGE":
            orbit.buildNumber("${response.BuildNumber}.${response.PackageNumber}")
            orbit.buildNumberOnly(response.BuildNumber)
            orbit.packageNumber(response.PackageNumber)
            currentBuild.description = "Version: " + env.BUILDVERSION + '\n'
            currentBuild.description += "Build: " + response.BuildNumber + '\n'
            currentBuild.description += "Package: " + response.PackageNumber + '\n'
            break

        default:
            error "Build type '${env.ORBIT_JOB_TYPE}' not recognised, please review Orbit documentation."
            break
    }

    currentBuild.displayName = orbit.buildNumber()
    log.info("Creating CMDB BOM record")
    bom.initBOM()
}

/**
 * Set build result in Radar.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.updateBuildResultInRadar(
 *     BuildResult: 'SUCCESS',
 *     BuildComment: 'Build passed',
 * )
 * </pre>
 * @param BuildResult [T:String] Result of the build step.
 * @param BuildComment [T:String] [OPTIONAL] Comment to be displayed in Radar.
 */
private def updateBuildResultInRadar(Map arg) {
    global()
    return request.post(
        url: global.radarUrl.AddBuildResult,
        error: 'Failed to set the build result in Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordID: orbit.buildRecordId(),
            BuildResult: arg.BuildResult,
            BuildComment: arg.BuildComment,
        ],
    )
}

/**
 * Set a milestone for a build in Radar.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.updateBuildMileStoneInRadar(
 *     buildMilestone: 'BUILD FAILED',
 *     username: 'auser',
 *     comment: 'Build has failed during delivery',
 * )
 * </pre>
 * @param buildMilestone [T:String] The build milestone you wish to set.
 * @param username [T:String] [OPTIONAL] Username of account requesting the milestone update.
 * @param comment [T:String] [OPTIONAL] Comment to be displayed against the milestone in Radar.
 */
public def updateBuildMileStoneInRadar(Map arg) {
    global()
    return request.post(
        url: global.radarUrl.UpdateBuildMilestone,
        error: 'Failed to update the milestone in Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordID: orbit.buildRecordId(),
            BuildMilestone: arg.buildMilestone,
            Username: arg.username,
            Comment: arg.comment,
        ],
    )
}

/**
 * Sets the Artifactory URL for this build in Radar.
 *
 * <h4>Sample usage</h4>
 * <pre>
 * radar.setArtifactoryUrlInRadar(
 *     artifactoryURL: 'https://artifactory.trellix.com/artifactory/COMPONENT-local/component_version/b1/',
 *     artifactoryBuildURL: 'https://artifactory.trellix.com/artifactory/webapp/#/builds/component_version/1',
 *     artifactsRoot: 'COMPONENT-local/component_version/b1/',
 *     artifactoryInstanceName: 'trellix',
 * )
 * </pre>
 * @param artifactoryURL [T:String] The URL to the build artifacts in Artifactory.
 * @param artifactoryBuildURL [T:String] The URL to the build general information page in Artifactory.
 * @param artifactsRoot [T:String] The path to the artifacts.
 * @param artifactoryInstanceName [T:String] The name of the artifactory instance.
 */
public def setArtifactoryUrlInRadar(Map arg) {
    global()
    return request.post(
        url: global.radarUrl.SetArtifactoryUrl,
        error: 'Failed to set Artifactory URL in Radar',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordID: orbit.buildRecordId(),
            ArtifactoryUrl: arg.artifactoryURL,
            ArtifactoryBuildUrl: arg.artifactoryBuildURL,
            artifactsRoot: arg.artifactsRoot,
            ArtifactoryInstanceName: arg.artifactoryInstanceName,
        ],
    )
}

/**
 * Update the build status in Radar.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.updateJobStatusInRadar(
 *     stage: "Delivery",
 *     stepResult: "SUCCESS",
 * )
 * </pre>
 * @param stage [T:String] The name of the stage to update.
 * @param stepResult [T:String] The status of the stage.
 */
@Deprecated(['radar.markBuildStep()'])
public def updateJobStatusInRadar(Map step) {
    deprecated()
    markBuildStep(
        StepName: step.stage,
        StepResult: step.stepResult
    )
}

/**
 * Update a build step in Radar.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.markBuildStep(
 *     StepName: 'Compile',
 *     StepResult: 'Pass',
 * )
 * </pre>
 * @param StepName [T:String] The name of the step to update.
 * @param StepResult [T:String] The status of the step.
 */
public def markBuildStep(Map step) {
    global()
    return request.post(
        url: global.radarUrl.AddBuildStepData,
        error: 'Failed to update build step in Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordID: orbit.buildRecordId(),
            StepName: step.StepName,
            StepResult: step.StepResult,
        ]
    )
}

/**
 * Get a build record from Radar.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.getBuildRecord(123)
 * </pre>
 * @param buildRecordId [T:int] The build record id.
 * @return [T:Map] The build record.
 */
public Map getBuildRecord(def buildRecordId) {
    global()
    return request.get(
        url: global.radarUrl.GetBuildRecord,
        error: 'Failed to get the build record from Radar.',
        token: 'orbit_radar_cred',
        params: [
            BuildRecordID: buildRecordId,
        ]
    )
}

/**
 * Search Radar for a build by type.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.buildSearch(
 *     projectName: "Radar",
 *     componentName: "Radar_Client",
 *     componentVersion: "1.0.0",
 * )
 * </pre>
 * @param projectName [T:String] The product name.
 * @param componentName [T:String] The component name.
 * @param componentVersion [T:String] The component version.
 * @param buildNumber [T:String] [OPTIONAL] The build number.
 * @param type [T:String] [OPTIONAL] The type Build/Package.
 * @param packageType [T:String] [OPTIONAL] The package type.
 * @param packageNumber [T:String] [OPTIONAL] The package number.
 * @param milestone [T:String] [OPTIONAL] The build milestone, defaults to is at least build passed.
 * @param useExactMilestone [T:boolean] [OPTIONAL] Whether to use the exact milestone. [DEFAULT:true]
 * @return [T:Map] The build record.
 */
public Map buildSearch(Map arg) {
    global()
    // Radar expects a `productName` key
    arg.put("productName", arg.remove("projectName"))
    // Account for build number using orbit BUILD.PACKAGE format
    if (arg.buildNumber?.contains('.')) {
        def index = arg.buildNumber.lastIndexOf('.')
        arg.buildNumber = arg.buildNumber[0..index-1]
        arg.packageNumber = arg.buildNumber[index+1..-1]
        log.debug(
            "Overriding build number to ${arg.buildNumber} " +
            "and package number to ${arg.packageNumber}."
        )
    }
    return request.get(
        url: global.radarUrl.BuildSearchByType,
        error: 'Radar build search failed',
        token: 'orbit_radar_cred',
        params: arg,
    )
}

/**
 * Marks a build as Complete in Radar
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.markBuildComplete()
 * </pre>
 */
private void markBuildComplete() {
    def info = artifacts.getArtifactoryInfo()
    def filename = env.CONSOLE_TEXT_LOGFILE_NAME
    if (StringUtils.isNullOrEmpty(filename)) {
        def splitFullJobName = env.JOB_NAME.split('/')
        filename = splitFullJobName[2] + "-ConsoleText-" + StringUtils.getPaddedRandomNumber() + ".log"
    }
    try {
        retry(5) {
            if (!env.skipUploadConsoleTextToArtifactory) {
                build(
                    job: 'ADMIN/uploadConsoleTextToArtifactory',
                    quietPeriod: 300,
                    wait: false,
                    parameters: [
                        string(name: 'ArtifactoryUrl', value: info.artifactoryURL + '/' + filename),
                        string(name: 'BuildUrl', value: env.BUILD_URL),
                        string(name: 'CredentialId', value: info.credentialId),
                    ]
                )
            } else {
                log.info('skipUploadConsoleTextToArtifactory turned on, skipping to trigger uploadConsoleTextToArtifactory job')
            }
        }
    } catch (Throwable e) {
        log.error('Failed to build uploadConsoleTextToArtifactory')
    }
}

/**
 * Uploads transaction ID to Radar from the the log generated by symstore.exe.
 * Retrieve the last transaction ID from the symbols log file and sends the transaction ID to Radar.
 * <p><b>Example Code Usage:</b></p>
 * <pre>
 * radar.sendSymbolsTransactionID("orbitUploadSymbolsResult.log")
 * </pre>
 * @param symbolsUploadLogFile The log file which was generated by symbol store executable.
 *                             This is used to read the last transaction ID.
 */
private def sendSymbolsTransactionID(String symbolsUploadLogFile) {
    global()
    // Reading the log produced by symstore.exe
    String transactionUploadResult = readFile file: symbolsUploadLogFile
    def transactionUploadResultArray = transactionUploadResult.split("SYMSTORE MESSAGE");
    def transactionIDContainer = transactionUploadResultArray[3]
    // If the symbols have been run the first time for a component, make sure to set transaction id to 0000000001 which is the first transcation id generated for any project
    // Otherwise transactionID defaults to just 0 in the log file but 0000000001 on the server log and therefore a mismatch which leads to symbols failing to delete due to id lookup failure.
    String transactionID = "0000000001"
    if (!transactionIDContainer.contains("History.txt reported id 0")) {
        // Extracting the 10 characters after 15th element on the line 3 in the log file
        transactionID = transactionIDContainer.substring(14, 24)
    }
    log.info("Transaction ID: " + transactionID)
    return request.post(
        url: global.radarUrl.AddSymbolTransaction,
        error: 'Failed to update build step in Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordID: orbit.buildRecordId(),
            TransactionId: transactionID,
        ]
    )
}

/**
 * Uploads the QwietAI Scan details to Radar
 * <p><b>Example Code Usage:</b></p>
 * <pre>
 * radar.uploadShiftLeftScanDetails("https://app.shiftleft.io/apps/orbit-signing-service/summary?scan=3")
 * </pre>
 * @param timestamp [T:String] The timestamp of the shiftleft scan
 * @param appId [T:String] The ID of the app for which the scan was done
 * @param scanId [T:String] The ID of the scan
 * @param location [T:String] The path of code or or artifact to scan
 * @param sourceCodeId [T:String][OPTIONAL] The SCM Source Code ID to link to this scan in Radar
 * @param branch [T:String][OPTIONAL] The branch of the repository for which the scan was done
{
    "BuildRecordId": 680191,
    "Id": 2,
    "AppName": "Test",
    "Branch": "123",
    "Timestamp": "2025-01-24T15:45:00",
    "Location": "/src",
    "SourceCodeId": 43949
}
 */
 
private def uploadShiftLeftScanDetails(String timestamp, String appId, String scanId, String location, Integer sourceCodeId = null, String branch = null) {
    global()
    return request.post(
        url: global.radarUrl.AddShiftLeftScanDetails,
        error: 'Failed to upload QwietAI Scan Link in Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordId: env.BUILD_RECORD_ID,
            AppName: appId,
            Timestamp: timestamp,
            Id: scanId,
            Branch: branch,
            Location: location,
            SourceCodeId: sourceCodeId,
        ]
    )
}

/**
 * Creates a node record in Radar.
 * @param node_label : label used in the Jenkinsfile
 * @param jobGuid : jobGuid of the node created in eportal
 * @return The ID of the node record
 */
private int createNodeRecord(String node_label, String jobGuid = null, String guestNodeLabel = null, String guestImage = null) {
    global()
    def response = request.post(
        url: global.radarUrl.nodeRecord,
        error: 'Failed to create node record in Radar.',
        token: 'orbit_radar_cred',
        json: [
            buildRecordID: orbit.buildRecordId(),
            nodeName: env.NODE_NAME,
            jobGuid: jobGuid,
            nodeLabel: node_label,
            guestNodeLabel: guestNodeLabel,
            guestImage: guestImage
        ],
    )
    if (!response.NodeId) {
        error "Failed to create build node record in Radar"
    }
    return response.NodeId
}

/**
 * Uploads AVScan status to Radar.
 *
 * @param nodeID Radar node ID for the entry to update
 * @param status Current Status of AVScan process i.e. Failed, Successful etc
 */
private def updateAvScanFlag(int nodeID, String status) {
    global()
    return request.post(
        url: global.radarUrl.AVScanStatus,
        error: 'Failed to update AVScan status to Radar.',
        token: 'orbit_radar_cred',
        json: [
            nodeID: nodeID,
            status: status,
        ],
    )
}

/**
 * Get a BOM result summary of any build.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.getBOMRecordAsJSON(12345)
 * </pre>
 * @param buildRecordId [T:int] The build record id to get the BOM for.
 * @return [T:Map] The BOM record
 */
public Map getBOMRecordAsJSON(def buildRecordId) {
    global()
    return request.get(
        url: global.radarUrl.GetBOMRecord,
        error: 'Failed to get the BOM record from Radar.',
        token: 'orbit_radar_cred',
        params: [
            buildRecordId: buildRecordId,
        ],
    )
}

/**
 * Get the id of a build system job.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.getBuildSystemJobId("Radar", "Radar_Backend", "MB_Job")
 * </pre>
 * @param productName [T:String] The product name.
 * @param componentName [T:String] The component name.
 * @param jobName [T:String] The job name.
 * @return [T:int] The job id.
 */
public int getBuildSystemJobId(String productName, String componentName, String jobName) {
    global()
    def response = request.get(
        url: global.radarUrl.GetJobId,
        error: 'Failed to get the id of the build system job from Radar.',
        token: 'orbit_radar_cred',
        params: [
            productName: productName,
            componentName: componentName,
            jobName: jobName,
        ],
    )
    return response.BuildSystemJobId.toInteger()
}

/**
 * Triggers an Orbit build.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.triggerOrbitBuild(1, "master", ["Bullseye": "true"], true)
 * </pre>
 * @param jobId [T:int] The job id to trigger.
 * @param branch [T:String] The branch to trigger in case of a multi branch pipeline job.
 * @param parameters [T:Map] The key/value parameters.
 * @param wait [T:boolean] [OPTIONAL] Wait for the build to complete. [DEFAULT:false]
 * @param timeout [T:int] [OPTIONAL] How long to wait for the build to complete in seconds. [DEFAULT:3600]
 * @return [T:Map] A map containing the <code>JobUrl</code> and <code>orbitGUID</code> keys.
 */
public Map triggerOrbitBuild(int jobId, String branch, Map parameters, boolean wait=false, int timeout=3600) {
    global()
    log.info(
        "Triggering orbit build for "+
        "Job id: ${jobId}, " +
        "Branch: ${branch}, " +
        "Parameter count: ${parameters.size()}"
    )
    def result = request.post(
        url: global.radarUrl.triggerOrbitBuild,
        error: 'Failed to trigger the requested build in orbit.',
        token: 'orbit_radar_cred',
        json: [
            Id: jobId,
            Branch: branch,
            Parameters: parameters,
        ],
    )
    //If there are any errors throw an exceptions
    if (result["JobUrl"] != null) {
        log.info("Triggered job " + result["JobUrl"])
    } else {
        throw new RuntimeException(result["Error"])
    }

    if (wait) {
        log.info("Waiting for job to complete. orbitGUID " + result["orbitGUID"])
        log.info("Setting timeout to " + timeout + " seconds.")
        String jobStatus = "Not started"
        //Set the timeout
        steps.timeout(time: timeout, unit: 'SECONDS') {
            while(jobStatus != "SUCCESS") {
                //Fetch the build record by guid
                def buildRecord = this.getBuildRecordByGUID(result["orbitGUID"])
                if (StringUtils.isNullOrEmpty(buildRecord)) {
                    log.info("Couldn't fetch the build from Radar yet. Possible cause is that the build is still to be created.")
                    sleep 60
                    continue
                }
                //If BuildResult is not present yet, then possibly the build record is not created yet
                if(buildRecord.BuildResult) {
                    if(buildRecord.BuildResult != 'SUCCESS' && buildRecord.BuildResult != 'IN_PROGRESS') {
                        throw new RuntimeException("The triggered build with GUID " + result["orbitGUID"] + 
                            " did not complete successfully. The build result is " + buildRecord.BuildResult) 
                    }
                    jobStatus = buildRecord.BuildResult
                }
                if(jobStatus != 'SUCCESS') {
                    log.info("Still waiting on the build with GUID " + result["orbitGUID"] + " to complete ... " + 
                        " Current job status is \"${jobStatus}\"" + 
                        (buildRecord.BuildURL != null ? " and the build is found at url " + buildRecord.BuildURL : ""))
                    sleep 60
                }
                else {
                    log.info("The triggered build with GUID " + result["orbitGUID"] + " completed successfully. " + buildRecord.BuildURL) 
                }
            }
        }
    }
    return result
}

/**
 * Uploads Bullseye results to Radar.
 *
 * @param percentFunctionsCovered Percent of functions covered
 * @param percentDecisionsCovered Percent of decisions covered
 * */
private def setBullseyeResults(int percentFunctionsCovered, int percentDecisionsCovered, String version='8', String coverageType="Bullseye") {
    global()
    return request.post(
        url: global.radarUrl.CodeCoverageResults,
        error: 'Failed to upload Bullseye results to Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordID: orbit.buildRecordId(),
            CoverageTool: "Bullseye",
            CoverageToolVersion: version,
            CoverageType: coverageType,
            DecisionCoverage: percentDecisionsCovered,
            FunctionCoverage: percentFunctionsCovered,
            UncoveredDecision: (100-percentDecisionsCovered),
            UncoveredFunctions: (100-percentFunctionsCovered),
        ],
    )
}

/**
 * Add build signing in Radar.
 *
 * <p>The status displayed in Radar will be associated with the current job stage.</p>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.addBuildSigning(
 *     CertificateIdentity: "1a2b3c4d5e",
 *     Tool: "SignTool",
 *     Status: "Success",
 *     Message: "None",
 *     TimestampServer: "http://timestamp_example.com",
 *     FileName: "filetosign.exe",
 * )
 * </pre>
 * @param CertificateIdentity [T:String] The signing Inbox name used to copy files to.
 * @param Tool [T:String] Type of server signing used.
 * @param Status [T:String] If the signing is a <code>"Success"</code> or <code>"Failure"</code>.
 * @param Message [T:String] [OPTIONAL] Message to explain reason if signing failed.
 * @param TimestampServer [T:String] [OPTIONAL] Timestamp of the server that was used for the binary.
 * @param FileName [T:String] The name of the file that is signed.
 */
public def addBuildSigningInRadar(Map payload) {
    global()
    return request.post(
        url: global.radarUrl.AddBuildSigning,
        error: 'Failed to add build signing record to Radar.',
        token: 'orbit_radar_cred',
        json: payload + [
            buildRecordID: orbit.buildRecordId(),
            BuildNode: utils.getBuildNodeID(),
        ],
    )
}

/**
 * Publishes a log entry to Radar.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.addBuildLog(
 *     'Error',
 *     'Upload',
 *     'Compile',
 *     'Artifactory',
 *     'The remote server responded with HTTP code 500',
 * )
 * </pre>
 * @param level The log level.
 * @param type The log entry type.
 * @param stage The pipeline stage being executed.
 * @param service The service being executed.
 * @param message The log message.
 */
private def addBuildLog(def level, def type, def stage, def service, def message) {
    global()
    return request.post(
        url: global.radarUrl.AddBuildLog,
        error: 'Failed to publish the build log to Radar.',
        token: 'orbit_radar_cred',
        fail: false,
        json: [
            buildRecordID: orbit.buildRecordId(),
            messages: [[
                Level: level,
                Type: type,
                Stage: stage,
                Service: service,
                Message: message,
            ]],
        ],
    )
}

/**
 * Creates a record of an Artifactory dependency in Radar.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.createArtifactoryDependency(
 *    "trellix",
 *    "path/to/files",
 *    ["file1.txt", "file2.txt"],
 * )
 * </pre>
 */
private def createArtifactoryDependency(String instanceName, String root, ArrayList<String> files) {
    global()
    return request.post(
        url: global.radarUrl.ArtifactoryDependency,
        error: 'Failed to publish the artifactory dependency to Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordId: orbit.buildRecordId(),
            ArtifactoryInstanceName: instanceName,
            Root: root,
            Files: files,
        ],
    )
}

/**
 * Creates a record of Artifacts for a build in Radar.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.postArtifacts([
 *    {
 *      "Path" : "/path/file1.txt",
 *      "CheckSum" : "generated_file1_checksum"
 *    },
 *    {
 *      "Path" : "/path/file2.txt",
 *      "CheckSum" : "generated_file2_checksum"
 *    }
 * ])
 * </pre>
 */
public def postArtifacts(def filesInformation) {
    global()
    return request.post(
        url: global.radarUrl.Artifacts,
        error: 'Failed to post the Artifacts data to Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordId: orbit.buildRecordId(),
            Files: filesInformation,
        ],
    )
}

/**
 * Get a build record by GUID.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.getBuildRecordByGUID("1234-1234-1234-1234")
 * </pre>
 * @param guid [T:String] The build record GUID.
 * @return [T:Map] The build record.
 */
public Map getBuildRecordByGUID(String guid) {
    global()
    return request.get(
        url: global.radarUrl.GetBuildByGuid,
        error: 'Failed to fetch the build record by guid from Radar.',
        token: 'orbit_radar_cred',
        fail: false,
        params: [
            guid: guid,
        ],
    )
}
/**
 * Get an Artifactory Instance by name.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.getArtifactoryInstanceByName("TRELLIX_PROD")
 * </pre>
 * @param name [T:String] The name of the Artifactory instance.
 * @return [T:Map] The Artifactory instance object.
 */
public Map getArtifactoryInstanceByName(String name) {
    global()
    return request.get(
        url: global.radarUrl.GetArtifactoryInstanceByName,
        error: 'Failed to get the artifactory instance record from Radar.',
        token: 'orbit_radar_cred',
        params: [
            name: name,
        ],
    )
}
/**
 * Get all Github instances.
 *
 * @param radarUrl [T:String] Radar Github instances URL
 * @return [T:List] The list of GitHub instances registered in Radar.
 */
private List getGithubInstances(String radarUrl) {
    return request.get(
        url: radarUrl,
        error: 'Failed to get GitHub instances.',
        token: 'orbit_radar_cred',
        master: true,
    )
}
/**
 * Set Protex Results in Radar.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.setProtexResults(123,2,0,0,2,"Test","https://protex.corp.entsec.com","repository",1,1,1,0,0)
 * </pre>
 * @param buildRecordId [T:int] The build record id.
 * @param filesAnalyzed [T:int] The number of files analyzed.
 * @param filesSkipped [T:int] The number of skipped files.
 * @param pendingID [T:int] The pending id.
 * @param noDiscoveries [T:int] The number of no discoveries.
 * @param projectId [T:String] The project Id.
 * @param url [T:String] The Protex server url.
 * @param repoScanned [T:String] The scanned SCM repository.
 * @param discoveredComponents [T:int] The number of discovered components.
 * @param totalComponents [T:int] The total number of components.
 * @param totalLicenses [T:int] The total number of licenses.
 * @param pendingReview [T:int] The number of pending reviews.
 * @param licenseViolations [T:int] The number of license violations.
 */
public def setProtexResults(
    def buildRecordId,
    def filesAnalyzed,
    def filesSkipped,
    def pendingID, 
    def noDiscoveries,
    def projectId,
    def url,
    def repoScanned,
    def discoveredComponents,
    def totalComponents,
    def totalLicenses,
    def pendingReview,
    def licenseViolations
) {
    global()
    return request.post(
        url: global.radarUrl.AddProtexResult,
        error: 'Failed to upload Protex results to Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordId: StringUtils.asInteger(buildRecordId),
            FilesAnalyzed: StringUtils.asInteger(filesAnalyzed),
            FilesSkipped: StringUtils.asInteger(filesSkipped),
            PendingID: StringUtils.asInteger(pendingID),
            NoDiscoveries: StringUtils.asInteger(noDiscoveries),
            ProjectID: projectId?.toString(),
            URL: url?.toString(),
            RepoScanned: repoScanned?.toString(),
            DiscoveredComponents: StringUtils.asInteger(discoveredComponents),
            TotalComponents: StringUtils.asInteger(totalComponents),
            TotalLicenses: StringUtils.asInteger(totalLicenses),
            PendingReview: StringUtils.asInteger(pendingReview),
            LicenseViolations: StringUtils.asInteger(licenseViolations),
        ]
    )
}
/**
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.getProductByShortName("Radar")
 * </pre>
 * @param productShortName [T:String] The product short name.
 * @return [T:Map] The product object.
 */
public Map getProductByShortName(String productShortName) {
    global()
    return request.get(
        url: global.radarUrl.GetProductByShortName,
        error: 'Failed to get the product from Radar.',
        token: 'orbit_radar_cred',
        params: [
            shortName: productShortName,
        ],
        master: true
    )
}


/**
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.getSignType("sha2")
 * </pre>
 * @param signType [T:String] The sign type to fetch details for.
 * @return [T:Map] The sign type object.
 */
public Map getSignType(String signType) {
    global()
    return request.get(
        url: global.radarUrl.GetSignType,
        error: 'Failed to get the sign type details from Radar.',
        token: 'orbit_radar_cred',
        params: [
            Name: signType,
        ],
    )
}

/**
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.getSigningCertificate("Musarubra")
 * </pre>
 * @param name [T:String] The certificate to fetch details for.
 * @return [T:Map] The certificate object.
 */
public Map getSigningCertificate(String name) {
    global()
    return request.get(
        url: global.radarUrl.GetSigningCertificate,
        error: 'Failed to get the certificate details from Radar.',
        token: 'orbit_radar_cred',
        params: [
            Name: name,
        ],
    )
}

/**
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.getSigningService("sha2")
 * </pre>
 * @param signType [T:String] The sign type to fetch details for.
 * @return [T:Map] The sign type object.
 */
public Map getSigningService(String signType) {
    global()
    return request.get(
        url: global.radarUrl.GetSigningService,
        error: 'Failed to get the signing service details from Radar.',
        token: 'orbit_radar_cred',
        params: [
            Type: signType,
        ],
    )
}

/**
 * <h4>Sample usage:</h4>
 * <pre>
 * radar.getSigningConfiguration(signType="sha2", config="CURRENT")
 * </pre>
 * @param signType [T:String] The sign type of the configuration to fetch
 * @param config [T:String] The config name
 * @param customCert [T:String] [OPTIONAL] The custom certificate associated with the configuration
 * @return [T:Map] The configuration object.
 */
public Map getSigningConfiguration(String signType, String config, String customCert=null) {
    global()
    def params = [
        Type: signType,
        Config: config
    ]
    if(customCert) {
        params.CustomCert = customCert
    }
    return request.get(
        url: global.radarUrl.GetSigningConfiguration,
        error: 'Failed to get the signing configuration details from Radar.',
        token: 'orbit_radar_cred',
        params: params,
    )
}
/**
 * Sets a note on the build
 *
 * @param note The note to set on the Build
 * */
public def setNote(String note) {
    global()
    return request.post(
        url: global.radarUrl.SetNote,
        error: 'Failed to set the note in Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordId: orbit.buildRecordId(),
            Note: note,
        ],
    )
}
