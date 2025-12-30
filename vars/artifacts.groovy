import groovy.json.JsonSlurperClassic
import com.mcafee.orbit.Credentials.CredentialStore
import com.mcafee.orbit.Utils.StringUtils

/**
 * Static class used to keeps track of whether
 * the artifactory URL has already been set in Radar
 */
class ArtifactoryState {
    static boolean isUrlSetInRadar = false
}

/**
 * Upload a list of files to an Artifactory repository.
 *
 * <p>
 * Files are uploaded to the standard repository (<component>-local/<component>_<version>_<buildnumber>).
 * Version and build number are picked up from environment variables env.BUILDVERSION and env.buildNumber respectively.
 * </p>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * artifacts.upload(
 *    files: ["file1, file2"],
 * )
 * </pre>
 * <pre>
 * artifacts.upload(
 *     files: "file1,file2,file3",
 *     component: "component-name",
 * )
 * </pre>
 *
 * @param files [T:List|String] List or comma separated value of filepaths that have to be uploaded to Artifactory.
 * @param component [T:String] [OPTIONAL] The name of the component under which to upload the files.
 * @param flat [T:boolean] [OPTIONAL] Flatten the directory structure before upload. [DEFAULT:true]
 * @param recursive [T:boolean] [OPTIONAL] Recursively search for matching file patterns. [DEFAULT:true]
 * @param artifactType [T:String] [OPTIONAL] Artifact type eg. Build, Package
 * @param product [T:String] [OPTIONAL] Product Name
 * @param jobName [T:String] [OPTIONAL] Job name of the project, e.g.
 * <code>"Orbit/Pipeline-common"</code>. Defaults to <code>env.JOB_NAME</code>. Use jobName in place of product and component params
 * @return [T:BuildInfo] https://jfrog.com/help/r/jfrog-pipelines-documentation/buildinfo
 */
public Object upload(Map arg) {
    assert (arg && arg.files): "ERROR: artifacts.upload : arg.files not provided"
    global()

    Boolean skipScan = config('DISABLEAVSCAN').toBoolean()
    if (env.DISABLEAVSCAN != null) {
        skipScan = env.DISABLEAVSCAN.toBoolean()
    }
    if (arg.skipScan)// This parameter is deliberately undocumented
    {
        skipScan = true
    }
    setArtifactoryUrlInRadar()

    String repositoryFullName, product, component, files, target
    String version, buildNumber

    String jobName = utils.isNullOrEmptyString(arg.jobName) ? env.JOB_NAME : arg.jobName
    def splitJobName = utils.splitJenkinsJobName(jobName)

    //We will have our defaults, if user has a override, use it else, go by default
    product = utils.isNullOrEmptyString(arg.product) ? splitJobName.product : arg.product
    component = utils.isNullOrEmptyString(arg.component) ? splitJobName.component : arg.component
    buildNumber = utils.isNullOrEmptyString(arg.buildNumber) ? orbit.buildNumber() : arg.buildNumber
    version = utils.isNullOrEmptyString(arg.version) ? env.BUILDVERSION : arg.version

    //right now we follow the convention <component>-local for repository name

    if(utils.isNullOrEmptyString(env.ARTIFACTORY_REPOSITORY_OVERRIDE)){
        repositoryFullName = this.getRepositoryNameFromComponent(component)
        target = repositoryFullName + "/" + component + "_" + version + "/b" + buildNumber + "/"
    }
    else{
        repositoryFullName = env.ARTIFACTORY_REPOSITORY_OVERRIDE
        target = repositoryFullName + "/b" + buildNumber + "/"
    }

    //The user can pass either a string or a list of files.
    //If the user passes a String or GString, we can directly call the createUploadSpec
    //Else we'll do a join and call the createUploadSpec function
    //User can also call artifacts upload using in that case build will fail, so added a check for GString.
    if (arg.files.getClass() == String || arg.files instanceof GString) {
        files = arg.files
    } else {
        files = arg.files.join(',') + ","
    }
    assert (files.length() > 1): "ERROR: artifacts.upload : arg.files is empty"

    //Double check we have all the values
    assert !utils.isNullOrEmptyString(repositoryFullName): "ERROR : artifacts.upload :repository is not set"
    assert !utils.isNullOrEmptyString(component): "ERROR : artifacts.upload : component is not set"
    assert !utils.isNullOrEmptyString(version): "ERROR : artifacts.upload : version is not set"
    assert !utils.isNullOrEmptyString(buildNumber): "ERROR : artifacts.upload : buildNumber is not set"

    def artifactoryServer = Artifactory.server(env.ARTIFACTORY_INSTANCE)

    def flat = arg.containsKey("flat") ? arg.flat : "true"
    def recursive = arg.containsKey("recursive") ? arg.recursive : "true"

    def artifactType = arg.containsKey("artifactType") ? arg.artifactType : env.ORBIT_JOB_TYPE

    def props = getJobArtifactProps(artifactType)

    // Push Artifact and File data to Radar before pushing to Artifactory. Fail upload to Artifactory in case Push to Radar is not successful. 
    // Use utils.exponentialRetry just in case of network issues. 
    def uploadSpec = createArtifactoryUploadSpec(
        filesStringCsv: files,
        target: target,
        flat: flat,
        recursive: recursive,
        props: props,
    )

    //Create new buildInfo to set the build number to orbit build number
    def buildInfo = Artifactory.newBuildInfo()
    buildInfo.setName component + '_' + version
    buildInfo.setNumber buildNumber

    try {
        def newBuildInfo
        utils.exponentialRetry(5, env.ARTIFACTORY_TIMEOUT, 0) {
            newBuildInfo = artifactoryServer.upload(uploadSpec, buildInfo)
        }
        def checksums = getChecksums(newBuildInfo.artifacts, target)
        radar.postArtifacts(checksums)
        log.info("Artifacts uploaded")
        log.debug("Checksums " + checksums)
        return newBuildInfo
    } catch (Throwable e) {
        radar.addBuildLog(
            "Error",
            "Artifactory_upload",
            env.current_stage,
            env.ARTIFACTORY_INSTANCE,
            e.message,
        )
        rethrow(e, 'Failed to upload artifact', true)
    }
}

/**
 * Returns the checksums for the uploaded artifacts.
 * @param artifacts The artifacts to get the checksums for.
 * @param target The relative path to the artifacts.
 * @return The checksums for the uploaded artifacts.
 */
@NonCPS
private def getChecksums(def artifacts, def target) {
    artifacts.collect {
        def last = target.split('/').findAll({ it }).last()
        def index = it.remotePath.indexOf(last) + last.size() + 1
        def relativePath = it.remotePath.substring(index)
        [
            Path: relativePath,
            CheckSum: it.sha256,
        ]
    }
}

/**
 * Create Artifactory upload spec from a comma-separated list of filepaths to be uploaded to Artifactory.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * artifacts.createArtifactoryUploadSpec(
 *     filesStringCsv:"/path/to/file1,/path/to/file2",
 *     target: "artifactory-path"
 * )
 * </pre>
 * <pre>
 * artifacts.createArtifactoryUploadSpec(
 *     filesStringCsv:"/path/to/file1,/path/to/file2",
 *     target: "artifactory-path",
 *     flat: false,
 *     recursive: false,
 * )
 * </pre>
 *
 * @param filesStringCsv [T:String] Comma-separated-value of filepaths that have to be uploaded to Artifactory
 * @param target [T:String] Artifactory path to upload files to.
 * @param flat [T:boolean] [OPTIONAL] Flatten Directory structure for upload. [DEFAULT:true]
 * @param recursive [T:boolean] [OPTIONAL] Recursively search through directory for the specified file patterns.
 * [DEFAULT:true]
 * @param props [T:String] [OPTIONAL] Artifact properties
 * @return [T:String] Upload spec string
 */
private String createArtifactoryUploadSpec(Map arg) {
    //TODO: Double check this code matches the jfrog recommendation
    assert arg.filesStringCsv: "ERROR: arg.filesStringCsv not provided"
    def fileStrings = arg.filesStringCsv.replace('\\', '/').tokenize(',')
    String filesArrStr = ""

    def flat = arg.containsKey("flat") ? arg.flat : "true"
    def recursive = arg.containsKey("recursive") ? arg.recursive : "true"

    for (int x = 0; x < fileStrings.size(); x++) {
        log.debug(fileStrings[x])
        filesArrStr = filesArrStr + "{" +
                "\"pattern\": \"" + fileStrings[x] + "\"," +
                "\"recursive\": \"" + recursive + "\"," +
                "\"target\":\"" + arg.target + "\"," +
                "\"flat\": \"" + flat + "\"," +
                "\"props\": \"" + arg.props +
                "\"}"
        if (x < fileStrings.size() - 1) {
            filesArrStr = filesArrStr + ","
        }
    }
    String ans = "{\"files\" :[" + filesArrStr + "]}"
    log.info("Created uploadSpec  = " + ans)
    return ans
}

/**
 * Download the specified component from Artifactory.
 *
 * <p>If the files in the files parameter do not download correctly, it will fail the build.</p>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * artifacts.download(
 *    component: "ESP",
 *    version: "11.0.0",
 *    buildNumber: "100",
 *    files: ["file1", "file2"],
 * )
 * </pre>
 *
 * @param component [T:String] Name of the component to download.
 * @param version [T:String] Version of the component to download.
 * @param buildNumber [T:String] Build number to download.
 * @param files [T:List|String] List or comma separated string of filepaths.
 * @param artifactoryInstance [T:String] [OPTIONAL] The Artifactory instance name to use. [DEFAULT:env.ARTIFACTORY_INSTANCE]
 * @param target [T:String] [OPTIONAL] Location to download files to locally. [DEFAULT:env.WORKSPACE]
 * @param flat [T:boolean] [OPTIONAL] Flatten directory structure after download. [DEFAULT:true]
 * @param recursive [T:boolean] [OPTIONAL] Recursively search for and download the specified file patterns. [DEFAULT:true]
 * @param artifactType [T:String] [OPTIONAL] The artifact type eg. Build, Package, BuildAndPackage, Default
 * @return [T:BuildInfo] https://jfrog.com/help/r/jfrog-pipelines-documentation/buildinfo
 */
public Object download(Map arg) {
    log.debug(arg)

    if (!arg.root) {
        assert (arg.containsKey("component")): "ERROR: artifacts.download : component not provided"
        assert (arg.containsKey("version")): "ERROR: artifacts.download : version not provided"
        assert (arg.containsKey("buildNumber")): "ERROR: artifacts.download : buildNumber not provided"
        assert (arg.containsKey("artifactoryInstance")): "ERROR: artifacts.download : artifactoryInstance not provided"
    } else {
        assert (arg.containsKey("root")): "ERROR: artifacts.download : root not provided"
        assert (arg.containsKey("credentialsId")): "ERROR: artifacts.download : credentialsId not provided"
    }
    assert (arg.containsKey("files")): "ERROR: artifacts.download : files not provided"

    def newBuildInfo

    def artifactoryServer
    if(arg.root) {
            artifactoryServer = Artifactory.newServer([
            url : arg.url,
            credentialsId : arg.credentialsId
        ])
        artifactoryServer.bypassProxy = true
    }
    else {
        artifactoryServer = Artifactory.server(arg.artifactoryInstance)
    }

    //The user can pass either a string or a list of files.
    //If the user passes a List, we can directly call the createUploadSpec
    //Else we'll tokenize and call the createUploadSpec function
    if (arg.files.class == String) {
        arg.files = arg.files.tokenize(',')
    }
    arg.files = arg.files.collect {
        it.trim().replace('\\', '/')
    }
    assert arg.files.size() > 0: "ERROR: artifacts.upload : arg.files is empty"

    boolean flat = arg.containsKey("flat") ? !!arg.flat : true
    boolean recursive = arg.containsKey("recursive") ? !!arg.recursive : true

    def downloadOpts = [:]
    downloadOpts.put("files", arg.files)
    if (!arg.root) {
        downloadOpts.put("component", arg.component)
        downloadOpts.put("version", arg.version)
        downloadOpts.put("buildNumber", arg.buildNumber)
    } else {
        downloadOpts.put("root", arg.root)
    }
    downloadOpts.put("target", arg.target)
    downloadOpts.put("flat", flat)
    downloadOpts.put("recursive", recursive)

    if (arg.containsKey("artifactType")) {
        downloadOpts.put("artifactType", arg.artifactType)
    }

    try {
        timeout(45) {
            retry(5) {
                def downloadSpec = this.createArtifactoryDownloadSpec(downloadOpts)
                log.info("Begin download of artifacts")
                newBuildInfo = artifactoryServer.download(downloadSpec)
                log.info("Artifacts downloaded")
            }
        }
    } catch (Throwable e) {
        radar.addBuildLog(
            "Error",
            "Artifactory_download",
            env.current_stage,
            artifactoryServer.url,
            e.message,
        )
        rethrow(e, 'Failed to download artifact', true)
    }

    return newBuildInfo
}

/**
 * Create Artifactory file spec from  a comma-seperated-value of filepaths that have to be downloaded from Artifactory
 *
 * <h4>Sample usage:</h4>
 * <pre>artifacts.createArtifactoryDownloadSpec([
 *              files          :  ["/path/to/file1", "/path/to/file2"],
 *              component      :  "Component-name",
 *              version        :  "version",
 *              buildNumber    :  "buildNumber",
 *              target         :  "target directory to download files"
 *         ])
 * </pre>
 *
 * @param files List of filepaths that have to be downloaded from Artifactory
 * @param component component name to download the files from
 * @param version component version
 * @param buildNumber build number to be downloaded
 * @param target Target location to which to download the files to.Defaults to current working directory.
 * @param artifactType ( optional ) Artifact type eg. Build, Package, BuildAndPackage, Default
 * @return String
 */
private String createArtifactoryDownloadSpec(Map arg) {
    log.debug(arg)

    assert arg.files: "ERROR: arg.files not provided"
    String repositoryFullName, root_pattern

    if(!arg.root && utils.isNullOrEmptyString(env.ARTIFACTORY_REPOSITORY_OVERRIDE) ){
        assert arg.component: "ERROR: arg.component not provided"
        assert arg.version: "ERROR: arg.version not provided"
        assert arg.buildNumber: "ERROR: arg.buildNumber not provided"
    }
    
    String filesArrStr = ""

    //Artifactory supports forward slash for all platforms
    arg.target = arg.target.replace('\\', '/')
    //Append a / to indicate the target is a directory. Otherwise, we'll run into permission issues as this tries to overwrite an existing dir.
    if (!arg.target.endsWith('/')) {
        arg.target += '/'
    }

    if(utils.isNullOrEmptyString(env.ARTIFACTORY_REPOSITORY_OVERRIDE)){
        if(!arg.root)
        {
            repositoryFullName = this.getRepositoryNameFromComponent(arg.component)
        }
    }
    else{
        repositoryFullName = env.ARTIFACTORY_REPOSITORY_OVERRIDE
    }

    def flat = arg.containsKey("flat") ? arg.flat : "true"
    def recursive = arg.containsKey("recursive") ? arg.recursive : "true"

    if(arg.root){
        root_pattern = arg.root
    }
    else if (!utils.isNullOrEmptyString(env.ARTIFACTORY_REPOSITORY_OVERRIDE)) {
        root_pattern =  env.ARTIFACTORY_REPOSITORY_OVERRIDE + "/b" + arg.buildNumber + "/"
    }
    else{
        root_pattern = repositoryFullName + "/" + arg.component + "_" + arg.version + "/b" + arg.buildNumber + "/"
    }

    for (int x = 0; x < arg.files.size(); x++) {
        log.debug(arg.files[x])
        filesArrStr += "{" +
                "\"pattern\": \"" + root_pattern + arg.files[x] + "\"," +
                "\"recursive\": \"" + recursive + "\"," +
                "\"target\":\"" + arg.target + "\"," +
                "\"flat\": \"" + flat + "\"" +
                (arg.containsKey("artifactType") ? ",\"props\": \"orbit.build.artifact.type=" + arg.artifactType + "\"" : "") +
                "}"
        "\"flat\": \"" + flat + "\"" +
                (arg.containsKey("artifactType") ? ",\"props\": \"orbit.build.artifact.type=" + arg.artifactType + "\"" : "") +
                "}"
        if (x < arg.files.size() - 1) {
            filesArrStr = filesArrStr + ","
        }
    }


    String ans = "{\"files\" :[" + filesArrStr + "]}"
    log.info("Created downloadSpec  = " + ans)
    return ans

}

/**
 * This functions returns an Artifactory Repository Name given a component name. Right now, repository naming convention is <component>-local
 * @param Name of the component
 * @return String full repository Name
 */
private String getRepositoryNameFromComponent(componentName) {
    Boolean testJob = env.ORBIT_TEST_JOB?.toBoolean() ?: false
    String repositoryName
    String repository_suffix = "-local"
    if (config('ORBIT_BUILD_ENV').toUpperCase() != "PROD" || testJob) {
        repositoryName = config('ARTIFACTORY_TEST_REPOSITORY')
    } else {
        repositoryName = componentName.toUpperCase()
        repositoryName = repositoryName + repository_suffix
    }
    return repositoryName
}

/**
 * Publish build information to Artifactory
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * artifacts.publishBuildInfo(buildInfo: buildInfo)
 * </pre>
 *
 * @param buildInfo Object - Typically returned from the Artifactory upload call
 */
private def publishBuildInfo(def arg) {
    if (env.SKIP_ARTIFACTORY_PUBLISH_BUILD_INFO?.toBoolean()){
        log.info("Skipping publishBuildInfo as SKIP_ARTIFACTORY_PUBLISH_BUILD_INFO is set to true.")
        return
    }
    def splitJobName = utils.splitJenkinsJobName(env.JOB_NAME)
    def buildInfo = arg.buildInfo
    def component = splitJobName.component
    def version = env.BUILDVERSION
    buildInfo.setName(component + '_' + version)
    buildInfo.setNumber(orbit.buildNumber())
    try {
        def artifactoryServer = Artifactory.server(env.ARTIFACTORY_INSTANCE)
        timeout(15) {
            retry(5) {
                log.info('Publishing build information to Artifactory')
                artifactoryServer.publishBuildInfo(buildInfo)
            }
        }
    }
    catch (Throwable e) {
        rethrow(e, 'Failed to publish build information to Artifactory', true)
    }
}

/**
 * Gets the artifact properties specific to the job
 *
 * <header>Sample Usage:</header>
 * <pre>this.getJobArtifactProps(artifactType: "Default")
 * </pre>
 *
 * @param artifactType Type of artifact eg. Build, Package, BuildAndPackage, Default
 * @return [T:String] Artifact properties
 */
private getJobArtifactProps(def artifactType)
{
        String properties = ""

        properties = "orbit.build.type=" + env.ORBIT_JOB_TYPE
        properties += ";orbit.build.artifact.type= " +artifactType
        switch (env.ORBIT_JOB_TYPE.toUpperCase()) {
            case ["DEFAULT", "BUILD"]:
                properties += ";orbit.build.number=" + orbit.buildNumberOnly()
                break
            case ["PACKAGE", "BUILDANDPACKAGE"]:
                properties += ";orbit.build.number=" + orbit.buildNumberOnly()
                properties += ";orbit.build.package.number=" + orbit.packageNumber()
                break
            default:
                //If the environment is set up correctly this should not happen but if not then throw an error
                error "Build type " + env.ORBIT_BUILD_TYPE + " not recognised, please review Orbit documentation."
                break
        }
        return properties
}

/**
 * Returns the information about the Artifactory repository.
 * Includes the repo url, path and instance name.
 */
private def getArtifactoryInfo() {
    def splitJobName = utils.splitJenkinsJobName(env.JOB_NAME)
    def component = splitJobName.component
    def version = env.BUILDVERSION
    def buildNumber = orbit.buildNumber()
    def repositoryFullName = this.getRepositoryNameFromComponent(component)
    def artifactoryServer = Artifactory.server(env.ARTIFACTORY_INSTANCE)
    String artifactoryPath
    String artifactsRoot
    String buildUrl_override
    if (utils.isNullOrEmptyString(env.ARTIFACTORY_REPOSITORY_OVERRIDE)) {
        artifactoryPath = artifactoryServer.url + "/" + repositoryFullName + "/" + component + "_" + version + "/b" + buildNumber + "/"
        buildUrl_override = component + "_" + version + "/" + buildNumber
        artifactsRoot = repositoryFullName + "/" + component + "_" + version + "/b" + buildNumber + "/"
    } else{
        artifactoryPath = artifactoryServer.url + "/" + env.ARTIFACTORY_REPOSITORY_OVERRIDE + "/b" + buildNumber + "/"
        buildUrl_override = env.ARTIFACTORY_REPOSITORY_OVERRIDE + "/" + buildNumber
        artifactsRoot = env.ARTIFACTORY_REPOSITORY_OVERRIDE + "/b" + buildNumber + "/"
    }
    def artifactoryInstance = radar.getArtifactoryInstanceByName(env.ARTIFACTORY_INSTANCE)
    def artifactoryBuildPath = artifactoryInstance["BuildUrlFormat"]
        .replace("{DOMAIN}", artifactoryInstance["Domain"])
        .replace("{OVERRIDE}", buildUrl_override)
    return [
        artifactoryURL: artifactoryPath,
        artifactoryBuildURL: artifactoryBuildPath,
        artifactsRoot: artifactsRoot,
        artifactoryInstanceName: env.ARTIFACTORY_INSTANCE,
        credentialId: artifactoryInstance.CredentialId,
    ]
}

/**
 * Sets the artifactory URL in Radar, if it hasn't been set already.
 * Fails the build if the URL cannot be set due to
 * another build having already written to the exact same uRL.
 */
private void setArtifactoryUrlInRadar() {
    if (!ArtifactoryState.isUrlSetInRadar) {
        ArtifactoryState.isUrlSetInRadar = true
        def info = getArtifactoryInfo()
        radar.setArtifactoryUrlInRadar(info)
    }
}
