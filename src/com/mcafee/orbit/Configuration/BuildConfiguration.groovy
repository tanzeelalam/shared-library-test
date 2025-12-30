package com.mcafee.orbit.Configuration

import com.cloudbees.groovy.cps.SerializableScript
import com.mcafee.orbit.Configuration
import com.mcafee.orbit.Utils.Mappable
import com.mcafee.orbit.Utils.StringUtils

/**
 * Configuration for the Build
 */
final class BuildConfiguration extends Mappable {
    private final SerializableScript script
    /**
     * The poorly named component version.
     * Deprecated. Use HIERARCHY.COMPONENT_VERSION, see {@link BuildHierarchy#COMPONENT_VERSION}
     */
    @Deprecated
    final String BUILD_VERSION
    /**
     * The Jenkins job name
     */
    final String JOB_NAME
    /**
     * See {@link BuildHierarchy}
     */
    final BuildHierarchy HIERARCHY
    /**
     * The orbit environment. Dev, Staging, Production, Etc.
     */
    final String ORBIT_BUILD_ENV
    /**
     * Default, Build, Package or BuildAndPackage
     */
    final String ORBIT_JOB_TYPE
    /**
     * Base URL for the PDIT Artifactory server
     */
    final String ARTIFACTORY_URL
    /**
     * Default constructor
     *
     * @param config User configuration
     * @param script The pipeline script
     */
    BuildConfiguration(Configuration config, SerializableScript script) {
        this.script = script
        HIERARCHY = new BuildHierarchy(
            script.env.JOB_NAME as String,
            config.BUILD_VERSION as String
        )
        BUILD_VERSION = config.BUILD_VERSION
        JOB_NAME = script.env.JOB_NAME as String
        ORBIT_BUILD_ENV = script.env.ORBIT_BUILD_ENV as String
        ORBIT_JOB_TYPE = script.env.ORBIT_JOB_TYPE as String
        ARTIFACTORY_URL = script.env.ARTIFACTORY_URL as String
        if (StringUtils.isNullOrEmpty(config.BUILD_VERSION)) {
            throw new IllegalArgumentException(
                'BUILD_VERSION not set in build configuration.'
            )
        } else {
            script.env.BUILDVERSION = config.BUILD_VERSION
        }
    }
}