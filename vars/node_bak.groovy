import hudson.AbortException
import groovy.json.JsonOutput
import com.mcafee.orbit.Utils.EmailUtils

/**
 * Provision a build node for your pipeline.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * node("w2k16_orbit") {
 *     // Run job on the "w2k16_orbit" build node
 * }
 * </pre>
 * <pre>
 * node("w2k16_orbit", [cpus:8, memory:16, lfsRepo:'FileMaster-local']) {
 *     // Run job on the "w2k16_orbit" build node with specified number of CPUs and RAM.
 *     // Use the FileMaster-local as the lfs repository from artifactory
 *     // when checking out the Jenkinsfile from the GitHub repository.
 * }
 * </pre>
 *
 * @param label [T:String] The Orbit node label configured in Jenkins.
 * @param options [T:Map] Specify the cpu and memory values that the machine will be provisioned with.
 *                  Allows the over-ride of the lfs repository via the <code>lfsRepo</code> key
 *                  when checking out the Jenkinsfile from GitHub.
*                   Specify whether to run a cloud label on Kubernetes or a single dockerhost using <code>runOnDockerhost</code>.
 *                  Defaults to radar configuration values <code>CDA_DEFAULT_CPU</code> and <code>CDA_DEFAULT_MEMORY</code>.
 */
void call(String label, def options = [:], Closure body) {
    config.init()
    label = mapLabel(label)
    env.current_stage = 'node'
    env.current_task = 'initial'
    def lfsRepo = options.remove('lfsRepo')
    def runOnDockerhost = options.remove('runOnDockerhost')
    String guestLabel = label
    Map cloudTemplateDetails = utils.getCloudTemplateDetails(guestLabel)
    String guestNodeLabel = null
    String guestImage = null
    timeout([time: 1, unit: 'DAYS']) {
        def nps = null
        if (!env.DEBUGLEVEL) {
            env.DEBUGLEVEL = "1"
        }
        try {
            //Orbit requires a node from ePortal if the label is not cloud configured
            //or when the label is cloud configured with the run on dockerhost flag turned on
            if (!cloudTemplateDetails || (cloudTemplateDetails && runOnDockerhost )) {
                label = (cloudTemplateDetails && runOnDockerhost) ? 
                                        config("DOCKERHOST_"+cloudTemplateDetails["NodeSelector"]) : label
                // Label is not configured in kubernetes, use NPS for provisioning
                steps.node('pypeline') {
                    container('build-container') {
                        def OsJavaPathMapForJenkins = config.group('JAVA_BIN_LOCATIONS_FOR_JENKINS_CONNECTOR')
                        if (label.startsWith('w')) {
                            nps = pypeline.provisionNode(label, "Windows", options)
                            agent.CreateNode(nps.NodeName, 'E:\\', OsJavaPathMapForJenkins['JAVA_BIN_WINDOWS'])
                            pypeline.connectNode(nps.NodeName, nps.IpAddress)
                        } else if (label.startsWith('osxarm')) {
                            nps = pypeline.provisionNode(label, "OSXARM", options)
                            agent.CreateNode(nps.NodeName, '/Users/orbit', OsJavaPathMapForJenkins['JAVA_BIN_OSX'], nps.IpAddress)
                        } else if (label.startsWith('osx')) {
                            nps = pypeline.provisionNode(label, "OSX", options)
                            agent.CreateNode(nps.NodeName, '/Users/orbit', OsJavaPathMapForJenkins['JAVA_BIN_OSX'], nps.IpAddress)
                        } else {
                            nps = pypeline.provisionNode(label, "Linux", options)
                            agent.CreateNode(nps.NodeName, '/home/orbit', OsJavaPathMapForJenkins['JAVA_BIN_LINUX'], nps.IpAddress)
                        }
                    }
                }
            }
            steps.node(nps ? nps.NodeName : label) {
                def insideNode = {
                    // ORPL-364, PDSBUIL-2285: Fix support for long paths
                    execute('git config --global core.longpaths true')
                    //Setting the node label for parallel calls to reuse. Retrieved via: utils.getNodeVar('REUSE_LABEL')
                    withEnv(["REUSE_LABEL=${label}"]) {
                        log.info("Running on node: ${env.NODE_NAME}")
                        wrap([$class: 'TimestamperBuildWrapper']) {
                            wrap([$class: 'AnsiColorBuildWrapper']) {
                                def nodeId = radar.createNodeRecord(label, nps?.JobGuid, guestNodeLabel, guestImage)
                                orbit.buildNode(env.NODE_NAME, nodeId)
                                // If a new record was created archive build_record_id.txt
                                if (it) {
                                    utils.mapReporter(env.getEnvironment())
                                    writeFile(file: 'build_record_id.txt', text: orbit.buildRecordId().toString())
                                    archiveArtifacts(artifacts: 'build_record_id.txt')
                                }
                                //Store CommitSHA of current 
                                boolean inline_script = jenkins.isInlineScript()
                                if (!orbit.buildSha() && !inline_script && !jenkins.isReplay(currentBuild)) {
                                    utils.checkoutBuildSHA("OrbitTempCheckoutDirectory", "Build", lfsRepo)
                                }
                                //Install orbit-pypeline/Check if orbit-pypeline works on the node
                                pypeline.getVersion(false)
                                log.info("Orbit pypeline has been successfully installed on the node.")
                                body.call()
                            }
                        }
                    }
                }
                if (agent.isKubernetes(env.NODE_NAME)) {
                    container('build-container') {
                        decorator(insideNode)
                    }
                }
                else {
                    if(cloudTemplateDetails && runOnDockerhost ) {
                        guestNodeLabel = guestLabel
                        guestImage = cloudTemplateDetails['Image']
                        docker.image(
                            cloudTemplateDetails['Image'])
                            //Map orbit pypeline
                            .withRun('-v /usr/local/orbit/pypeline:/usr/local/orbit/pypeline:rw,z'){
                            log.info("Running on " + cloudTemplateDetails['Image'])
                            decorator(insideNode)
                        }
                    }
                    else {
                        decorator(insideNode)
                    }
                    
                }
            }
        } catch (Throwable e) {
            log.info("Build failed")
            log.error("Error : ${e.message}")
            log.error("Stack trace: ${e.getStackTrace().join('\n')}")
            rethrow(e)
        } finally {
            log.debug("Finally block of provision node")
            if (nps) {
                log.info("CDA Node clean up actions")
                boolean keepNode = false
                def splitFullJobName = env.JOB_NAME.split('/')
                //Check if node will be kept after a failed build
                if (currentBuild.result == "FAILURE" && env.KeepFailedNode == "true" &&
                    !radar.getProductByShortName(splitFullJobName[0].replace("_", " "))["CanKeepNodeOnFailure"]) {
                    log.info("The KeepFailedNode flag is set to true but the node will be destroyed as this product cannot keep nodes")
                }
                else if(currentBuild.result == "FAILURE" && env.KeepFailedNode == "true"){
                    keepNode = true
                    log.info("The build failed but the node will not be destroyed as KeepFailedNode flag is set to true")
                }
                if(!keepNode) {
                    log.info("Freeing the lease in NPS and removing build node from Jenkins.")
                    pypeline.releaseNode(nps.JobGuid)
                    agent.RemoveNode(nps.NodeName)
                }
            }
        }
    }
}

/**
 * Test if the current node is a mac node
 */
private boolean isMac() {
    if(this.getOS() ==~ /(?i)darwin.*/) {
        return true
    }
    return false
}
/**
 * This function returns the os on which the build is running
 * Currently returns
 * darwin  for osx
 * linux for CentOS, MLOS
 * windows for non-unix
 * @return String Name of the OS
 */
private String getOS() {
    String osName = "windows"
    if(isUnix()) {
        /*
            Tries to identify the OS through uname
            uname returns "linux" for most distros of linux
            If we find the need to identify a specific distro, we can consider expanding this method
            to use lsb_release or something similar to identify the distro incase of linux.
         */
        try {
            osName = sh(returnStdout: true, script: 'uname -s').trim()
        } catch (Throwable err) {
            String msg = err.toString()
            utils.getErrorMessage("ERROR: Could not determine OS information through uname  ${msg}")
        }
    }
    return osName.toLowerCase()
}

/**
 * This function returns the Mac os version on which the build is running
 * Currently returns
 * darwin version for osx
 * @return String Name of the OS
 */

private String getMacOSVer() {
    String osVer = ""

    if(this.isMac()) {
        try {
            osVer = sh(returnStdout: true, script: 'sw_vers -productVersion').trim()
            log.debug("Mac OS version: " + osVer)
        } catch (Throwable err) {
            String msg = err.toString()
            utils.getErrorMessage("ERROR: Could not determine Mac OS information,  ${msg}")
        }
    } else {
        log.debug("Warning: Get OS version is only supported for Mac OS")
    }
    return osVer.toLowerCase()
}

/**
 * Mapping old VM template names to those available in NPS.
 *
 * @param label - Template name to check
 * @return The mapped label
 */
private String mapLabel(String label) {
    def templates = config.group('CDA_TEMPLATES')
    def replaced = label.replaceAll('_', '-')
    if (templates[replaced]) {
        return templates[replaced]
    } else {
        return label
    }
}

/**
 * This decorator wraps the build node related code and handles
 * everything related to creation and finalization of the build.
 *
 * @param body The closure to wrap
 */
private def decorator(Closure body) {
    def status = 'started'
    def newRecord = false
    try {
        if (!orbit.buildRecordId()) {
            // Create the build record in Radar
            radar()
            newRecord = true
            radar.updateBuildMileStoneInRadar(
                buildMilestone: 'Build started',
                username: 'svcacct-orbit-build',
                comment: 'Build has started'
            )
            // Send email notification
            notify.notifyBuild(status)
            // Record the cause chain
            if (env.CAUSECHAIN_ENABLED) {
                env.CAUSECHAIN = jenkins.getCauseChain()
            } else {
                env.CAUSECHAIN = "Cause chain disabled. To enable, set env.CAUSECHAIN_ENABLED = 'true'."
            }
            // Make some spaghetti
            utils.defineBuildParameters()
        }
        // Execute user code
        body.call(newRecord)
        // Check if user code executed successfully
        if (currentBuild.result == 'FAILURE') {
            status = 'failed'
        } else {
            status = 'passed'
        }
    } catch (Throwable e) {
        status = 'failed'
        rethrow(e)
    } finally {
        if (newRecord) { // Only execute for the top-most call to `node()`
            radar.updateBuildMileStoneInRadar(
                buildMilestone: "BUILD $status",
                username: 'svcacct-orbit-build',
                comment: "Build has $status",
            )
            if (jenkins.isReplay(currentBuild)) {
                radar.updateBuildMileStoneInRadar(
                    buildMilestone: 'BUILD IS A THROW-AWAY',
                    username: 'svcacct-orbit-build',
                    comment: 'Build is a throwaway as it is a replayed build',
                )
            }
            // Send email notification
            notify.notifyBuild(status)
            // Update result for enitre build
            radar.updateBuildResultInRadar(
                BuildResult: status == 'passed' ? 'SUCCESS' : 'FAILED'
            )
            // Upload console logs to artifactory
            radar.markBuildComplete()
        }
    }
}
