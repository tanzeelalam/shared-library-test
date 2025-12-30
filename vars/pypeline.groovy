import groovy.json.JsonOutput
import com.mcafee.orbit.Utils.StringUtils
import com.mcafee.orbit.Credentials.CredentialStore

/**
 * Runs scan workspace command using orbit-pypeline
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * pypeline.runAVScanWorkspace(
 *     123,
 *     "https://artifactory-lvs.corpzone.internalzone.com/artifactory/isecg-test-appbuild-local/Radar_Backend_1.0.0/b339/",
 *     "5b1cce89-0da7-49cf-b6e7-872adcbe16f6"
 * )
 * </pre>
 *
 * @param nodeID int - The node id in Radar
 * @param repositoryFullName [T:String] The artifactory repository to store the av scan log
 * @param jobGuid [T:String] The job GUID on CDA to free the lease
 */
@Deprecated
private void runAVScanWorkspace(int nodeID, String repositoryFullName, String jobGuid = "") {
    String logLocation = "avscan" + StringUtils.getPaddedRandomNumber() + '.log'
    def artifactoryInstance = radar.getArtifactoryInstanceByName(env.ARTIFACTORY_INSTANCE)
    withCredentials([
        usernamePassword(
            credentialsId: CredentialStore.CREDENTIAL_FOR_RADAR_TOKEN_AUTHENTICATION,
            usernameVariable: "UNUSED",
            passwordVariable: "RADAR_TOKEN",
        ),
        usernamePassword(
            credentialsId: artifactoryInstance["CredentialId"],
            usernameVariable: "ARTIFACTORY_USERNAME",
            passwordVariable: "ARTIFACTORY_PASSWORD",
        ),
        usernamePassword(
            credentialsId: CredentialStore.CREDENTIAL_FOR_CDA_AUTHENTICATION,
            usernameVariable: "CDA_USERNAME",
            passwordVariable: "CDA_PASSWORD",
        )
    ]) {
        def command = [
            "avscan",
            "-E", config('RADAR_ENVIRONMENT'),
            "scan_workspace",
            "-i", nodeID,
            "-w", env.WORKSPACE,
            "-o", logLocation,
            "-a", repositoryFullName,
        ]
        command = withOptions(command, "-j", jobGuid)
        command = withOptions(command, "-k", env.KeepFailedNode)
        if (agent.isKubernetes(env.NODE_NAME)) {
            executeOnNode(command, 'status')
        } else {
            executeAsync(command)
        }
    }
}

/**
 * Runs av scan command using orbit-pypeline
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * pypeline.runAvScan(
 *     ["path/to/some/folder", "somefile.exe"],
 *     "https://artifactory-lvs.corpzone.internalzone.com/artifactory/isecg-test-appbuild-local/Radar_Backend_1.0.0/b339/",
 * )
 * </pre>
 *
 * @param locations String[] - The locations to scan
 * @param logLocation [T:String] The location to write the log to
 */
private def runAvScan(def locations, def logLocation) {
    withCredentials([
        usernamePassword(
            credentialsId: CredentialStore.CREDENTIAL_FOR_RADAR_TOKEN_AUTHENTICATION,
            usernameVariable: "UNUSED",
            passwordVariable: "RADAR_TOKEN",
        ),
    ]) {
        def command = [
            "avscan",
            "-E", config('RADAR_ENVIRONMENT'),
            "scan_location",
            "-o", logLocation,
        ]
        command = withOptions(command, "-l", locations)
        return executeOnNode(command, 'status')
    }
}

/**
 * Gets the pypeline version running on the Jenkins server
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * pypeline.getVersion(false)
 * </pre>
 * @param master [T:boolean] [OPTIONAL] Whether to execute on the Jenkins master node.
 * @return String the orbit pypeline version
 */
private String getVersion(boolean master=true) {
    if(master) {
        return executeOnJenkinsServer("version")
    }
    return executeOnNode(["version"], "stdout")
    
}
/**
 * Provisions an NPS node for Orbit.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * pypeline.provisionNode("w2k8-latest", 12345, "Windows", [:])
 * </pre>
 * @param label The label to look for in ePortal
 * @param osType The type of OS: 'Windows', 'Linux' or 'OSX'
 * @param machineAttributes The cpu and memory configuration for the machine
 * @return Map with the ip address, node name and nps job guid of the provisioned machine
 */
private def provisionNode(String label, String osType, def machineAttributes) {
    def product = env.JOB_NAME.split("/")[0]
    def user
    withCredentials([
        usernamePassword(
            credentialsId: CredentialStore.CREDENTIAL_FOR_CDA_AUTHENTICATION,
            usernameVariable: "CDA_USERNAME",
            passwordVariable: "CDA_PASSWORD",
        ),
        usernamePassword(
            credentialsId: CredentialStore.CREDENTIAL_FOR_RADAR_TOKEN_AUTHENTICATION,
            usernameVariable: "UNUSED",
            passwordVariable: "RADAR_TOKEN",
        ),
    ]) {
        try {
            user = currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
        } catch (Throwable e) {
            log.warn("Could not find the user info, and it will not be passed to ePortal in relation to cost center details.")
        }
        def command = [
            "nps",
            "-E", config('RADAR_ENVIRONMENT'),
            "provision-node",
            "--label", label,
            "--product-name", product,
            "--vsphere-folder", config('VSPHERE_FOLDER'),
            "--os", osType,
        ]
        command = withOptions(command, "--cpu", machineAttributes["cpus"])
        command = withOptions(command, "--memory", machineAttributes["memory"])
        command = withOptions(command, "--build-system-login", user)
        withCredentials(
            [
                string(
                    credentialsId: CredentialStore.ORBIT_NODE_PUBLIC_SSH, 
                    variable: 'ORBIT_NODE_PUBLIC_SSH'
                )
            ]
        ) {
            def stdout = executeOnNode(command, 'stdout')
            log.debug("Provision node output : ${stdout}")
            return readJSON(text: stdout)
        }
    }
}
/**
 * Connects a windows build node to the Jenkins master server
*
 * @param name The name of the node to connect
 * @param ip The ip address of the node to connect
 */
private void connectNode(def name, def ip) {
    executeOnNode([
        "nps",
        "-E", config('RADAR_ENVIRONMENT'),
        "connect-node",
        "--node-name", name,
        "--ip-address", ip,
        "--jnlp-secret", agent.GetJnlpSecretForNode(name),
        "--jenkins-url", env.JENKINS_URL,
    ])
}
/**
 * Frees a node lease in NPS.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * pypeline.releaseNode("SASD-1234-ZYXF-ASD1")
 * </pre>
 * @param jobGuid The job guid of the previously provisioned machine
 */
private void releaseNode(def jobGuid) {
    withCredentials([
        usernamePassword(
            credentialsId: CredentialStore.CREDENTIAL_FOR_CDA_AUTHENTICATION,
            usernameVariable: "CDA_USERNAME",
            passwordVariable: "CDA_PASSWORD",
        ),
        usernamePassword(
            credentialsId: CredentialStore.CREDENTIAL_FOR_RADAR_TOKEN_AUTHENTICATION,
            usernameVariable: "UNUSED",
            passwordVariable: "RADAR_TOKEN",
        ),
    ]) {
        def stdout = executeOnJenkinsServer(
            [
                "nps",
                "-E", config('RADAR_ENVIRONMENT'),
                "release-node",
                "--job-guid", jobGuid,
            ],
            [
                "CDA_PASSWORD": env.CDA_PASSWORD,
                "CDA_USERNAME": env.CDA_USERNAME,
                "RADAR_TOKEN": env.RADAR_TOKEN,
            ],
        )
        if (stdout) {
            println(stdout)
        }
    }
}
/**
 * Runs async command for orbit-pypeline on the current node
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * pypeline.executeAsync(
 *     "scan_workspace -i 123 -o avscan.log -w E:\workspace"
 * )
 * </pre>
 *
 * @param command [T:String] The orbit-pypeline command to execute
 */
private void executeAsync(String command) {
    if (isUnix()) {
        def wrapper = config('UNIX_ORBIT_PYPELINE_LOCATION')
        sh "nohup ${wrapper} ${command} &"
    } else {
        def wrapper = config('WINDOWS_ORBIT_PYPELINE_LOCATION')
        bat "start call ${wrapper} ${command}"
    }
}
/**
 * Runs an orbit-pypeline command on the Jenkins server
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * String stdout = pypeline.executeOnJenkinsServer(
 *     "version"
 * )
 * </pre>
 *
 * @param command List<String> - The orbit-pypeline command to execute
 * @param environment Map<String, String> - The environment variables to set
 * @param workingDirectory The working directory where to run the command. Defaults to '/'
 * @param stdin [T:String] Data to write STDIN of the process
 * @return The stdout
 */
private String executeOnJenkinsServer(List<String> command, Map environment = [:], File workingDirectory = null, String stdin = null) {
    environment.put("ORBIT_PYPELINE_VERSION", env.ORBIT_PYPELINE_VERSION)
    environment.put("http_proxy", "http://proxy.ess.gslb.entsec.com:9090")
    environment.put("https_proxy", "http://proxy.ess.gslb.entsec.com:9090")
    environment.put("no_proxy", "localhost,127.0.0.1,.beaeng.mfeeng.org,.corp.nai.org,.internalzone.com,.corp.mcafee.com,.corp.entsec.com,.trellix.com")
    log.info("Executing orbit pypeline: ${command}")
    // prepend the system log level to the list of arguments
    command = ["/usr/local/orbit/pypeline/orbit", "-L", log.systemLevel] + command
    def result = jenkins_server.execute_command(
        command.collect { it.toString() },
        environment,
        workingDirectory,
        stdin,
    )
    println(result["stderr"])
    if (result["exitCode"] != 0) {
        throw new RuntimeException("Orbit-pypeline returned non-zero exit code: ${result["exitCode"]}")
    }
    return result["stdout"]
}
/**
 * Runs command for orbit-pypeline on the current node
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * pypeline.executeOnNode([
 *     "scan_workspace",
 *     "-i", "123",
 *     "-o", "avscan.log",
 *     "-w", "E:\workspace",
 * ])
 * </pre>
 *
 * @param command List - The orbit-pypeline command to execute
 * @param return [T:String] What to return: nothing, the exit code or the stdout
 */
private def executeOnNode(List<String> command, String returnType = null) {
    //Set the orbit pypeline venvs folder
    def is_unix = isUnix()
    String venvs_folder = env.WORKSPACE + (is_unix ? "/.orbit/venvs" : "\\.orbit\\venvs")
    boolean returnStatus = returnType == 'status'
    boolean returnStdout = returnType == 'stdout'
    log.info("Executing orbit pypeline: ${command}")
    withEnv(["ORBIT_PYPELINE_VENVS_FOLDER=${venvs_folder}"]) {
        // prepend the system log level to the list of arguments
        command = ["-L", log.systemLevel] + command
        // ensure every argument is in quotes
        def arguments = command.collect { "\"${it}\"" }.join(" ")
        if (is_unix) {
            def wrapper = config('UNIX_ORBIT_PYPELINE_LOCATION')
            return sh(
                script: "${wrapper} ${arguments}",
                returnStdout: returnStdout,
                returnStatus: returnStatus,
            )
        } else {
            def wrapper = config('WINDOWS_ORBIT_PYPELINE_LOCATION')
            return bat(
                script: "${wrapper} ${arguments}",
                returnStdout: returnStdout,
                returnStatus: returnStatus
            )
        }
    }
}
/**
 * Runs authenticode signing command using orbit-pypeline
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * pypeline.signing(
 *     "Musarubra",
 *     "sha2",
 *     ["FileToSign1.exe", "FileToSign2.exe"]
 * )
 * pypeline.signing(
 *     "Musarubra",
 *     "masign",
 *     ["FileToSign1.exe", "FileToSign2.exe"]
 * )
 * </pre>
 *
 * @param sign_cert [T:String] The certificate to use (eg. Musarubra)
 * @param sign_type [T:String] The sign type (eg. sha2)
 * @param files List<String> - The list of files to sign
 * @param removeExistingSignatures [T:Boolean] Whether to remove existing signatures from the files. This is only applicable to sha2 and sha2_ph sign types. [DEFAULT:false]
 */
private void signing(String sign_cert, String sign_type, List<String> files, boolean removeExistingSignatures=false) {
    String noProxy = config.array("TRELLIX_SIGNING_SERVERS").join(',')
    def build_node_id = utils.getBuildNodeID()
    withEnv([
        "NO_PROXY=${noProxy}",
        "no_proxy=${noProxy}",
        "http_proxy=http://proxy.ess.gslb.entsec.com:9090",
        "https_proxy=http://proxy.ess.gslb.entsec.com:9090",
    ]) {
        withCredentials([
            usernamePassword(
                credentialsId: CredentialStore.CREDENTIAL_FOR_RADAR_TOKEN_AUTHENTICATION,
                usernameVariable: "UNUSED",
                passwordVariable: "RADAR_TOKEN",
            ),
            usernamePassword(
                credentialsId: CredentialStore.CREDENTIAL_FOR_TRELLIX_SIGNING_SERVER,
                usernameVariable: "SIGNING_SERVER_USER",
                passwordVariable: "SIGNING_SERVER_PASS",
            ),
            usernamePassword(
                credentialsId: CredentialStore.CREDENTIAL_FOR_SIGNING_SERVICE,
                usernameVariable: "SIGNING_SERVICE_USERNAME",
                passwordVariable: "SIGNING_SERVICE_PASSWORD",
            ),
        ]) {
            def command = [
                "signing",
                "-E", config('RADAR_ENVIRONMENT'),
                "sign-file",
                "-b", orbit.buildRecordId(),
                "-c", sign_cert,
                "-t", sign_type,
                "-n", build_node_id,
            ]
            if(removeExistingSignatures){
                log.info("Removing any existing Signatures on the files.")
                command += ["-r"]
            }
            commands = splitCommand(command, "-f", files)
            timeout(time: config('SIGNING_SERVICE_REQUEST_TIMEOUT'), unit: 'SECONDS') {
                for (def c in commands) {
                    executeOnNode(c)
                }
            }
        }
    }
}

/**
 * Appends options to the array of pypeline command arguments.
 *
 * @param list The current list of arguments
 * @param flag The flag to use together with the value
 * @param values Either a list of a single value to append to the command
 * @return The updated array of pypeline command arguments
 */
@NonCPS
private def withOptions(def list, def flag, def values) {
    if (values != null && values != "") {
        if (values instanceof Iterable) {
            for (def value in values) {
                list += [flag, value]
            }
        } else {
            list += [flag, values]
        }
    }
    return list
}

/**
 * Split a command into multiple commands by a flag on a list of values.
 * Used by signing to work around the 8191 character maximum command
 * length of the cmd shell on Windows.
 *
 * The method appends the flag to each value in the provided list and ensures that
 * each resulting command does not exceed the specified length limit.
 *
 * @param list The current list of arguments forming the base of the command.
 * @param flag The flag (e.g., "-f") to append before each value in the `values` list.
 * @param values A list of values to append to the command with the flag. 
 *               If a single value is passed, it is treated as a list with one item.
 * @return A list of lists, where each sublist represents a separate command to execute. 
 *         Each sublist contains the base command and the flag-value pairs, up to the length limit.
 */
private def splitCommand(def list, def flag, def values) {
    if (isUnix()) {
        return [withOptions(list, flag, values)]
    } else {
        if (values == null || values == "") {
            return [list]
        }
        /*
         * On windows the maximum command length in cmd is
         * 8191 characters. We will split the command into
         * multiple commands by the flag.
         */
        def limit = 8100
        // The limit should be reduced by the overhead of
        // additional arguments added by `executeOnNode()`.
        // The number `3` is used because we surround each argument
        // with double quotes and add a space between arguments.
        limit -= [
            config('WINDOWS_ORBIT_PYPELINE_LOCATION'),
            "-L",
            log.systemLevel.toString(),
        ].sum { it.length() + 3 }
        return StringUtils.splitCommandByFlag(list, flag, values, limit)
    }
}
