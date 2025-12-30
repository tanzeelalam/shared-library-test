package com.mcafee.orbit.Artifact

import com.mcafee.orbit.Parameters.*

/**
 * Parameters for stashing files for upload to Artifactory.
 */
final class BuildArtifactsStashParameters extends Parameters {
    /**
     * Ant-style expression for files to be included in the stash
     */
    String includes
    /**
     * Will not fail a build if stashing is unsuccessful
     */
    Boolean catchFail = false
    /**
     * Sub-path to upload this set of files to.
     * During upload to artifactory, these files are uploaded at this
     * path relative to the Artifactory default project upload path.
     */
    @AllowEmpty String target
    /**
     * Type of Artifact.
     * E.g.: Build, Package
     */
    @AllowEmpty String artifactType
    /**
     * For internal use.
     * The name of the stash.
     */
    @Internal String name
    /**
     * For internal use.
     * Marks a stash for deletion, after it is used.
     */
    @Internal Boolean removeFlag = false
}