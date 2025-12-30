import com.mcafee.orbit.Utils.StringUtils
import com.mcafee.orbit.Utils.PathUtils
import com.mcafee.orbit.Deprecated

/**
 * Run an AV Scan on the specified folder(s).
 * Does not support wildcards.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * antivirus(['path/to/location/to/scan', 'path/to/another'])
 * </pre>
 * <p>or</p>
 * <pre>
 * antivirus(['path/to/location/to/scan'], 123)
 * </pre>
 *
 * @param locations [T:List&lt;String&gt;] The paths to run the scan on
 * @param nodeID [T:Integer] The node ID, used to update the AV status in Radar
 */
void call(List<String> locations, Integer nodeID = null) {
    if (env.DISABLEAVSCAN != null ? env.DISABLEAVSCAN.toBoolean() : false) {
        return
    }
    String logLocation = StringUtils.getPaddedRandomNumber() + '.log'
    try {
        def exitCode = pypeline.runAvScan(locations, logLocation)
        artifacts.upload([
            files: logLocation,
            skipScan: true
        ])
        if (exitCode == 42) {
            String log = readFile(logLocation)
            positiveResult(log, nodeID)
        } else if (exitCode != 0) {
            throw new RuntimeException(
                'AV scan returned non-zero exit code'
            )
        }
        if (isUnix()) {
            sh('rm ' + logLocation)
        } else {
            bat('del ' + logLocation)
        }
    } catch(Throwable e) {
        log.error('Failed to execute AV Scan')
        rethrow(e)
    }
}

/**
 * Run an AV Scan on the specified folder(s).
 * Supports wildcards.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * antivirus()
 * </pre>
 * <p>or</p>
 * <pre>
 * antivirus('path/to/location/to/scan/**')
 * </pre>
 * <p>or</p>
 * <pre>
 * antivirus('path/to/location/to/scan', 123)
 * </pre>
 *
 * @param locations [T:String] The path to run the scan on
 * @param nodeID [T:Integer] [OPTIONAL] The node ID, used to update the AV status in Radar. [DEFAULT:null]
 */
@Deprecated(["antivirus(List&lt;String&gt;&comma;&nbsp;Integer)"])
void call(String locations, Integer nodeID = null) {
    deprecated()
    List<String> list
    try {
        list = PathUtils.expandFileList(locations)
    } catch (Throwable e) {
        log.error('Failed to execute AV Scan')
        rethrow(e)
    }
    call(list, nodeID)
}

/**
 * Executes the AV scan for the workspace of a build node.
 *
 * @param nodeID [T:int] The ID of the node being scanned.
 * @param jobGuid [T:String] [OPTIONAL] The job GUID on CDA to free the lease.
 */
void scanWorkspace(int nodeID, String jobGuid = "") {
    if (config('DISABLEAVSCAN').toBoolean()) {
        return
    }
    timeout(time: 5, unit: 'HOURS') {
        log.info("Running AVScan of the workspace")
        try {
                def info = artifacts.getArtifactoryInfo()
                pypeline.runAVScanWorkspace(
                    nodeID,
                    info.artifactoryURL,
                    jobGuid
                )
        } catch (Throwable e) {
            radar.updateBuildMileStoneInRadar([
                buildMilestone: "BUILD IS A THROW-AWAY",
                username: "svcacct-orbit-build",
                comment: "Build is a throwaway because AV Scan failed to execute."
            ])
            rethrow(e, 'Failed to execute AV scan', true)
        }
    }
}

/**
 * Run an AV Scan on a specified folder.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * antivirus.avScanLocation(location: env.WORKSPACE)
 * </pre>
 *
 * @param location [T:String] The path to run the scan on.
 */
@Deprecated(["antivirus(List&lt;String&gt;&comma;&nbsp;Integer)"])
void avScanLocation(arg) {
    deprecated()
    assert arg != null && arg.location != null : 'Invalid invocation of antivirus.avScanLocation'
    call(arg.location.toString())
}

/**
 * Returns the body for an email notifying about a positive detection.
 *
 * @param details [T:String] The logs from the scanner
 * @return [T:String] The email body
 */
@NonCPS
private def getEmailBody(String details) {
    return """
<!DOCTYPE html>
<html>
  <head>
    <title>!!! ORBIT AVSCAN POSITIVE DETECTION !!!</title>
    <style type="text/css">
    body {
      font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
    }
    </style>
  </head>
  <body>
    <h1>!!! ORBIT AVSCAN POSITIVE DETECTION !!!</h1>
    <p>Job: ${env.JOB_NAME}</p>
    <p>Orbit Build Number: ${orbit.buildNumber()}</p>
    <p>
      Av scan log:
      <pre>$details</pre>
    </p>
  </body>
</html>
    """.toString()
}

/**
 * Executes in case of a detection.
 *
 * @param details The content to be sent in the email.
 * @param nodeID The node ID, used to update the AV status in Radar.
 */
private void positiveResult(
    String details,
    Integer nodeID
) {
    String subject = "!!! ORBIT AVSCAN POSITIVE DETECTION !!!"
    if (nodeID) {
        radar.updateAvScanFlag(nodeID, "Positive")
    } else {
        log.error('Failed to update scan status in Radar. Node ID not set.')
    }
    radar.updateBuildMileStoneInRadar([
        buildMilestone: "BUILD IS A THROW-AWAY",
        username: "svcacct-orbit-build",
        comment: "Build is a throwaway because AV Scan failed to execute."
    ])
    emailext(
        attachLog: true,
        body: getEmailBody(details),
        compressLog: true,
        mimeType: 'text/html',
        replyTo: 'orbit.noreply@trellix.com',
        subject: subject,
        to: config('ORBIT_CORE_TEAM_EMAIL'),
    )
    // Fail build
    currentBuild.result = 'FAILURE'
    // Bye bye 👋
    Jenkins.instance.doExit(null, null)
}
