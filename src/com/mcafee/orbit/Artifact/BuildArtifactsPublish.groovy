package com.mcafee.orbit.Artifact

/**
 * Build information publisher
 */
final class BuildArtifactsPublish implements Runnable {
    final BuildArtifactsHandler handler
    /**
     * Default constructor
     *
     * @param handler The artifact handler
     */
    BuildArtifactsPublish(BuildArtifactsHandler handler) {
        this.handler = handler
    }
    /**
     * Publishes the build information to Artifactory
     */
    @Override
    void run() {
        handler.build.context.artifacts.publishBuildInfo([
            buildInfo: handler.buildInfo,
        ])
    }
}