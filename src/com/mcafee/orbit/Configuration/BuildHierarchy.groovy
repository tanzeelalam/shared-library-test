package com.mcafee.orbit.Configuration

/**
 * Works out the values the build hierarchy
 * and stores them for easy access
 */
final class BuildHierarchy {
    /**
     * The product name
     */
    final String PRODUCT
    /**
     * The component name
     */
    final String COMPONENT
    /**
     * The component version
     */
    final String COMPONENT_VERSION
    /**
     * Default constructor
     *
     * @param jobName The Jenkins job name
     * @param componentVersion The component version
     */
    BuildHierarchy(String jobName, String componentVersion) {
        List splitFullJobName = jobName.split('/')
        PRODUCT = splitFullJobName[0]
        COMPONENT = splitFullJobName[1]
        COMPONENT_VERSION = componentVersion
    }
}