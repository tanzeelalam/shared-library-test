package com.mcafee.orbit

import com.cloudbees.groovy.cps.SerializableScript
import com.mcafee.orbit.Build.BuildArtifacts
import com.mcafee.orbit.Build.BuildInternals
import com.mcafee.orbit.Log.Logger

/**
 * Main build class, this is to be used directly in the project Jenkinsfile.
 *
 * Instantiate by passing the script context to the constructor,
 * along with a configuration object.
 *
 * <pre>
 *     import com.mcafee.orbit.Build
 *
 *     Map config = [ BUILD_VERSION: '0.0.0' ]
 *     Build build = new Build(this, config)
 * </pre>
 *
 * Or:
 * <pre>
 *     import com.mcafee.orbit.Build
 *     import com.mcafee.orbit.Configuration
 *
 *     Configuration config = new Configuration(
 *         BUILD_VERSION: '0.0.0'
 *     )
 *     Build build = new Build(this, config)
 * </pre>
 */
final class Build {
    private final BuildInternals internals
    /**
     * Default constructor
     *
     * @param script A reference to the Jenkinsfile context
     * @param userConfig A user configuration object
     */
    Build(SerializableScript script, Configuration userConfig) {
        internals = new BuildInternals(script, userConfig)
    }
    /**
     * Alternative constructor
     *
     * @param script A reference to the Jenkinsfile context
     * @param userConfigMap A user configuration object
     */
    Build(SerializableScript script, Map userConfigMap) {
        Configuration userConfig = new Configuration(userConfigMap)
        internals = new BuildInternals(script, userConfig)
    }
    /**
     * Shadows access to this.internals
     */
    static def getInternals() {
        throw new IllegalAccessException('Attempted to access build internals!')
    }
    /**
     * Returns an instance of the current {@link com.mcafee.orbit.Log.Logger} object
     *
     * @return A Logger object
     */
    Logger getLog() {
        internals.log
    }
    /**
     * Returns the current configuration object.
     * Any changes made to this object do not affect
     * the actual build configuration.
     *
     * @return The current configuration object
     */
    Map getConfig() {
        internals.config.toMap()
    }
    /**
     * Getter for the artifact management object
     */
    BuildArtifacts getArtifacts() {
        internals.artifacts
    }
}