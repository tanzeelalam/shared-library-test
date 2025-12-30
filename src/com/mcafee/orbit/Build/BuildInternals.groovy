package com.mcafee.orbit.Build

import com.cloudbees.groovy.cps.SerializableScript
import com.mcafee.orbit.Configuration
import com.mcafee.orbit.Configuration.BuildConfiguration
import com.mcafee.orbit.Log.Logger
import com.mcafee.orbit.Pipeline.Context
import com.mcafee.orbit.Pipeline.Environment
import com.mcafee.orbit.Utils.AgentToolbox

/**
 * Internal build class
 */
final class BuildInternals {
    final Logger log
    final Environment env
    final Context context
    final BuildConfiguration config
    final AgentToolbox toolbox
    private BuildArtifacts artifacts
    /**
     * Default constructor
     *
     * @param script A reference to the Jenkinsfile context
     * @param userConfig A user configuration object
     */
    BuildInternals(SerializableScript script, Configuration userConfig) {
        Environment env = new Environment(script)
        Context context = new Context(script)
        Logger logger = new Logger(context, script)
        BuildConfiguration config = new BuildConfiguration(userConfig, script)
        AgentToolbox toolbox = new AgentToolbox(config, env, context, logger)
        this.log = logger
        this.env = env
        this.context = context
        this.config = config
        this.toolbox = toolbox
    }
    /**
     * Getter for the artifact management object
     */
    BuildArtifacts getArtifacts() {
        if (artifacts == null) {
            artifacts = new BuildArtifacts(this)
        }
        artifacts
    }
}