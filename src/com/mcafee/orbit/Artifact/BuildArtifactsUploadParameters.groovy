package com.mcafee.orbit.Artifact

import com.mcafee.orbit.Parameters.*

final class BuildArtifactsUploadParameters extends Parameters {
    /**
     * The type of files to upload. Default is null, means all types.
     */
    @AllowEmpty String type
}
