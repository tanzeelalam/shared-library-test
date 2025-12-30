package org.mcafee

import groovy.transform.Field
import com.mcafee.orbit.Credentials.CredentialStore


/**
 * The main Orbit build object
 *
 * This object is an attempt minimise the size of Jenkinsfiles and create a maintainable code structure.
 *
 * See https://confluence.trellix.com/display/BaaS/Job+pipeline+template for more information.
 *
 * @param none
 */

@Field stashNumber = 0
@Field stashList = []
// @Field buildInfo = Artifactory.newBuildInfo()

// Add constructor to accept script context
orbitbuild(script) {
    this.steps = script
    this.buildInfo = script.Artifactory.newBuildInfo()  // ✅ Initialize here with script context
}

/**
 * Check out a GIT repository to the workspace.
 *
 * <h4>Sample usage</h4>
 * <pre>
 * def buildProfile = new org.mcafee.orbitbuild()
 * buildProfile.addGIT(
 *    project: 'trellix-utilities/orbit-template',
 *    instance: 'trellix',
 *    branch: 'master',
 *    tag: 'MFS-Supersonic.AAA.1.0.0.135',
 *    target: 'buildDir',
 *    lfsRepo: 'orbit_template_repo',
 * )
 * </pre>
 * @param project [T:String] Name of the project. The specified project name should be in the standard GitHub hierarchy, it should be preceded by organization name.
 * @param instance [T:String][OPTIONAL] Override GitHub instance: <code>"trellix"</code>, <code>"mcafee"</code> or <code>"shared"</code>. [DEFAULT:"trellix"]
 * @param branch [T:String][OPTIONAL] Name of the branch , eg: "master" or "branches/release_1.0". [DEFAULT:"master"]
 * @param tag [T:String][OPTIONAL] The tag to checkout. Over-rides the branch parameter. [DEFAULT:null]
 * @param target [T:String][OPTIONAL] Target directory for checking out the files. [DEFAULT:pwd()]
 * @param lfsRepo [T:String][OPTIONAL] Artifactory Git LFS repo name. [DEFAULT:null]
 * @param returnBom [T:Bool][OPTIONAL] Artifactory Git LFS repo name. [DEFAULT:false]
 * @param extensions [T:List][OPTIONAL] Over-ride for GIT extension configuration, as used in <code>checkout()</code>. Defaults to <code>null</code>. See "extensions" in "GitSCM" section at https://www.jenkins.io/doc/pipeline/steps/workflow-scm-step/
 * @return [T:String] The commit sha OR [T:Map] the SCM BOM response found in https://docs.orbit.corp.entsec.com/radar/prod/#/Bom/Bom_CodeExport
 */
def addGIT(Map arg) {
    arguments(arg) {
        addBoolean('returnBom', false)
        addString('project')
        addString('instance', 'trellix')
        addString('target', pwd()) /**Target defaults to the present working directory if one is not provided*/
        addString('branch', 'master')
        addString('lfsRepo', null)
        addString('tag', null)
        addList('extensions', [])
        parse()
    }
    log.info("Using git branch: " + arg.branch)
    return git.gitMain(arg)
}

/**
 * Add a maven dependency.
 *
 * @param option [T:String] The maven command to be run.
 * @param mavenDir [T:String] The path to directory to be used.
 * @param mavenHomePath [T:String] The maven installation to use.
 * @param parameter [T:String] [OPTIONAL] Parameter to append to the command.
 */
def addMVN(Map arg) {
    log.debug(arg)

    if (utils.isNullOrEmptyString(arg.option)) {
        log.info("arg.option not specified. Default to clean install.")
        arg.option = "clean install"
    }

    if (utils.isNullOrEmptyString(arg.parameter)) {
        log.info("arg.parameter not specified. Default to empty string")
        arg.parameter = " "
    }

    maven.mavenMain(arg)
}

/**
 * Downloads eCM build artifacts to your workspace and the records the dependency for Radar and reproducibility.
 *
 * <h4>Sample usage</h4>
 * <pre>
 * def buildProfile = new org.mcafee.orbitbuild()
 * buildProfile.addECM(
 *    project: 'VSCORE',
 *    version: '15.7.0',
 *    buildState: 'Released WHQL-signed to QA (RTQA_WHQL)',
 *    packageNumber: "latest",
 *    overrideVersionOnly: true,
 *    files: ["VSCore*.zip": "dependencies\\vscore_packages\\"],
 * )
 *</pre>
 *
 * @param project [T:String] The specified project name used in the eCM Build console.
 * @param version [T:String] Name of the version, e.g.: "2.2.0".
 * @param files [T:Map] Single or wildcard files and the relative location they are to be downloaded into
 *          e.g. ("Package_" + &lt;packagenumber&gt; + "\\AAC_SDK*.zip") : "dependencies\\vscore_packages\\"
 * @param buildNumber [T:String] [OPTIONAL] The number of a specific builds to use.
 * @param configType [T:String] [OPTIONAL] This is the eCM 'Config Type' build parameter which can be used when
 *          searching for a build in eCM. If a valid match with the other arguments is not found, it will fail.
 * @param packageNumber [T:String] [OPTIONAL] The number of a specific package to use.
 * @param overrideVersionOnly [T:boolean] [OPTIONAL] If the project contains only the version instead of major.minor.subminor, then set this variable as true. [DEFAULT:false]
 * @param buildState [T:String] [OPTIONAL] The exact eCM milestone string e.g., <code>"Released WHQL-signed to QA (RTQA_WHQL)"</code>
 * @param releaseType [T:String] [OPTIONAL] The exact eCM release type string e.g., <code>"Main"</code>.
 * @param promotionStateNumber [T:String] [OPTIONAL]  Specify a promotion state number to use as a filter.
 * @param buildOverride [T:String] [OPTIONAL] Provide an eCM single string as a build override,
 *      <code>&lt;version&gt;.&lt;build&gt;.&lt;package&gt;</code>
 *          e.g., 15.7.0.450.4. Note: &lt;package&gt; is optional.
 * @param recursive [T:boolean] [OPTIONAL] If <code>true</code>, dependency will be downloaded recursively
 *          from ecm, downloading subfolders and empty folders. <code>false</code> will skip subfolders. [DEFAULT:true]
 * @return [T:Map] eCM build properties and api responses.
 */
public Map addECM(Map arg) {
    if (arg.recursive == null) {
        arg.recursive = true
    }
    boolean overrideBuildVersion = arg.containsKey("overrideVersionOnly") ? arg.overrideVersionOnly.toBoolean() : false

    if (utils.isNullOrEmptyInMap(arg)) {
        log.error("Null value found in call to addECM: ${arg}")
        error 'Failed to add ECM dependency'
    }

    //deal with buildOverrides
    if (arg.buildOverride) {
        log.info(
                "buildOverride arguement provided:${arg.buildOverride}. " +
                "This will replace version {${arg.version}}, buildNumber " +
                "(${arg.buildNumber}), and packageNumber (${arg.packageNumber})"
        )
        if(overrideBuildVersion) {
            log.debug("Project Version is not a regular version")
            def buildOverride = arg.buildOverride.split(/\./)
            log.debug("buildOverride.size(): " + buildOverride.size())
            if (buildOverride.size() >=2 && buildOverride.size() <=3) {
                arg.version = buildOverride[0]
                arg.buildNumber = buildOverride[1]
                if (buildOverride.size() == 3) {
                    arg.packageNumber = buildOverride[2]
                }
            } else {
                error "ERROR : invalid eCM dependency buildOverride string detected: " +buildOverride + " Please use the correct arg"
            }
        } else {
            def eCMBuildOverRide = ecm.splitECMOverRide(arg.buildOverride)
            arg.version = eCMBuildOverRide.version
            arg.buildNumber = eCMBuildOverRide.buildNumber
            if (eCMBuildOverRide.isPackage) {
                arg.packageNumber = eCMBuildOverRide.packageNumber
            }
        }

        // setting promotionStateNumber to be null, since this eCM Dependency will now use a
        // specific build override
        arg.promotionStateNumber = null
        log.info(
            "New values after buildOverride: version {${arg.version}}," +
            " buildNumber (${arg.buildNumber}), and packageNumber (${arg.packageNumber})"
        )
    }

    //eCM Package
    if (arg.packageNumber) {
        arg.buildType = "1"
        //eCM API does not support latest, but an empty value is the equivalent.
        if (arg.packageNumber.toLowerCase() == "latest") {
            log.info("orbitbuild: attempting to find latest eCM package job.")
            arg.packageNumber = ""
        }
    } else {
        arg.buildType = "0"
    }

    def eCMApiResponse = ["text": "ERROR: NO ECM API RESPONSE GIVEN FOR ${arg.toString()}"] //DEFAULT
    eCMApiResponse = ecm.getBuildOrPackageJobDetails(arg) //text property is the string response
    log.debug("ecmDetails: " + eCMApiResponse.text)

    if (arg.configType) {
        eCMApiResponse.CONFIGTYPE = arg.configType
    }

    //map of data to be stored in Radar CMDB to be used for reproducibility
    def bomRecord = [
            ecmMasterId   : eCMApiResponse.MASTERID,
            ecmProjectName: eCMApiResponse.PROJECT,
            ecmBuildNumber: eCMApiResponse.BUILDNUMBER,
            ecmVersion    : eCMApiResponse.VERSION
    ]

    def eCMDownload = arg + [
            project      : eCMApiResponse.PROJECT,
            version      : eCMApiResponse.VERSION,
            language     : eCMApiResponse.LANGUAGE,
            build        : eCMApiResponse.BUILDNUMBER,
            packageNumber: eCMApiResponse.PACKAGENUMBER,
            configType   : eCMApiResponse.CONFIGTYPE
    ]
    eCMDownload.putAll(eCMApiResponse)

    if (eCMApiResponse.BUILDTYPE == "BUILD") {
        eCMDownload.put("server", eCMApiResponse.OBJECT_PATH.tokenize("\\").first().tokenize(".").first().toUpperCase())
        eCMDownload.put("objectPath", eCMApiResponse.OBJECT_PATH) //TODO: handle SOURCE_PATH or a parent root
    } else {
        eCMDownload.put("server", eCMApiResponse.PACKAGE_PATH.tokenize("\\").first().tokenize(".").first().toUpperCase())
        eCMDownload.put("packagePath", eCMApiResponse.PACKAGE_PATH)
        eCMDownload.put("packageFile", eCMApiResponse.PACKAGE_FILE)
        bomRecord.put("ecmPackageNumber", eCMApiResponse.PACKAGENUMBER)
    }

    //Allow customer to target files that are at the build or package root
    if (arg.root) {
        if (eCMApiResponse.OBJECT_PATH) {
            eCMDownload.objectPath = "\\\\" + eCMApiResponse.OBJECT_PATH.tokenize("\\").dropRight(1).join("\\") + arg.root
        } else {
            eCMDownload.packagePath = "\\\\" + eCMApiResponse.PACKAGE_PATH.tokenize("\\").dropRight(1).join("\\") + arg.root
        }
    }

    try {
        ecm.downloadDependencyFromBuildMasterOrPackageMaster(eCMDownload)
    }
    catch (Throwable e) {
        rethrow(e, 'Failed to download eCM dependency')
    }
    bom.eCMBOM(bomRecord)

    return eCMApiResponse
}

/**
 * Adds an Orbit build to the project
 *
 * <h4>Sample usage</h4>
 * <pre>
 * def buildProfile = new org.mcafee.orbitbuild()
 * buildProfile.addOrbitBuild(
 *    buildOverride: env.DATAUPLOADER_OVERRIDE,
 *    project: "Generic_Tools",
 *    component: "dataUploader",
 *    componentVersion: "1.0.0",
 *    targetLocation: "dependencies\\dataUploader\\",
 * )
 * </pre>
 * @param project [T:String] Name of the Project
 * @param component [T:String] Name of the component
 * @param componentVersion [T:String] Version of the component
 * @param files [T:String] [OPTIONAL] Contains wildcard files and the relative location they are to be downloaded into. [DEFAULT:"**"]
 * @param buildNumber [T:String] [OPTIONAL] The number of a specific builds to use.
 * @param packageNumber [T:String] [OPTIONAL] The number of a specific package to use.
 * @param packageType [T:String] [OPTIONAL]  The number of a specific builds to use.
 * @param milestone [T:String] [OPTIONAL] Filter eCM milestone string e.g., <code>"SMOKE-TEST PASSED"</code>. [DEFAULT:"BUILD PASSED"]
 * @param useExactMilestone [T:boolean] [OPTIONAL] use exact milestone instead of using it as a filter. [DEFAULT:true]
 * @param targetLocation  [T:String] [OPTIONAL] The target location for the files relative to workspace. [DEFAULT:env.WORKSPACE]
 * @param artifactType [T:String] [OPTIONAL] Type of artifacts to download: <code>"Build"</code>, <code>"Package"</code>, <code>"Default"</code> or <code>"BuildAndPackage"</code>.
 * @param milestone [T:String] [OPTIONAL] Filter eCM milestone string e.g., <cocde>"Released WHQL-signed to QA (RTQA_WHQL)"</code>.
 * @param useExactMilestone [T:boolean] [OPTIONAL]  use exact milestone instead of using it as a filter. [DEFAULT:true]
 * @param buildOverride [T:String] [OPTIONAL] String used to describe the version and build number. Version and build number.
 *                        are "," separated, with an optional "," separated package number. e.g. 19.1.0,222(,2).
 * @param artifactType [T:String] [OPTIONAL] Type of artifacts to download eg. <code>"Build"</code>, <code>"Package"</code>, <code>"Default"</code> or <code>"BuildAndPackage"</code>.
 * @param flat [T:boolean] [OPTIONAL] Flatten directory structure after download. [DEFAULT:true]
 * @param recursive [T:boolean] [OPTIONAL] Recursively search for and download the specified file patterns. [DEFAULT:true]
 * @return [T:Map]
 */
public Map addOrbitBuild(Map arg) {
    arg['jobType'] = "Build"

    return this.addOrbit(arg)
}

/**
 * Adds an Orbit package to the project
 * <h4>Sample usage</h4>
 * <pre>
 * def buildProfile = new org.mcafee.orbitbuild()
 * buildProfile.addOrbitPackage(
 *     buildOverride: env.DATAUPLOADER_OVERRIDE,
 *     project: "Generic_Tools",
 *     component: "dataUploader",
 *     componentVersion: "1.0.0",
 *     targetLocation: "dependencies\\dataUploader\\",
 * )
 * </pre>
 * @param project [T:String] Name of the Project
 * @param component [T:String] Name of the component
 * @param componentVersion [T:String] Version of the component
 * @param files [T:String] [OPTIONAL] Contains wildcard files and the relative location they are to be downloaded into. [DEFAULT:"**"]
 * @param buildNumber [T:String] [OPTIONAL] The number of a specific builds to use.
 * @param packageNumber [T:String] [OPTIONAL] The number of a specific package to use.
 * @param packageType [T:String] [OPTIONAL]  The number of a specific builds to use.
 * @param milestone [T:String] [OPTIONAL] Filter eCM milestone string e.g., <code>"SMOKE-TEST PASSED"</code>. [DEFAULT:"BUILD PASSED"]
 * @param useExactMilestone [T:boolean] [OPTIONAL] use exact milestone instead of using it as a filter. [DEFAULT:true]
 * @param targetLocation  [T:String] [OPTIONAL] The target location for the files relative to workspace. [DEFAULT:env.WORKSPACE]
 * @param artifactType [T:String] [OPTIONAL] Type of artifacts to download: <code>"Build"</code>, <code>"Package"</code>, <code>"Default"</code> or <code>"BuildAndPackage"</code>.
 * @param milestone [T:String] [OPTIONAL] Filter eCM milestone string e.g., <cocde>"Released WHQL-signed to QA (RTQA_WHQL)"</code>.
 * @param useExactMilestone [T:boolean] [OPTIONAL]  use exact milestone instead of using it as a filter. [DEFAULT:true]
 * @param buildOverride [T:String] [OPTIONAL] String used to describe the version and build number. Version and build number.
 *                        are "," separated, with an optional "," separated package number. e.g. 19.1.0,222(,2).
 * @param artifactType [T:String] [OPTIONAL] Type of artifacts to download eg. <code>"Build"</code>, <code>"Package"</code>, <code>"Default"</code> or <code>"BuildAndPackage"</code>.
 * @param flat [T:boolean] [OPTIONAL] Flatten directory structure after download. [DEFAULT:true]
 * @param recursive [T:boolean] [OPTIONAL] Recursively search for and download the specified file patterns. [DEFAULT:true]
 * @return [T:Map]
 */
public Map addOrbitPackage(Map arg) {
    arg['jobType'] = "Package"

    return this.addOrbit(arg)
}

/**
 * Adds an Orbit build or package to project
 * <h4>Sample usage</h4>
 * <pre>
 * def buildProfile = new org.mcafee.orbitbuild()
 * buildProfile.addOrbit(
 *     buildOverride: env.DATAUPLOADER_OVERRIDE,
 *     project: "Generic_Tools",
 *     component: "dataUploader",
 *     componentVersion: "1.0.0",
 *     targetLocation: "dependencies\\dataUploader\\",
 * )
 * </pre>
 * @param project [T:String] Name of the Project
 * @param component [T:String] Name of the component
 * @param componentVersion [T:String] Version of the component
 * @param files [T:String] [OPTIONAL] Contains wildcard files and the relative location they are to be downloaded into. [DEFAULT:"**"]
 * @param buildNumber [T:String] [OPTIONAL] The number of a specific builds to use.
 * @param packageNumber [T:String] [OPTIONAL] The number of a specific package to use.
 * @param packageType [T:String] [OPTIONAL]  The number of a specific builds to use.
 * @param milestone [T:String] [OPTIONAL] Filter eCM milestone string e.g., <code>"SMOKE-TEST PASSED"</code>. [DEFAULT:"BUILD PASSED"]
 * @param useExactMilestone [T:boolean] [OPTIONAL] use exact milestone instead of using it as a filter. [DEFAULT:true]
 * @param targetLocation  [T:String] [OPTIONAL] The target location for the files relative to workspace. [DEFAULT:env.WORKSPACE]
 * @param artifactType [T:String] [OPTIONAL] Type of artifacts to download: <code>Bui"ld</code>, <code>"Package"</code>, <code>"Default"</code> or <code>"BuildAndPackage"</code>.
 * @param milestone [T:String] [OPTIONAL] Filter eCM milestone string e.g., <cocde>Released WHQL-signed to QA (RTQA_WHQL)</code>.
 * @param useExactMilestone [T:boolean] [OPTIONAL]  use exact milestone instead of using it as a filter. [DEFAULT:true]
 * @param buildOverride [T:String] [OPTIONAL] String used to describe the version and build number. Version and build number.
 *                        are "," separated, with an optional "," separated package number. e.g. 19.1.0,222(,2).
 * @param artifactType [T:String] [OPTIONAL] Type of artifacts to download eg. <code>"Build"</code>, <code>"Package"</code>, <code>"Default"</code> or <code>"BuildAndPackage"</code>.
 * @param jobType [T:String] [OPTIONAL] Used to filter jobs in Orbit: <code>"Build"</code>, <code>"Package"</code>, <code>"Default"</code> or <code>"BuildAndPackage"</code>. [DEFAULT:"Build"]
 * @param flat [T:boolean] [OPTIONAL] Flatten directory structure after download. [DEFAULT:true]
 * @param recursive [T:boolean] [OPTIONAL] Recursively search for and download the specified file patterns. [DEFAULT:true]
 * @return [T:Map]
 */
public Map addOrbit(Map arg) {
    log.debug(arg)
    String project, component, componentVersion
    String milestone, targetLocation
    String useExactMilestone
    String buildNumber, packageNumber, packageType
    String jobType
    String artifactType
    boolean flat, recursive
    Map dependencyDetails = utils.convertMapValuesToTypeString(arg)

    //account for the usage of product instead of project
    if (dependencyDetails.product && !dependencyDetails.project) {
        dependencyDetails.project = dependencyDetails.product
    }

    //Mandatory params
    assert !utils.isNullOrEmptyString(dependencyDetails.project):
            "ERROR: project value is empty."
    assert !utils.isNullOrEmptyString(dependencyDetails.component):
            "ERROR: component value is empty."
    assert !utils.isNullOrEmptyString(dependencyDetails.componentVersion):
            "ERROR: componentVersion value is empty."

    //assign to method variables
    project = dependencyDetails.project.replaceAll('_', ' ')
    component = dependencyDetails.component
    componentVersion = dependencyDetails.componentVersion
    files = !(dependencyDetails.files) ? "**" : dependencyDetails.files
    

    //Optional params which require default values
    useExactMilestone = utils.isNullOrEmptyString(dependencyDetails.useExactMilestone) ? "true" :
            dependencyDetails.useExactMilestone

    targetLocation = utils.isNullOrEmptyString(dependencyDetails.targetLocation) ? env.WORKSPACE :
            dependencyDetails.targetLocation

    milestone = utils.isNullOrEmptyString(dependencyDetails.milestone) ? 'BUILD PASSED' :
            dependencyDetails.milestone.toUpperCase()

    jobType = utils.isNullOrEmptyString(dependencyDetails.jobType) ? 'Build' :
            dependencyDetails.jobType

    buildNumber = utils.isNullOrEmptyString(dependencyDetails.buildNumber) ? "" :
            dependencyDetails.buildNumber

    packageNumber = utils.isNullOrEmptyString(dependencyDetails.packageNumber) ? "" :
            dependencyDetails.packageNumber

    packageType = utils.isNullOrEmptyString(dependencyDetails.packageType) ? "" :
            dependencyDetails.packageType

    artifactType = utils.isNullOrEmptyString(dependencyDetails.artifactType) ? "" :
            dependencyDetails.artifactType

    flat = true
    if (arg.containsKey('flat')) {
        flat = !!arg.flat
    }
    recursive = true
    if (arg.containsKey('recursive')) {
        recursive = !!arg.recursive
    }

    //error if package number provided but job type is of type build
    if (jobType.toUpperCase() == 'BUILD' && packageNumber != ""){
        error "${env.STAGE_NAME} - Please check jenkinsfile and orbitbuild.addOrbit() groovydocs," +
                " a package number was provided with a job of type build. These parameters can not " +
                "be used together, as build jobs do not contain package numbers."
    }

    //build override
    if (dependencyDetails.buildOverride) {
        log.info("Build override detected for ${dependencyDetails.project}: ${dependencyDetails.buildOverride}")

        Map orbitBuildOverride = utils.splitOrbitOverride(dependencyDetails.buildOverride)
        componentVersion = orbitBuildOverride.component ?: componentVersion
        buildNumber = orbitBuildOverride.build ?: buildNumber
        packageNumber = orbitBuildOverride.package ?: ''
        useExactMilestone = false
        log.info("Using componentVersion" + componentVersion +
                "\nbuildNumber " + buildNumber +
                "\npackageNumber " + packageNumber)
    }

    if (dependencyDetails.get("buildNumber")?.contains('.') &&
            dependencyDetails.jobType?.toUpperCase() == 'BUILD') {
        error "${env.STAGE_NAME} addOrbit() - " +
                "Attempting to search for an Orbit job of type 'build' with a build number " +
                "that contains an embedded package number or an invalid character '.'"
    }

        def receivedProps = radar.buildSearch(
            projectName: project,
            componentName: component,
            componentVersion: componentVersion,
            milestone: milestone,
            buildNumber: buildNumber,
            type: jobType,
            packageType: packageType,
            packageNumber: packageNumber,
            useExactMilestone: useExactMilestone,
        )
        String orbitDependencyRadarBuildRecordID = receivedProps["BuildRecordID"]
        String orbitDependencyBuildNumberOnly = receivedProps["BuildNumber"]
        String orbitDependencyPackageNumber = receivedProps["PackageNumber"]
        String orbitDependencyArtifactoryServer = receivedProps["ArtifactoryServer"]
        String orbitDependencyArtifactsRoot = receivedProps["ArtifactsRoot"]
        String orbitDependencyArtifactoryServerCredentialId = receivedProps["ArtifactoryServerCredentialId"]
        String orbitDependencyBuildNumber = orbitDependencyBuildNumberOnly + (
                !utils.isNullOrEmptyString(orbitDependencyPackageNumber) ?
                        ".${orbitDependencyPackageNumber}" : "")

        log.debug("orbitDependencyBuildNumber: ${orbitDependencyBuildNumber}")
        log.debug("orbitDependencyRadarBuildRecordID: ${orbitDependencyRadarBuildRecordID}")

        this.downloadFromArtifactory(
            component: component,
            version: componentVersion,
            buildNumber: orbitDependencyBuildNumber,
            target: targetLocation,
            artifactType: artifactType,
            files: files,
            flat: flat,
            recursive: recursive,
            root: orbitDependencyArtifactsRoot,
            url: orbitDependencyArtifactoryServer,
            credentialsId: orbitDependencyArtifactoryServerCredentialId
        )

        //record orbit dependency to Radar BOM.
        bom.orbitBOM([
            dependencyBuildRecordId: orbitDependencyRadarBuildRecordID
        ])
        Map returnMap = [
                BUILDNUMBER  : orbitDependencyBuildNumberOnly,
                VERSION      : componentVersion,
                PACKAGENUMBER: orbitDependencyPackageNumber
        ]

        return returnMap
}

/**
 * Transfers a file to jenkins master for Upload to artifactory. Invoke this method on the files that you have to upload to Artifactory.
 * This function only stashes the files on master. The actual upload to artifactory is done only through the orbitbuild.uploadToArtifactory function.
 * Typically, a build has multiple stashForUpload calls which is then followed by a single  uploadToArtifactory Call.
 *
 * @param includes [T:String] Ant-style expression for files to be included in the stash.
 * @param target [T:String] [OPTIONAL] Sub-path to upload this set of files to.
 *        During upload to artifactory, these files are uploaded at this path relative to the artifactory default project upload path.
 * @param artifactType [T:String] [OPTIONAL] type of Artifact eg. Build, Package.
 * @param catchFail [T:boolean] [OPTIONAL] Will not fail a build if stashing is unsuccessful. [DEFAULT:false]
 * @return [T:String] The name of the stash.
 */
String stashForUpload(Map arg) {
    assert (arg && arg.includes): "ERROR: stash files : arg.includes not provided"
    
    try {
        this.stashNumber = this.stashNumber + 1
        def stashName = "stash" + this.stashNumber
        def filesToBeStashed = arg.includes
        if (!(filesToBeStashed instanceof String) && !(filesToBeStashed instanceof GString)) {
            filesToBeStashed = filesToBeStashed.join(',')
        }
        filesToBeStashed = filesToBeStashed.replace('\\', '/')

        log.debug("filesToBeStashed :  " + filesToBeStashed)

        stash name: stashName, includes: filesToBeStashed
        def stashInfo = [:]

        stashInfo.put("name", stashName)
        if (arg.containsKey("target") && !utils.isNullOrEmptyString(arg.target)) {
            stashInfo.put("target", arg.target)
        }

        if (arg.containsKey("artifactType") && !utils.isNullOrEmptyString(arg.artifactType)) {
            stashInfo.put("artifactType", arg.artifactType)
        }

        this.stashList.push(stashInfo)

        return stashName

        }
    catch(Throwable e)
    {
        log.warn("Failed to stash")

        if(!arg.catchFail){
            rethrow(e)
        }
    }
}


/**
 * Downloads artifacts to your workspace from Artifactory.
 * <h4>Sample usage</h4>
 * <pre>
 * def buildProfile = new org.mcafee.orbitbuild()
 * buildProfile.addArtifactory(
 *    files: ["babelfish-0.1-1.noarch.rpm"],
 *    root: "ISecG-MFE-local/McAfee/",
 *    target: "BEAART",
 * )
 * </pre>
 * @param root [T:String] The artifcatory root relative to the base URL.
 * @param files [T:List&lt;String&gt;] [OPTIONAL] List of files to download. [DEFAULT:"**"]
 * @param target [T:String] [OPTIONAL] location to which to download files to. [DEFAULT:env.WORKSPACE]
 * @param instance [T:String] [OPTIONAL] The artifactory instance to download from. [DEFAULT:env.ARTIFACTORY_INSTANCE]
 * @param flat [T:boolean] [OPTIONAL] Flattens directory structure on downland. [DEFAULT:true]
 * @param recursive [T:boolean] [OPTIONAL] Recursively search the path for specified file patterns. [DEFAULT:true]
 */
def addArtifactory(Map arg) {
    arg.files = arg.files ?: ["**"] //default of all files
    assert arg.files && arg.files instanceof List: "ERROR: files is empty."
    assert !utils.isNullOrEmptyString(arg.root): "ERROR: root is empty."
    String artifactoryInstanceName = arg.instance?: env.ARTIFACTORY_INSTANCE
    def artifactoryInstance = radar.getArtifactoryInstanceByName(artifactoryInstanceName)
        
    arg.put('credentialsId', artifactoryInstance["CredentialId"])
    arg.put('url', artifactoryInstance["Url"])

    this.downloadFromArtifactory(arg)

    radar.createArtifactoryDependency(
        artifactoryInstanceName,
        arg.root,
        arg.files,
    )
}

/**
 * This function uploads stashed files from the build to artifactory.
 * It must be used only once per build.
 * Use <code>orbitbuild.stashForUpload()</code> to mark files for upload.
 */
void uploadToArtifactory() {
    def i

    def outputDirName = "artifactory_upload"

    dir(outputDirName) {
        //unstash
        for (i = 0; i < this.stashList.size(); i++) {
            print "unstash " + stashList[i].name
            if (stashList[i].containsKey("target")) {
                dir(stashList[i].target) {
                    unstash stashList[i].name
                }
            } else {
                unstash stashList[i].name
            }
        }
    }

    //Upload
    dir(outputDirName) {
        def newBuildInfo = artifacts.upload([
                files    : "**",
                flat     : "false",
                recursive: "true",
                artifactType: env.ORBIT_JOB_TYPE
        ])
        this.buildInfo.append(newBuildInfo)
    }
}

/**
 * This function uploads stashed files from the build to artifactory using the artifact type. This function will be used once per type
 * @param type [T:String] <code>"Build"</code>, <code>"Package"</code>, etc...
 */
void uploadToArtifactoryByType(Map arg) {
    def i

    String type = !utils.isNullOrEmptyString(arg.type) ? arg.type : "";

    assert type != "" : "Mandatory argument type not provided!"

    def outputDirName = "artifactory_upload_" + type

    dir(outputDirName) {
        //unstash
        for (i = 0; i < this.stashList.size(); i++) {
            if(!stashList[i].containsKey("artifactType") || stashList[i].artifactType != type)
                continue

            log.debug("unstash ${stashList[i].name}")
            if (stashList[i].containsKey("target")) {
                dir(stashList[i].target) {
                    unstash stashList[i].name
                }
            } else {
                unstash stashList[i].name
            }
            //Already unstashed so mark for removal
            stashList[i].put("removeFlag", 1)
        }
    }
    //Clean up stash list
    stashList.removeAll {it.containsKey("removeFlag") && it.removeFlag == 1}

    //Upload
    dir(outputDirName) {
        def newBuildInfo = artifacts.upload([
                files    : "**",
                flat     : "false",
                recursive: "true",
                artifactType: type
        ])
        this.buildInfo.append(newBuildInfo)
    }
}

/**
 * This function downloads the specified component from artifactory.
 *
 * @param component The component name to download.
 * @param version The component version to download.
 * @param buildNumber The build number to download.
 * @param artifactoryInstance [OPTIONAL] The Artifactory instance to use.
 * @param flat [OPTIONAL] Flattens directory structure on downland. [DEFAULT:true]
 * @param files [OPTIONAL]Files to download. [DEFAULT:"**"]
 * @param recursive [OPTIONAL] Recursively search the path for specified file patterns. [DEFAULT:true]
 * @param artifactType [OPTIONAL] Type of artifacts to download eg. <code>"Build"</code>, <code>"Package"</code>, <code>"Default"</code> or <code>"BuildAndPackage"</code>
 * @param target [OPTIONAL] location to which to download files to. [DEFAULT:env.WORKSPACE]
 */
private def downloadFromArtifactory(Map arg) {
    log.debug(arg)

    if (utils.isNullOrEmptyInMap(arg)) {
        log.error("Null value found in call to downloadFromArtifactory: ${arg}")
        error 'Failed to download dependency from artifactory'
    }

    if (!arg.root) {
        assert (!utils.isNullOrEmptyString(arg.component)): "Error orbitbuild.downloadFromArtifactory : component not specified"
        assert (!utils.isNullOrEmptyString(arg.version)): "Error orbitbuild.downloadFromArtifactory : version not specified"
        assert (!utils.isNullOrEmptyString(arg.buildNumber)): "Error orbitbuild.downloadFromArtifactory : buildNumber not specified"
        assert (!utils.isNullOrEmptyString(arg.artifactoryInstance)): "Error orbitbuild.downloadFromArtifactory : artifactoryInstance not specified"
    } else {
        assert (!utils.isNullOrEmptyString(arg.root)): "Error orbitbuild.downloadFromArtifactory : root not specified"
        assert (!utils.isNullOrEmptyString(arg.credentialsId)): "Error orbitbuild.downloadFromArtifactory : credentialsId not specified"
    }

    String target = arg.containsKey("target") ? arg.target.toString() : env.WORKSPACE.toString()
    String artifactType = arg.containsKey("artifactType") ? arg.artifactType.toString() : ""
    Map downloadOpts = [:]

    downloadOpts.put("target", target)

    if(!arg.root) {
        downloadOpts.put("component", arg.component)
        downloadOpts.put("version", arg.version)
        downloadOpts.put("buildNumber", arg.buildNumber)
        downloadOpts.put("artifactoryInstance", arg.artifactoryInstance)
    } else {
        downloadOpts.put("root", arg.root)
        downloadOpts.put("credentialsId", arg.credentialsId)
        downloadOpts.put("url", arg.url)
    }
    arg.containsKey("files") ? downloadOpts.put("files", arg.files) : downloadOpts.put("files", "**")
    arg.containsKey("flat") ? downloadOpts.put("flat", arg.flat) : "true"
    arg.containsKey("recursive") ? downloadOpts.put("recursive", arg.recursive) : "true"
    if (!utils.isNullOrEmptyString(artifactType)) {
        downloadOpts.put("artifactType", artifactType)
    }

    def newBuildInfo = artifacts.download(downloadOpts)
    this.buildInfo.append(newBuildInfo)
}
/**
 * This function publishes build information to artifactory.
 */
def publishBuildInfo() {
    artifacts.publishBuildInfo([
        buildInfo: this.buildInfo,
    ])
}

/**
 * Check out the git repo and returns the commit sha of the current build.
 * <h4>Sample usage</h4>
 * <pre>
 * def buildProfile = new org.mcafee.orbitbuild()
 * buildProfile.checkoutBuildSHA("my_test_directory")
 * </pre>
 * @param directory [T:String] Name of the directory to checkout the commit sha into. If empty string is passed in, it will checkout repo to job level.
 * @return [T:String] The commit sha.
 */
String checkoutBuildSHA(String directory) {
    utils.checkoutBuildSHA(directory)
}
