import com.mcafee.orbit.Utils.StringUtils
import com.mcafee.orbit.Credentials.CredentialStore

/**
 * The <code>orbitDocker</code> stage template.
 *
 * <p>This stage separates the building of docker images into 3 stages:</p>
 * <ol>
 *     <li>Build docker image</li>
 *     <li>Test docker image</li>
 *     <li>Publish docker image</li>
 * </ol>
 *
 * <p>See https://confluence.trellix.com/display/PDS/Docker for more information.</p>
 *
 * <p>The <code>env.BRANCH_NAME</code> is prepended to the docker image tag. Due to restrictions,
 * if the branch_name contains multiple directories, we will only use the final directory to tag it.
 * </p>
 *
 * @param image_tag [T:String] [OPTIONAL] Specifies the tag to apply to the docker image.
 *                           [DEFAULT:$BRANCH_NAME.$BUILD_NUMBER]
 * @param distribution [T:String] [OPTIONAL] Specifies the distribution to build.
 *                           [DEFAULT:"mlos3"]
 * @param project [T:String] [OPTIONAL] Specifies the project to build.
 *                           [DEFAULT:env.JOB_NAME]
 * @param credId [T:String] [OPTIONAL] Specifies the credential ID used within the Dockerfile
 *                           to access remote repos. [DEFAULT:"artifactory_api_token"].
 *                           Within the Dockerfile, it is accessed via: ARG credId
 * @param buildArgs [T:Map] [OPTIONAL] Any additional arguments to be passed to the docker build.
 * @param useDefaultRepo [T:Boolean] [OPTIONAL] Specifies if the docker image is pushed to the default Artifactory repository.
 *                           'orbit-docker-local' or not.
 *                           [DEFAULT:true]
 * @param dockerRegistries [T:List&lt;String&gt;] [OPTIONAL] Additional docker registries to store the image.
 *                           The following strings will be replaced &lt;PROJECT&gt;, &lt;DISTRIBUTION&gt;, &lt;IMAGE_TAG&gt;
 *                           Example value - https://docker.dev.webwasher.com/repository/csp/&lt;PROJECT&gt;:&lt;IMAGE_TAG&gt;
 */
public void call(Map arg) {
    utils.mapReporter(arg)

    // Docker builds should be never published to eCM.
    env.skipECM = true

    def dockerBranch = env.BRANCH_NAME.split("/").last()
    def fullJobName = env.JOB_NAME.split("/")
    String project = arg.project ?: fullJobName[fullJobName.size() - 3]
    String image_tag = arg.image_tag ?: dockerBranch + "." + orbit.buildNumberOnly()
    String distribution = arg.distribution ?: 'mlos3'
    String dockerName = (project + "/" + distribution).toLowerCase()
    String artifactoryCred = arg.credID ?: CredentialStore.ARTIFACTORY_API_TOKEN
    boolean useDefaultRepo = arg.useDefaultRepo != null ? arg.useDefaultRepo : true
    def buildArgs = ''
    if (arg.buildArgs) {
        for (def i=0; i<arg.buildArgs.size(); i++) {
            def key = arg.buildArgs.keySet()[i]
            def value = arg.buildArgs[key]
            buildArgs += " --build-arg $key='$value'"
        }
    }
    /* If a build explicitly uses the arg.useDefaultRepo value then we should respect that choice irrespective of the 
    fact that arg.dockerRegistries might be empty and ultimately the user might not have any docker repositories to push
    the image to. 
    If a call to orbitDocker does not use the useDefaultrepo parameter, then it is true by default. This will 
    maintain backwards compatibility
    */
    assert !image_tag.contains(" ") : "Error: image_tag must not contain spaces in the name"
    assert !distribution.contains(" ") : "Error: distribution must not contain spaces in the name"
    assert !image_tag.contains("stable") : "Error: should not create stable tag here.\n" +
            "This image should be verified and then promoted separately."

    // Update build description to include docker specific info
    currentBuild.description += "\nDistribution : " + distribution
    currentBuild.description += "\nimage_tag : " + image_tag

    // Generate all the tag and push commands
    def dockerRegCommands = [:]
    def dockerRegistries = []
    if (arg.dockerRegistries != null) {
        dockerRegistries += arg.dockerRegistries
    }
    if (useDefaultRepo) {
        dockerRegistries += config('DOCKER_REPO')
    }
    for (def dockerRegistry in dockerRegistries) {
        String tmpDockerTag = dockerRegistry.replace("<PROJECT>", project)
        tmpDockerTag = tmpDockerTag.replace("<DISTRIBUTION>", distribution)
        String tmpLatestTag = tmpDockerTag.replace("<IMAGE_TAG>", "latest")
        tmpDockerTag = tmpDockerTag.replace("<IMAGE_TAG>", image_tag)
        dockerRegCommands[dockerRegistry] = [
            tagCmd: "docker tag ${dockerName} ${tmpDockerTag}",
            latestTagCmd: "docker tag ${dockerName} ${tmpLatestTag}",
            pushCmd: "docker push ${tmpDockerTag}",
            latestPushCmd: "docker push ${tmpLatestTag}"
        ]
    }

    stage('DockerBuild') {
        utils.mapReporter([
            "Image tag to build": image_tag,
            "project": project,
            "distribution": distribution,
            "dockerName": dockerName,
            "dockerRegistries": dockerRegistries,
            "dockerBranch": dockerBranch,
        ])
        withCredentials([string(credentialsId: artifactoryCred, variable: 'credId')]) {
            dir(distribution) {
                execute("docker build $buildArgs --build-arg credId='$credId' -t $dockerName .")
            }
        }
    }

    stage('DockerTest') {
        def is_unix = isUnix()
        def container_name = "test_container_" + StringUtils.getPaddedRandomNumber()
        if (is_unix) {
            def container = sh(
                script: "docker run --name $container_name --entrypoint tail -d $dockerName -f /dev/null",
                returnStdout: true,
            ).trim()
            log.info("Started docker container $container_name")
            execute("docker exec $container_name /bin/echo OKAY", "Docker exec failed")
        } else {
            def container = bat(
                script: "docker run --name $container_name --entrypoint cmd.exe -d -i -t $dockerName ''",
                returnStdout: true,
            ).trim()
            log.info("Started docker container $container_name")
            execute("docker exec $container_name cmd.exe", "Docker exec failed")
        }
        execute("docker top $container_name", "Docker top failed")
        execute("docker stop $container_name", "Docker stop failed")
    }

    stage('DockerPublish') {
        for (def item in dockerRegCommands.entrySet()) {
            log.info("Pushing image to ${item.key}.")
            execute(item.value["tagCmd"], "Docker tag failed for ${item.key}.")
            execute(item.value["pushCmd"], "Docker push failed for ${item.key}.")
            // Only Tag current docker image as latest and
            // push to repo when building from master branch
            if (env.BRANCH_NAME?.toLowerCase() == 'master' && !env.SkipLatestTag?.toBoolean()) {
                execute(item.value["latestTagCmd"], "Docker tag latest failed for ${item.key}.")
                execute(item.value["latestPushCmd"], "Docker push latest failed for ${item.key}.")
            }
            log.info("Successfully pushed image to ${item.key}.")
        }

        // Remove docker images once they are pushed to the repo.
        try {
            log.info("Docker prune $distribution")
            execute('docker image prune -a -f')
        } catch (Throwable e) {
            // Print the error but don't fail the build, as there
            // are valid reasons for the prune command to fail.
            log.error('Docker prune error')
            log.error(e)
        }
    }
}
