package com.mcafee.orbit.Artifact

import com.mcafee.orbit.Runner.RunnerWithParameters

/**
 * Stashes files for upload to artifactory during delivery
 */
final class BuildArtifactsStash extends RunnerWithParameters<BuildArtifactsStashParameters> {
    final BuildArtifactsHandler handler
    /**
     * Default constructor
     *
     * @param handler The artifact handler
     */
    BuildArtifactsStash(BuildArtifactsHandler handler) {
        super(handler.build)
        this.handler = handler
    }
    /**
     * Stashes files for upload to artifactory during delivery
     *
     * @param args An instance of {@link BuildArtifactsStashParameters}
     * @return void
     */
    @Override
    protected call(BuildArtifactsStashParameters args) {
        args.includes = args.includes.replace('\\', '/')
        args.name = handler.nextStashName
        build.log.debug("Stashing: ${args.includes}")
        try {
            build.context.stash(name: args.name, includes: args.includes)
        } catch (e) {
            build.log.warn("Failed to stash files: $e")
            if (!args.catchFail) {
                throw e
            }
        }
        handler.stashList << args
    }
}