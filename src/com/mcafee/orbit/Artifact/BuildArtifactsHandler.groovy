package com.mcafee.orbit.Artifact

import com.mcafee.orbit.Build.BuildInternals

/**
 * Handler for artifact-related actions
 */
final class BuildArtifactsHandler {
    final BuildInternals build
    final BuildArtifactsStash stasher
    final BuildArtifactsUpload uploader
    final BuildArtifactsDownload downloader
    final BuildArtifactsArbitraryDownload arbitraryDownloader
    final BuildArtifactsPublish publisher
    final List<BuildArtifactsStashParameters> stashList = new ArrayList<BuildArtifactsStashParameters>()
    final def buildInfo

    private int stashNumber = 0

    /**
     * Default constructor
     *
     * @param build The build internals
     */
    BuildArtifactsHandler(BuildInternals build) {
        this.build = build
        this.buildInfo = build.context.Artifactory.newBuildInfo()
        stasher = new BuildArtifactsStash(this)
        uploader = new BuildArtifactsUpload(this)
        downloader = new BuildArtifactsDownload(this)
        arbitraryDownloader = new BuildArtifactsArbitraryDownload(this)
        publisher = new BuildArtifactsPublish(this)
    }
    /**
     * Returns the name for the next stash
     * @return the name for the next stash
     */
    String getNextStashName() {
        "stash${++stashNumber}"
    }
}
