package com.mcafee.orbit.Artifact

import com.mcafee.orbit.Runner.RunnerWithParameters

/**
 * Downloader for arbitrary artifacts
 */
final class BuildArtifactsArbitraryDownload extends RunnerWithParameters<BuildArtifactsArbitraryDownloadParameters> {
    final BuildArtifactsHandler handler
    /**
     * Default constructor
     *
     * @param handler The artifact handler
     */
    BuildArtifactsArbitraryDownload(BuildArtifactsHandler handler) {
        super(handler.build)
        this.handler = handler
    }
    /**
     * Downloads an arbitrary artifact
     *
     * @param args An instance of {@link BuildArtifactsArbitraryDownloadParameters}
     * @return void
     */
    @Override
    protected call(BuildArtifactsArbitraryDownloadParameters args) {
        if (!args.target) {
            args.target = build.env.WORKSPACE
        }
        handler.buildInfo.append(build.context.artifacts.download(args))
    }
}
