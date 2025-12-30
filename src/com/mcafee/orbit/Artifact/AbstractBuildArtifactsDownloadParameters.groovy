package com.mcafee.orbit.Artifact

import com.mcafee.orbit.Parameters.*

/**
 * Common parameters for downloading artifacts
 */
abstract class AbstractBuildArtifactsDownloadParameters extends Parameters {
    /**
     * Flattens directory structure on downland
     */
    Boolean flat = true
    /**
     * Recursively search the path for specified file patterns
     */
    Boolean recursive = true
    /**
     * Comma-separated list of files to download
     */
    String files = '**'
    /**
     * Type of artifacts to download.
     * E.g.: Build, Package, Default and BuildAndPackage
     */
    @AllowEmpty String artifactType
    /**
     * Location to which to download files to.
     * Defaults to the WORKSPACE
     */
    @AllowEmpty String target
}