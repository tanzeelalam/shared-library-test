package com.mcafee.orbit.Artifact

import com.mcafee.orbit.Runner.RunnerWithParameters

/**
 * Downloader for Artifactory artifacts
 */
final class BuildArtifactsDownload extends RunnerWithParameters<BuildArtifactsDownloadParameters> {
    final BuildArtifactsHandler handler
    /**
     * Default constructor
     *
     * @param handler The artifact handler
     */
    BuildArtifactsDownload(BuildArtifactsHandler handler) {
        super(handler.build)
        this.handler = handler
    }
    /**
     * Downloads an artifact from Artifactory
     *
     * @param args An instance of {@link BuildArtifactsDownloadParameters}
     * @return void
     */
    @Override
    protected call(BuildArtifactsDownloadParameters args) {
        if (!args.target) {
            args.target = build.env.WORKSPACE
        }
        handler.buildInfo.append(build.context.artifacts.download(args))
    }
}
