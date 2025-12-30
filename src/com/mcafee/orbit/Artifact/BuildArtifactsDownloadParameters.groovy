package com.mcafee.orbit.Artifact

/**
 * Parameters for downloading artifacts from Artifactory
 */
final class BuildArtifactsDownloadParameters extends AbstractBuildArtifactsDownloadParameters {
    /**
     * The name of component to download
     */
    String component
    /**
     * The version of component to download
     */
    String version
    /**
     * The build number of component to download
     */
    String buildNumber
}
