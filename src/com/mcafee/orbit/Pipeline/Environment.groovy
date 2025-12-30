package com.mcafee.orbit.Pipeline

import com.cloudbees.groovy.cps.SerializableScript

/**
 * Accessor for pipeline environment variables
 */
final class Environment {
    private final SerializableScript script
    /**
     * Default constructor
     *
     * @param script The pipeline script
     */
    Environment(SerializableScript script) {
        this.script = script
    }
    /**
     * Returns the path to the current workspace
     *
     * @return The path to the current workspace
     */
    String getWORKSPACE() {
        this.script.env.getProperty('WORKSPACE')
    }
    /**
     * Returns the build record id for the current build
     *
     * @return build record id
     */
    Integer getBUILD_RECORD_ID() {
        this.script.env.getProperty('BUILD_RECORD_ID') as Integer
    }
    /**
     * Returns the orbit build number for the current
     * build and possibly a package number
     *
     * @return orbit build number
     */
    String getORBIT_BUILD_NUMBER() {
        this.script.env.getProperty('ORBIT_BUILD_NUMBER')
    }
    /**
     * Returns the orbit build number only for the current build
     *
     * @return orbit build number
     */
    Integer getORBIT_BUILD_NUMBER_ONLY() {
        this.script.env.getProperty('ORBIT_BUILD_NUMBER_ONLY') as Integer
    }
    /**
     * Returns the orbit build number only for the current build
     *
     * @return orbit build number
     */
    Integer getORBIT_PACKAGE_NUMBER() {
        this.script.env.getProperty('ORBIT_PACKAGE_NUMBER') as Integer
    }
    /**
     * Returns the name of the currently executing stage
     *
     * @return stage name
     */
    String getSTAGE_NAME() {
        this.script.env.getProperty('STAGE_NAME')
    }
}