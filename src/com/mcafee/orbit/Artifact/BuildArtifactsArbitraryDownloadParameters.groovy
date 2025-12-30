package com.mcafee.orbit.Artifact

/**
 * Parameters for downloading arbitrary artifacts
 */
final class BuildArtifactsArbitraryDownloadParameters extends AbstractBuildArtifactsDownloadParameters {
    /**
     * FIXME: No idea what this is. Not documented in legacy code either :3
     */
    String root
    /**
     * The ID of the credentials to use for connecting to artifactory server
     */
    String credentialsId
    /**
     * The URL to download from.
     * FIXME: Relative? Absolute?
     */
    String url
}