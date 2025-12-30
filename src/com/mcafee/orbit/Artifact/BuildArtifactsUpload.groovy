package com.mcafee.orbit.Artifact

import com.mcafee.orbit.Runner.RunnerWithParameters

/**
 * Uploader for Artifactory artifacts
 */
final class BuildArtifactsUpload extends RunnerWithParameters<BuildArtifactsUploadParameters> {
    final BuildArtifactsHandler handler
    /**
     * Default constructor
     *
     * @param handler The artifact handler
     */
    BuildArtifactsUpload(BuildArtifactsHandler handler) {
        super(handler.build)
        this.handler = handler
    }
    /**
     * Uploads artifacts to Artifactory
     *
     * @param args An instance of {@link BuildArtifactsUploadParameters}
     * @return void
     */
    @Override
    protected call(BuildArtifactsUploadParameters args) {
        String type
        if (args != null) {
            type = args.type
        }
        if (handler.stashList.size() > 0) {
            String outputDirName = "artifactory_upload_${build.env.ORBIT_BUILD_NUMBER}"
            build.context.dir(outputDirName) {
                for (int i=0; i < handler.stashList.size(); i++) {
                    BuildArtifactsStashParameters stash = handler.stashList[i]
                    if (type == null || stash.artifactType == type) {
                        build.log.debug("Unstashing: ${stash.name}")
                        if (stash.target) {
                            build.context.dir(stash.target) {
                                build.context.unstash(stash.name)
                            }
                        } else {
                            build.context.unstash(stash.name)
                        }
                        // Already unstashed so mark for removal
                        stash.removeFlag = true
                    }
                }
            }
            handler.stashList.removeAll { it.removeFlag }
            // Upload unstashed files
            build.context.dir(outputDirName) {
                def newBuildInfo = build.context.artifacts.upload([
                    files    : '**',
                    flat     : false,
                    recursive: true,
                    artifactType: args.type ?: build.config.ORBIT_JOB_TYPE
                ])
                handler.buildInfo.append(newBuildInfo)
            }
        }
    }
}
