import hudson.model.Node
import hudson.slaves.*
import hudson.plugins.sshslaves.*
import hudson.plugins.sshslaves.verifiers.*

@NonCPS
private void RemoveAllLabels(String nodeName) {
    jenkins.model.Jenkins.instance.nodes.findAll {
        it.nodeName == nodeName
    }.each {
        it.labelString = ""
    }
    Jenkins.instance.labels.each { it.reset() }
}

@NonCPS
private String GetAllLabels(String nodeName) {
    return hudson.model.Hudson.instance.slaves.find {
        it.nodeName == nodeName
    }?.labelString
}

@NonCPS
private void SetLabels(String nodeName, String labels) {
    hudson.model.Hudson.instance.slaves.findAll {
        it.nodeName == nodeName
    }.each {
        it.labelString = labels
    }
    Jenkins.instance.labels.each { it.reset() }
}

@NonCPS
private boolean IsDockerNode(String nodeName) {
    return hudson.model.Hudson.instance.slaves.find {
        it.nodeName == nodeName
    }?.getNodeDescription()?.contains('Docker Node') ||
            isKubernetes(nodeName)
}

// Check for the "SkipExternalAVScan" label on the build node.
@NonCPS
private boolean SkipExternalAVScan(String nodeName) {
    return isLabelAppliedToNode(nodeName, "SkipExternalAVScan")
}

// Check for the "kubernetes_agent" label on the build node.
@NonCPS
private boolean isKubernetes(String nodeName) {
    return isLabelAppliedToNode(nodeName, "kubernetes_agent")
}

// Check if a label has been applied to a node
@NonCPS
private boolean isLabelAppliedToNode(String nodeName, String label) {
    return hudson.model.Hudson.instance.slaves.find {
        it.nodeName == nodeName
    }?.labelString.contains(label)
}

/**
 * Creates a node object in Jenkins.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * agent.CreateNode("node-name")
 * </pre>
 *
 * @param name [T:String] The name of the node to create
 * @param remoteFS [T:String] The path to the remote root directory
 * @param javaPath [T:String] The path of the java bin to use for jenkins
 * @param ip [T:String][OPTIONAL] The ip address for a linux node, `null` for windows nodes
 */
@NonCPS
private void CreateNode(def name, def remoteFS, String javaPath, def ip = null) {
    // Create the node launcher
    // JNLP by default for Windows nodes
    def launcher = new JNLPLauncher()
    // SSH for linux nodes if the ip address is passed in
    if (ip) {
        launcher = new SSHLauncher(ip, 22, 'orbit_node_ssh')
        launcher.setSshHostKeyVerificationStrategy(
            new NonVerifyingKeyVerificationStrategy()
        )
        launcher.setJavaPath(javaPath)
    }
    // Create the node object
    def node = new DumbSlave(name, remoteFS, launcher)
    node.setLabelString(name)
    node.setNodeDescription('Dynamic node provisioned via NPS.')
    node.setNumExecutors(1)
    node.setMode(Node.Mode.NORMAL)
    node.setRetentionStrategy(new RetentionStrategy.Always())
    // Add the new node to Jenkins
    Jenkins.instance.addNode(node)
    // Save the configuration
    Jenkins.instance.save()
}

/**
 * Returns the JNLP secret for a node.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * agent.GetJnlpSecretForNode("node-name")
 * </pre>
 *
 * @param name The name of the node to get the JNLP secret for
 */
@NonCPS
private def GetJnlpSecretForNode(def name) {
    try {
        return Jenkins.instance.getComputer(name).getJnlpMac()
    } catch (Throwable e) {
        throw new RuntimeException("Failed to collect node information from Jenkins.", e)
    }
}

/**
 * Removes a node object from Jenkins.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * agent.RemoveNode("node-name")
 * </pre>
 *
 * @param name The name of the node to remove
 */
@NonCPS
private void RemoveNode(String name) {
    jenkins.model.Jenkins.instance.nodes.findAll {
        it.nodeName == name
    }.each {
        it.getComputer().doDoDelete()
    }
}
