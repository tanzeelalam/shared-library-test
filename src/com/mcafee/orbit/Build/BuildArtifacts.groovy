package com.mcafee.orbit.Build

import com.mcafee.orbit.Artifact.BuildArtifactsHandler

/**
 * Build artifacts management
 */
class BuildArtifacts {
    /**
     * Internal handler for build artifact management.
     * Used to proxy access to various runners.
     */
    private final BuildArtifactsHandler handler
    /**
     * Default constructor
     *
     * @param build The build internals
     */
    BuildArtifacts(BuildInternals build) {
        handler = new BuildArtifactsHandler(build)
    }
    /**
     * Stashes files for upload to artifactory during delivery
     *
     * @param args An instance of {@link com.mcafee.orbit.Artifact.BuildArtifactsStashParameters}
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    void stashForUpload(def args) {
        handler.stasher.run(args)
    }
    /**
     * Uploads stashed files to Artifactory
     *
     * @param type The type of files to upload. Default is null, means all types.
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    void uploadStash(def args) {
        handler.uploader.run(args)
    }
    /**
     * Download the specified component from artifactory.
     * If the files in the file parameter do not download correctly, fail the build.
     *
     * @param args An instance of {@link com.mcafee.orbit.Artifact.BuildArtifactsDownloadParameters}
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    void download(def args) {
        handler.downloader.run(args)
    }
    /**
     * Download the specified component from an arbitrary location.
     * If the files in the file parameter do not download correctly, fail the build.
     *
     * @param args An instance of {@link com.mcafee.orbit.Artifact.BuildArtifactsArbitraryDownloadParameters}
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    void arbitraryDownload(def args) {
        handler.arbitraryDownloader.run(args)
    }
    /**
     * Publishes the build information to Artifactory
     */
    void publishBuildInfo() {
        handler.publisher.run()
    }
    /**
     * Shadows access to this.internals
     */
    static def getHandler() {
        throw new IllegalAccessException(
            'Attempted to access internal artifact handler!'
        )
    }
}