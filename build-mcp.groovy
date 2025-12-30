#!Groovy
/* groovylint-disable CompileStatic, DuplicateNumberLiteral, DuplicateStringLiteral, ElseBlockBraces, IfStatementBraces, LineLength, UnnecessaryGString */
/** Refactored: Amit Prajapati */
/* *************************************************************************************************
   Basic Orbit Jenkinsfile Template
   Job Type: Default
   *************************************************************************************************
*/
// Create an Orbit build object [Mandatory]
buildProfile = new org.mcafee.orbitbuild()
//Orbit job type


env.EMAIL_ONLY_ON_FAILURE = false

// Create Job properties. See examples: https://jenkins.io/doc/book/pipeline/syntax/
properties(
    [
        // Parameters block - Handles the user defined parameters in the Jenkins UI.
        parameters(
        [
            string(defaultValue: "w2k16-latest", description: "Project Build Node Label", name: "PROJECT_NODE_LABEL"),
            choice(choices: ['4.9.4', '4.9.3','4.9.2','4.9.1','4.9.0','4.8.0'].join("\n"), description: 'Version ?', name: 'MCP_VERSION'),
            choice(choices: ['BuildAndPackage', 'Build', 'Package'].join("\n"), description: 'Job Type', name: "JOB_TYPE"),
            choice(choices: ['2019', '2022', '2010'].join("\n"), description: 'Visual Studio Version', name: "VISUAL_STUDIO_VERSION"),
            string(defaultValue: "", description: "Build Number to be packaged/re-packaged", name: "BUILD_TO_PACKAGE"),
            booleanParam(defaultValue: false, description: "Extension Only", name: "extensionOnly"),
            string(defaultValue: "master", description: "Branch to build", name: "branchOverride"),
            string(defaultValue: "master", description: "MCP External libs branch", name: "mcpExtLibs"),
            string(defaultValue:'24.11.0,325,3', description: "SYSCORE-SDK Build# (24.11.0,325,3)", name: "SysCoreBuild"),
            string(defaultValue:'5.7.7,435,1', description: "MA-SDK Build# (5.7.7,435,1)", name: "MA_SDKOverrride"),
            string(defaultValue:'10.7.0,5162,1', description: "BLFramework Build# (10.7.0,5162,1)", name: "HostCommon"),
            string(defaultValue:'186', description: "DRA Build# (186)", name: "DRA_BUILD_NO"),
            booleanParam(defaultValue: false, description: "Build MCP-Client Migration OnPremePO", name: "MCPClientMigrationOnPremePO"),
            booleanParam(defaultValue: false, description: "Build on-Prem ePO Extension", name:"onPremEPO"),
            booleanParam(defaultValue: false, description: "Build MVISION ePO Extension", name:"mvisionEPO"),
            booleanParam(defaultValue: false, description: "Build Cloud ePO Extension?", name:"cldExtnEPO"),
            booleanParam(defaultValue: false, description: "Create a Coverity Build", name: "COVERITY_BUILD"),
            booleanParam(defaultValue: false,description: "Create a Bullseye Build",name: "BULLSEYE_BUILD"),
            booleanParam(defaultValue: false, description: "Run API Scaner", name:"ApiScanner"),
            booleanParam(defaultValue: true, description: "Musarubra Certificate?", name:"FutureCertificate"),
			booleanParam(defaultValue: true, description: "Support Tool Release Build", name:"SupportToolReleaseBuild"),
            choice(choices: "2\n1\n0\n3\n4", description: "Log Level", name:"DEBUGLEVEL"),
            string(defaultValue: "", description: "Job notifications email address (comma seperated)", name: "Email_ID"),

        ]),
        buildDiscarder(
            logRotator(
                artifactDaysToKeepStr: '2',
                artifactNumToKeepStr: '1',
                daysToKeepStr: '5',
                numToKeepStr: '2'
            )
        )
    ]
)
/* *************************************************************************************************
     ORBIT Job Steps
     1. Set up job variables
     2. Prebuild stage [MANDATORY]
     3. Compile stage
     4. Prepackage stage
     5. Packaging stage
     6. Delivery stage [MANDATORY]
   *************************************************************************************************
*/

/**
 * Detect buildType and set appropiate varibles
 */
env.EMAIL_DL = params.Email_ID
env.BUILDVERSION = params.MCP_VERSION
env.ORBIT_JOB_TYPE = params.JOB_TYPE
if (params.JOB_TYPE == "Package") {
    echo message: "[MCP-DEBUG] Build type is Package and BUILD_TO_PACKAGE is: " + params.BUILD_TO_PACKAGE
    if (!utils.isNullOrEmptyString(params.BUILD_TO_PACKAGE))
        env.buildToPackage = params.BUILD_TO_PACKAGE.split('\\.')[0]
    else
        error "[MCP-DEBUG] Buid type is Package but, params.BUILD_TO_PACKAGE is empty, Aborting the job."
}
else if (params.BUILD_TO_PACKAGE) {
    error "[MCP-DEBUG] params.BUILD_TO_PACKAGE is ${params.BUILD_TO_PACKAGE}, But you selected job type = ${params.JOB_TYPE} Aborting the job."
}
env.DEBUGLEVEL = '4'
buildnode(params.PROJECT_NODE_LABEL) {
    /**
     * Set some enviornment variables
    */
    env.SWIDFILE                = env.WORKSPACE + "com.mcafee_apis.swidtag"
    buildRoot                   = env.WORKSPACE
    dependenciesRelativePath    = "/Dependencies"
    dependenciesFullPath        = env.WORKSPACE + dependenciesRelativePath
    mcpMainHomePath             = buildRoot + "/mcp-main"
    onPremEPOHome               = mcpMainHomePath + "/OnPremextension"
    mEpoHome                    = mcpMainHomePath + "/MVISIONePOextn"
    cEpoHome                    = mcpMainHomePath + "/extension"
    orbitBuildPullPath          = buildRoot + "/orbitBuildPullPath"
    jobOutputPath               = buildRoot + "/jobOutput"


    echo message: "MCP-DEBUG-ENV ORBIT_JOB_TYPE= ${env.ORBIT_JOB_TYPE} extensionOnly= ${extensionOnly} ORBIT_BUILD_NUMBER= ${env.ORBIT_BUILD_NUMBER} buildToPackage= ${env.buildToPackage}"
    print params
    print env

    buildOnly                   = env.ORBIT_JOB_TYPE == "Build"
    packageOnly                 = env.ORBIT_JOB_TYPE == "Package"
    isBuildAndPackage           = env.ORBIT_JOB_TYPE == "BuildAndPackage"
    extensionOnly               = env.ORBIT_JOB_TYPE == "Package" && params.extensionOnly

    TOOLSDIR                    = "E:/Tools"
    advancedInstallerPath       = "E:/tools/AdvanceInstaller/advinst_12_3_1/bin/x86/AdvancedInstaller.com"

    List versionSplit           = env.BUILDVERSION.split('\\.')
    majorVersion                = versionSplit[0]
    minorVersion                = versionSplit[1]
    patchVersion                = versionSplit[2]

    env.ENS_SDK                 = dependenciesFullPath + '/BLFramework/sdk'
    env.MA_SDK                  = dependenciesFullPath + '/MA'
    env.McTray_SDK              = dependenciesFullPath + '/McTray/sdk'
    env.SYSCORE_SDK              = dependenciesFullPath + '/SysCore/sdk'
    env.DRA_SDK                 = dependenciesFullPath + '/DRA/sdk'
    env.MCP_EXT_LIBS            = dependenciesFullPath +  "/mcp-ext/Out"

    env.JAVA_HOME               = TOOLSDIR + "/jdk1.8.0_45-X64"
    env.ANT_HOME                = TOOLSDIR + "/ANT_194"

    versionInfoToken = [
        "_V1_"          : majorVersion,
        "_V2_"          : minorVersion,
        "_V3_"          : patchVersion,
        "_BUILDNUMBER_" : env.ORBIT_BUILD_NUMBER_ONLY
    ]

    /**
     * Prebuild:
     * - For packageing , pull from artifact
     * - Add Dependencies
     *
     *
    */
    prebuild() {
        echo message: "[MCP-DEBUG] Prebuild"
        buildProfile.addGIT([
            project             : "/trellix-skyhigh/mcp-win",
            branch              : params.branchOverride,
            server              : "ssh://git@github.trellix.com",
	    instance            : "trellix",
            target              : "/mcp-main"
        ])
        if (!extensionOnly) {
            parallel(
                'Pull MCP-EXT-LIBS' : {
                    buildProfile.addGIT([
                        project         : "/trellix-skyhigh/mcp-ext",
                        branch          : params.mcpExtLibs,
                        server          : "ssh://git@github.trellix.com",
			instance        : "trellix",
                        target          : dependenciesRelativePath + "/mcp-ext"
                    ])
                    ziputils.unzipFile([
                        files: [
                            [
                                fileName : dependenciesFullPath + "/mcp-ext/Out/*.7z",
                                targetDir: dependenciesFullPath + "/mcp-ext/Out"
                            ]
                        ]
                    ])
                },
                'Pull MCP Dependencies' :
                {
					try
					{
						println "Downloading SYSCORE package from orbit"
						buildProfile.addOrbit(
						[
							buildOverride : params.SysCoreBuild,
							project : "MPT",
							component : "SYSCORE",
							componentVersion: "21.12.0",
							useExactMilestone: false,
							jobType : "Package",
							files : "SysCore*.zip",
							targetLocation : dependenciesRelativePath
						])
							def subzipfile
							dir("${dependenciesRelativePath}"){
								subzipfile = findFiles(glob: "SysCore*.zip")
								ziputils.unzipFile([
								fileName: "${subzipfile[0]}",
								targetDir: dependenciesFullPath + "/SysCore"
							])
							}
					}
					catch(error)
					{
						println "Failed to download SysCORE package from orbit"
					}

                    try
					{
						println "Downloading BLFrameworkSDK package from orbit"
                        buildProfile.addOrbit(
                        [
                            buildOverride : params.HostCommon,
                            project : "ENSW",
                            component : "HostCommon",
                            componentVersion: "10.7.0",
                            files : "BLFramework*.zip",
                            targetLocation : dependenciesRelativePath
                        ])
							def subzipfile
							dir("${dependenciesRelativePath}"){
								subzipfile = findFiles(glob: "BLFramework*.zip")
								ziputils.unzipFile([
								fileName: "${subzipfile[0]}",
								targetDir: dependenciesFullPath + "/BLFramework"
							])
							}
					}
					catch(error)
					{
						println "Failed to download BLFrameworkSDK package from orbit"
					}

                    try
					{
						println "Downloading MA package from orbit"
						buildProfile.addOrbit(
						[
                            buildOverride : params.MA_SDKOverrride,
							project : "MA",
							component : "MA",
							componentVersion: "5.7.7",
							useExactMilestone: false,
							jobType : "Package",
							files : "SDK/ma_sdk500_win.zip",
							targetLocation : dependenciesRelativePath
						])
							def subzipfile
							dir("${dependenciesRelativePath}"){
								subzipfile = findFiles(glob: "ma_sdk500_win*.zip")
								ziputils.unzipFile([
								fileName: "${subzipfile[0]}",
								targetDir: dependenciesFullPath + "/MA"
							])
							}
					}
					catch(error)
					{
						println "Failed to download MA package from orbit"
					}

                    buildProfile.addECM([
                        project         : 'McTray',
                        version         : "2.2.0",
                        buildNumber     : "615",
                        packageNumber   : "2",
                        files           : [("McTray SDK.2.2.0.zip") : dependenciesRelativePath]
                    ])

                    buildProfile.addArtifactory([
                        files           : ["Dra_Win_1.0.0.${params.DRA_BUILD_NO}.zip"],
                        root            : "DRA-local/DRA-Endpoint_1.0.0/b${params.DRA_BUILD_NO}/",
                        target          : dependenciesFullPath
                    ])

                    /**
                     * Unzip All Dependecies
                    */
                    ziputils.unzipFile([
                        files: [
                            [
                                fileName : dependenciesFullPath + "/McTray SDK.2.2.0.zip",
                                targetDir: dependenciesFullPath + "/McTray"
                            ],
                            [
                                fileName : dependenciesFullPath + "/Dra_Win*.zip",
                                targetDir: dependenciesFullPath + "/DRA"
                            ]
                        ]
                    ])
                }
            )
            /**
             * Set Version Info
            */
            utils.searchAndReplace([
                fileToReplace   : buildRoot  +  "/mcp-main/Sources/VersionInfo.h",
                input           : versionInfoToken
            ])
			  
			if(params.SupportToolReleaseBuild)
	    	{
				echo message: "[MCP-DEBUG] SupportToolReleaseBuild is true"
				
				utils.searchAndReplace([
				fileToReplace   : buildRoot  +  "/mcp-main/Sources/McpSupportTool/McpSupportTool.vcxproj",
				input           : ["SCP_RELEASE_BUILD=0" : "SCP_RELEASE_BUILD=1"]
				])				

			}
		
        }
    }

    /**
     * Skip Stage if jobType there is nothing to compile, only package, else execute.
     */
    skipStage("Compile", packageOnly == true && extensionOnly == false)
    {
        echo message: "[MCP-DEBUG] In compile stage"
        parallel(
            'Compile x64': {
                if (buildOnly || isBuildAndPackage) {
                    echo message: "[MCP-DEBUG] Compile x64"

                    visualstudio([
                        version         : params.VISUAL_STUDIO_VERSION,
                        solutionFile    : buildRoot  +  "/mcp-main/mcp-win.sln",
                        targets         : "RELEASE|x64",
                        logName         : "build64.txt",
                        enableCoverity  : params.COVERITY_BUILD,
                        enableBullseye  : params.BULLSEYE_BUILD
                    ])
                    /** Save compiled output to jobOutputPath and upload it to artifact. */
                    amd64relZip = jobOutputPath + "/amd64rel.7z"
                    dir(buildRoot + "/mcp-main/amd64rel")
                    {
                        ziputils.createZip([
                            fileName: amd64relZip,
                            input: "."
                        ])
                    }

                    opgGeneratorZip = jobOutputPath + "/OPGRelease.7z"
                    dir(buildRoot + "/mcp-main/Sources/OPG/Release")
                    {
                        ziputils.createZip([
                            fileName: opgGeneratorZip,
                            input: "."
                        ])
                    }
                    /** Generate tmf files */
                    tmfOutputPathX64 = buildRoot + "\\jobOutput\\ETW\\x64\\tmffiles"
                    bat "${mcpMainHomePath}/WppBins/bin/amd64/tracepdb.exe -f ${mcpMainHomePath}/amd64rel/Symbols/*.pdb -p ${tmfOutputPathX64}"
                }
            },

            'Compile x86': {
                if (buildOnly || isBuildAndPackage) {
                    echo message: "[MCP-DEBUG] Compile x86"
                    utils.runRobocopy([
                        src: buildRoot + "/mcp-main",
                        dest: buildRoot + "/mcp-main-x86"
                    ])
                    sleep time: 1, unit: "MINUTES"
                    visualstudio([
                        version         : params.VISUAL_STUDIO_VERSION,
                        solutionFile    : buildRoot  +  "/mcp-main-x86/mcp-win.sln",
                        targets         : "RELEASE|Win32",
                        logName         : "build32.txt",
                        enableCoverity  : params.COVERITY_BUILD,
                        enableBullseye  : params.BULLSEYE_BUILD
                    ])
                    utils.runRobocopy([
                        src: buildRoot + "/mcp-main-x86/i386rel",
                        dest: buildRoot + "/mcp-main/i386rel"
                    ])

                    /** Save compiled output to jobOutputPath and upload int to artifact. */
                    i386relZip = jobOutputPath + "/i386rel.7z"
                    dir(buildRoot + "/mcp-main/i386rel")
                    {
                        ziputils.createZip([
                            fileName: i386relZip,
                            input: "."
                        ])
                    }
                    /** Generate tmf files */
                    tmfOutputPathX32 = buildRoot + "\\jobOutput\\ETW\\x86\\tmffiles"
                    bat "${mcpMainHomePath}/WppBins/bin/x86/tracepdb.exe -f ${mcpMainHomePath}/i386rel/Symbols/*.pdb -p ${tmfOutputPathX32}"
                }
            },

            "Extension Build Steps": {
                echo message: "[MCP-DEBUG] Extension Build"
                /**
                    * Execute if this is not Build-Only job ,
                    * Here we Setup required dependecies for building extension
                    */
                if (!buildOnly) {
                    if (!extensionOnly) sleep time: 2, unit: "MINUTES"
                    extensionToBeInstalled = [
                        "AgentMgmt",
                        "CommonEvents",
                        "ComputerMgmt",
                        "DataChannel",
                        "EPOCore",
                        "epoMigration",
                        "Notifications",
                        "PolicyMgmt",
                        "RepositoryMgmt",
                        "SoftwareMgmt"
                    ]

                    /** Setup Environment for OnPrem ePO */
                    if (params.onPremEPO) {
                        onPremDependendeciesRelativePath = dependenciesRelativePath + "/onPrem"
                        onPremExtensionInstalledPath = onPremDependendeciesRelativePath + '/server/extensions/installed'
                        buildProfile.addECM([
                            project     : 'EPO',
                            version     : '5.10.0',
                            buildNumber : '3335',
                            files       : [
                                ("/Release/extensions/core/**")     : onPremDependendeciesRelativePath,
                                ("/Release/server/**")              : onPremDependendeciesRelativePath + "/server",
                                ("/Release/remote-client/**")       : onPremDependendeciesRelativePath + "/remote-client",
                                ("/Release/lib/ldap.jar")           : onPremExtensionInstalledPath + "/ldap",
                                ("/Release/lib/ldapdemo.jar")       : onPremExtensionInstalledPath + "/ldap",
                                ("/Release/lib/ldapsync.jar")       : onPremExtensionInstalledPath + "/ldap",
                                ("/Release/lib/ldap-tags.jar")      : onPremExtensionInstalledPath + "/ldap",
                                ("/Release/lib/rs.jar")             : onPremExtensionInstalledPath + "/rs",
                                ("/Release/lib/console.jar")        : onPremExtensionInstalledPath + "/console",
                                ("/Release/lib/console-tags.jar")   : onPremExtensionInstalledPath + "/console"
                            ]
                        ])
                        extensionToBeInstalled.each {
                            ziputils.unzipFile([
                                files: [
                                    [
                                        fileName    : buildRoot + onPremDependendeciesRelativePath + "/" + it + ".zip",
                                        targetDir   : buildRoot + onPremExtensionInstalledPath + "/" + it
                                    ]
                                ]
                            ])
                        }
                        /**
                        ziputils.unzipFile([
                            files: [
                                [
                                    fileName        : onPremEPOHome + "/Dependencies/catalogFramework*.zip",
                                    targetDir       : buildRoot + onPremExtensionInstalledPath + "/catalogframework"
                                ],
                                [
                                    fileName        : onPremEPOHome + "/Dependencies/coreCatalog*.zip",
                                    targetDir       : buildRoot + onPremExtensionInstalledPath + "/corecatalog"
                                ],
                            ]
                        ])
                        */
                    }

                    /** Setup Environment for Mvision ePO */
                    if (params.mvisionEPO) {
                        /** Setup Environment for Mvision ePO */
                        mEPODependendeciesRelativePath = dependenciesRelativePath + "/mEPO"
                        mEPOExtensionInstalledPath = mEPODependendeciesRelativePath + '/server/extensions/installed'
                        buildProfile.addECM([
                            project     : 'EPO',
                            version     : '5.9.0',
                            buildNumber : '1858',
                            files       : [
                                ("/Release/extensions/core/**")     : mEPODependendeciesRelativePath,
                                ("/Release/server/**")              : mEPODependendeciesRelativePath + "/server",
                                ("/Release/remote-client/**")       : mEPODependendeciesRelativePath + "/remote-client",
                                ("/Release/lib/rs.jar")             : mEPOExtensionInstalledPath + "/rs",
                                ("/Release/lib/console.jar")        : mEPOExtensionInstalledPath + "/console",
                                ("/Release/lib/console-tags.jar")   : mEPOExtensionInstalledPath + "/console",
                                ("/Release/compile/lib/epo/MvisionMigrationCloud.jar") : mEPOExtensionInstalledPath + "/MvisionMigrationCloud"
                            ]
                        ])
                        extensionToBeInstalled.each {
                            ziputils.unzipFile([
                                files: [
                                    [
                                        fileName    : buildRoot + mEPODependendeciesRelativePath + "/" + it + ".zip",
                                        targetDir   : buildRoot + mEPOExtensionInstalledPath + "/" + it
                                    ]
                                ]
                            ])
                        }
                    }
                    /** Setup Environment for CLoud ePO */
                    if (params.cldExtnEPO) {
                        cEPODependendeciesRelativePath = dependenciesRelativePath + "/cEPO"
                        cEPOExtensionInstalledPath = cEPODependendeciesRelativePath + '/server/extensions/installed'
                        buildProfile.addECM([
                            project     : 'EPO',
                            version     : '5.9.0',
                            buildNumber : '403',
                            files       : [
                                ("/Release/extensions/core/**")     : cEPODependendeciesRelativePath,
                                ("/Release/server/**")              : cEPODependendeciesRelativePath + "/server",
                                ("/Release/remote-client/**")       : cEPODependendeciesRelativePath + "/remote-client",
                                ("/Release/lib/ldap.jar")           : cEPOExtensionInstalledPath + "/ldap",
                                ("/Release/lib/ldapdemo.jar")       : cEPOExtensionInstalledPath + "/ldap",
                                ("/Release/lib/ldapsync.jar")       : cEPOExtensionInstalledPath + "/ldap",
                                ("/Release/lib/ldap-tags.jar")      : cEPOExtensionInstalledPath + "/ldap",
                                ("/Release/lib/rs.jar")             : cEPOExtensionInstalledPath + "/rs",
                                ("/Release/lib/console.jar")        : cEPOExtensionInstalledPath + "/console",
                                ("/Release/lib/console-tags.jar")   : cEPOExtensionInstalledPath + "/console"
                            ]
                        ])
                        extensionToBeInstalled.each {
                            ziputils.unzipFile([
                                files: [
                                    [
                                        fileName    : buildRoot + cEPODependendeciesRelativePath + "/" + it + ".zip",
                                        targetDir   : buildRoot + cEPOExtensionInstalledPath + "/" + it
                                    ]
                                ]
                            ])
                        }
                        /**
                        ziputils.unzipFile([
                            files: [
                                [
                                    fileName        : cEpoHome + "/Dependencies/catalogFramework*.zip",
                                    targetDir       : buildRoot + cEPOExtensionInstalledPath + "/catalogframework"
                                ],
                                [
                                    fileName        : cEpoHome + "/Dependencies/coreCatalog*.zip",
                                    targetDir       : buildRoot + cEPOExtensionInstalledPath + "/corecatalog"
                                ],
                            ]
                        ])
                        */
                    }
                    /** if it is Build And Package, wait until OPGGenterator.exe is ready */
                    if (isBuildAndPackage) {
                        waitUntil {
                            isOpgGenetorExist = fileExists(env.WORKSPACE + "/mcp-main/Sources/OPG/Release/OPGGenerator.exe")
                            if (isOpgGenetorExist) return isOpgGenetorExist
                            echo message: "[MCP-DEBUG] Checking if OPGGenerator.exe exist"
                            sleep time: 1, unit: 'MINUTES'
                            return isOpgGenetorExist
                        }
                    }
                    else {
                        /** Execute if this is not Build-And-Package (use OPGGenerator from build specified) */
                        buildProfile.addArtifactory([
                                files           : ["OPGRelease.7z"],
                                root            : "MCP-local/MCP_${params.MCP_VERSION}/b${params.BUILD_TO_PACKAGE}/", //* eg. "MCP-local/MCP_4.3.0/b95.1/"
                                target          : buildRoot + "/mcp-main/Sources/OPG/Release"
                        ])
                        ziputils.unzipFile([
                            files: [
                                [
                                    fileName : buildRoot + "/mcp-main/Sources/OPG/Release/OPGRelease.7z",
                                    targetDir: buildRoot + "/mcp-main/Sources/OPG/Release"
                                ]
                            ]
                        ])
                    }
                }

                /**
                * Execute if this is not Build-Only job
                * Here we build the extensions
                */
                if (!buildOnly) {
                    echo message: "[MCP-DEBUG] ExtensionBuild"
                    // bat '''dir Dependencies\\onPrem /s /b | findstr /i /v ".git"'''
                    // bat '''dir Dependencies\\mEPO /s /b | findstr /i /v ".git"'''
                    withEnv([
                        "PATH+JAVA_BIN=${env.JAVA_HOME}/bin",
                        "PATH+ANT_BIN=${env.ANT_HOME}/bin"]) {

                        homePath = env.WORKSPACE.replaceAll("\\\\","/")
                        echo message: "[MCP-DEBUG] " + homePath
                        /**
                        * Build OnPrem ePO Extension
                        */
                        if (params.onPremEPO) {
                            echo message: "[MCP-DEBUG] Building Onprem ePO extension"
                            onPremDependendeciesRelativePath = dependenciesRelativePath + "/onPrem"
                            utils.searchAndReplace([
                                fileToReplace   : onPremEPOHome + "/build.properties",
                                input           : versionInfoToken
                            ])

                            utils.searchAndReplace([
                                fileToReplace   : onPremEPOHome + "/build.properties",
                                input           : ["_EPO_LIB" : homePath + onPremDependendeciesRelativePath]
                            ])

                            utils.searchAndReplace([
                                fileToReplace   : onPremEPOHome + "/web/DATA/Product.cfg",
                                input           : versionInfoToken
                            ])

                            utils.searchAndReplace([
                                fileToReplace   : onPremEPOHome + "/install/extension.properties",
                                input           : [
                                    "_VERSION_" : env.BUILDVERSION,
                                    "_BUILD_"   : env.ORBIT_BUILD_NUMBER_ONLY
                                ]
                            ])
                            // bat script: "type ${env.WORKSPACE}\\mcp-main\\OnPremextension\\build.properties"
                            dir(onPremEPOHome)
                            {
                                bat script: "ant >> onPremEPOBuild.txt"
                                utils.runRobocopy([
                                    src: onPremEPOHome,
                                    dest: jobOutputPath,
                                    file: "onPremEPOBuild.txt",
                                    flags: "",
                                    replaceFlags : true
                                ])
                                tempZipPath = onPremEPOHome + "/temp-zip"
                                utils.runRobocopy([
                                    src: onPremEPOHome + '/build',
                                    dest: tempZipPath,
                                    file: "MCPSRVER1000_" + env.BUILDVERSION + ".zip",
                                    flags: " ",
                                    replaceFlags : true
                                ])
                                utils.runRobocopy([
                                    src: onPremEPOHome + '/Dependencies',
                                    dest: tempZipPath,
                                    file: "*.zip",
                                    flags: " ",
                                    replaceFlags : true
                                ])
                                /** Bundle coreCatalog, commonCatalog & MCP Help zip together */
                                dir(tempZipPath) {
                                    // bat '''dir /s /b | findstr /i /v ".git"'''
                                    /**
                                    signing.ePoExtSignAFile([
                                        files: [
                                            "MCPSRVER1000_" + env.BUILDVERSION + ".zip",
                                            "coreCatalog_2.0.6.33.zip",
                                            "help_mcc_200.zip",
                                            "help_mcp_232.zip"
                                        ]
                                    ])
                                    */
                                    signing.ePoExtSignAFile([
                                        files: [
                                            "MCPSRVER1000_" + env.BUILDVERSION + ".zip",
                                            "help_mcp_232.zip"
                                        ]
                                    ])
                                    ziputils.createZip([
                                        fileName: "MCPSRVER1000_" + env.BUILDVERSION + "." + env.ORBIT_BUILD_NUMBER_ONLY + ".zip",
                                        input: "*.zip"
                                    ])
                                    signing.ePoExtSignAFile([
                                        files: [
                                            "MCPSRVER1000_" + env.BUILDVERSION + "." + env.ORBIT_BUILD_NUMBER_ONLY + ".zip"
                                        ]
                                    ])
                                    utils.runRobocopy([
                                        src: tempZipPath,
                                        dest: jobOutputPath,
                                        file: "MCPSRVER1000_*.zip",
                                        flags: "",
                                        replaceFlags : true
                                    ])
                                }
                            }
                        }
                        /**
                        * Build Mvision ePO Extension
                        */
                        if (params.mvisionEPO) {
                            echo message: "[MCP-DEBUG] Building Mvision ePO extension"
                            mEPODependendeciesRelativePath = dependenciesRelativePath + "/mEPO"
                            utils.searchAndReplace([
                                fileToReplace   : mEpoHome + "/build.properties",
                                input           : versionInfoToken
                            ])

                            utils.searchAndReplace([
                                fileToReplace   : mEpoHome + "/build.properties",
                                input           : ["_EPO_LIB" : homePath + mEPODependendeciesRelativePath]
                            ])

                            utils.searchAndReplace([
                                fileToReplace   : mEpoHome + "/install/extension.properties",
                                input           : [
                                    "_VERSION_" : env.BUILDVERSION,
                                    "_BUILD_"   : env.ORBIT_BUILD_NUMBER_ONLY
                                ]
                            ])
                            // bat script: "type ${env.WORKSPACE}\\mcp-main\\MVISIONePOextn\\build.properties"
                            dir(mEpoHome)
                            {
                                bat script: "ant >> mvisionEPOExtensionBuild.txt"
                                utils.runRobocopy([
                                    src: mEpoHome,
                                    dest: jobOutputPath,
                                    file: "mvisionEPOExtensionBuild.txt",
                                    flags: "",
                                    replaceFlags : true
                                ])
                                outputZipPath = mEpoHome + "/build/MCPMV_" + env.BUILDVERSION + ".zip"
                                signing.ePoExtSignAFile([
                                    files: [outputZipPath]
                                ])
                                utils.runRobocopy([
                                    src: mEpoHome + '/build',
                                    dest: jobOutputPath,
                                    file: "*.zip",
                                    flags: "",
                                    replaceFlags : true
                                ])
                            }
                        }
                        /**
                        * Build CLoud ePO Extension
                        */
                        if (params.cldExtnEPO) {
                            echo message: "[MCP-DEBUG] Building Cloud ePO extension"
                            cEPODependendeciesRelativePath = dependenciesRelativePath + "/cEPO"
                            utils.searchAndReplace([
                                fileToReplace   : cEpoHome + "/build.properties",
                                input           : versionInfoToken
                            ])

                            utils.searchAndReplace([
                                fileToReplace   : cEpoHome + "/build.properties",
                                input           : ["_EPO_LIB" : homePath + cEPODependendeciesRelativePath]
                            ])

                            utils.searchAndReplace([
                                fileToReplace   : cEpoHome + "/install/extension.properties",
                                input           : [
                                    "_VERSION_" : env.BUILDVERSION,
                                    "_BUILD_"   : env.ORBIT_BUILD_NUMBER_ONLY
                                ]
                            ])
                            bat script: "type ${env.WORKSPACE}\\mcp-main\\MVISIONePOextn\\build.properties"
                            dir(cEpoHome)
                            {
                                bat script: "ant >> cloudEPOExtensionBuild.txt"
                                utils.runRobocopy([
                                    src: cEpoHome,
                                    dest: jobOutputPath,
                                    file: "cloudEPOExtensionBuild.txt",
                                    flags: "",
                                    replaceFlags : true
                                ])

                                outputZipPath = cEpoHome + "/build/MCPAGENTMETA_" + env.BUILDVERSION + ".zip"
                                signing.ePoExtSignAFile([
                                    files: [outputZipPath]
                                ])
                                utils.runRobocopy([
                                    src: cEpoHome + '/build',
                                    dest: jobOutputPath,
                                    file: "*.zip",
                                    flags: "",
                                    replaceFlags : true
                                ])
                            }
                        }
                        }
                }
            }
        )
    }
    /**
     * Stage = Prepackage
     * Skip Stage if jobType is BuildOnly or only extension require, else execute.
     */
    skipStage('Prepackage', buildOnly == true || extensionOnly == true)
    {
        echo message: "[MCP-DEBUG] Prepackage"
        if (isBuildAndPackage == false) {
            buildProfile.addArtifactory([
                files           : ["*.7z"],
                root            : "MCP-local/MCP_${params.MCP_VERSION}/b${params.BUILD_TO_PACKAGE}/", //* eg. "MCP-local/MCP_4.3.0/b95.1"
                target          : orbitBuildPullPath
            ])
            // bat '''dir orbitBuildPullPath /s /b | findstr /i /v ".git"'''
            ziputils.unzipFile([
                files: [
                    [
                        fileName : orbitBuildPullPath + "/amd64rel.7z",
                        targetDir: mcpMainHomePath + "/amd64rel"
                    ],
                    [
                        fileName : orbitBuildPullPath + "/i386rel.7z",
                        targetDir: mcpMainHomePath + "/i386rel"
                    ],
                ]
            ])
        }
    }

    /**
     * Stage = signing
     * Skip Stage if jobType is BuildOnly or only extension require, else execute.
     */
    stage("Signing")
    {
        if (!(buildOnly == true || extensionOnly == true)) {
            echo message: "[MCP-DEBUG] Signing"
            /**
             * Signing executable and dll's before packaging in parallel
             */
            exeAndDllForSigning = [
                "ScpService.exe",
                "SCPAbout.exe",
                "SCPUninstallProtection.exe",
                "InstallHelper.dll",
                "SCPBypass.exe",
                "ma_app.dll",
                "scp.resources.dll",
                "McpBOVerifier.exe",
                "MCPUnitTest.exe",
                "SCPSupportTool.exe",
                "Firewall.dll",
                "Wireguard.dll",
                "Socks.dll"
            ]
            ['mcp-main/amd64rel', 'mcp-main/i386rel'].each {
                dir(it)
                {
			if(params.FutureCertificate)
			{
				echo message: "[MCP-DEBUG] Signing with FutureCertificate"
				signing.digitalSignSHA256([files: exeAndDllForSigning, certConfig: "CURRENT"])
			}
			else
			{
				echo message: "[MCP-DEBUG] Signing with Skyhigh Certificate"
				signing.digitalSignSHA256([files: exeAndDllForSigning, customCert:"skyhigh"])
			}
			echo message: "[MCP-DEBUG] Signed exeAndDllForSigning"
			
			if(params.FutureCertificate)
			{
				signing.digitalSignSHA256([files: ["scp.cat"], certConfig: "CURRENT"])
			}
			else
			{
				signing.digitalSignSHA256([files: ["scp.cat"], customCert:"skyhigh"])
			}
			echo message: "[MCP-DEBUG] Signed mcp.cat"

                    signing.MASigning([
                        files: ["ma_msgbus_auth.xml"]
                    ])
                }
            }

            dir(env.MCP_EXT_LIBS)
            {
		    	if(params.FutureCertificate)
			{
				signing.digitalSignSHA256([files: [
                        "openssl\\lib\\Release\\x64\\libssl-3-x64.dll",
                        "openssl\\lib\\Release\\x64\\libcrypto-3-x64.dll",
                        "openssl\\lib\\Release\\x32\\libssl-3.dll",
                        "openssl\\lib\\Release\\x32\\libcrypto-3.dll",
                        "curl\\lib\\Release\\x32\\libcurl.dll",
                        "curl\\lib\\Release\\x64\\libcurl.dll",
                        "ntk\\lib\\x64\\ndisapi.dll",
                        "ntk\\lib\\x32\\ndisapi.dll",
                        "ntk\\driver\\x64\\mcafee_vnsp_probe.sys",
                        "ntk\\driver\\x86\\mcafee_vnsp_probe.sys",
                        "ntk\\driver\\x64\\snetcfg.exe",
						"mowgliSDK-x64-Release\\Modules\\Binary.dll",
						"mowgliSDK-x64-Release\\Modules\\DateTime.dll",
						"mowgliSDK-x64-Release\\Modules\\Encoding.dll",
						"mowgliSDK-x64-Release\\Modules\\JSON.dll",
						"mowgliSDK-x64-Release\\Modules\\Strings.dll",
						"mowgliSDK-x64-Release\\Modules\\URI.dll",
						"mowgliSDK-x64-Release\\Modules\\Utils.dll",
						"mowgliSDK-x64-Release\\Modules\\Variant.dll",
                        "mowgliSDK-x64-Release\\Modules\\Regex.dll",
						"mowgliSDK-x86-Release\\Modules\\Binary.dll",
						"mowgliSDK-x86-Release\\Modules\\DateTime.dll",
						"mowgliSDK-x86-Release\\Modules\\Encoding.dll",
						"mowgliSDK-x86-Release\\Modules\\JSON.dll",
						"mowgliSDK-x86-Release\\Modules\\Strings.dll",
						"mowgliSDK-x86-Release\\Modules\\URI.dll",
						"mowgliSDK-x86-Release\\Modules\\Utils.dll",
						"mowgliSDK-x86-Release\\Modules\\Variant.dll",
						"mowgli-modules\\x64-Release\\Cache.dll",
						"mowgli-modules\\x64-Release\\Cryptography.dll",
						"mowgli-modules\\x64-Release\\DNS.dll",
						"mowgli-modules\\x64-Release\\HTTPClient.dll",
						"mowgli-modules\\x64-Release\\HTTPHeader.dll",
						"mowgli-modules\\x64-Release\\Locks.dll",
						"mowgli-modules\\x64-Release\\MCPAuth.dll",
						"mowgli-modules\\x64-Release\\Network.dll",
						"mowgli-modules\\x64-Release\\SQLite.dll",
						"mowgli-modules\\x64-Release\\TCP.dll",
						"mowgli-modules\\x86-Release\\Cache.dll",
						"mowgli-modules\\x86-Release\\Cryptography.dll",
						"mowgli-modules\\x86-Release\\DNS.dll",
						"mowgli-modules\\x86-Release\\HTTPClient.dll",
						"mowgli-modules\\x86-Release\\HTTPHeader.dll",
						"mowgli-modules\\x86-Release\\Locks.dll",
						"mowgli-modules\\x86-Release\\MCPAuth.dll",
						"mowgli-modules\\x86-Release\\Network.dll",
						"mowgli-modules\\x86-Release\\SQLite.dll",
						"mowgli-modules\\x86-Release\\TCP.dll"
                   		 ], certConfig: "CURRENT"])
			}
			else
			{
   			signing.digitalSignSHA256([files: [
                        "openssl\\lib\\Release\\x64\\libssl-3-x64.dll",
                        "openssl\\lib\\Release\\x64\\libcrypto-3-x64.dll",
						"openssl\\lib\\Release\\x64\\fips.dll",
                        "openssl\\lib\\Release\\x32\\libcrypto-3.dll",
                        "curl\\lib\\Release\\x32\\libcurl.dll",
                        "curl\\lib\\Release\\x64\\libcurl.dll",
                        "ntk\\lib\\x64\\ndisapi.dll",
                        "ntk\\lib\\x32\\ndisapi.dll",
                        "ntk\\driver\\x64\\mcafee_vnsp_probe.sys",
                        "ntk\\driver\\x86\\mcafee_vnsp_probe.sys",
                        "ntk\\driver\\x64\\snetcfg.exe",
                        "mowgliSDK-x64-Release\\Modules\\Binary.dll",
                        "mowgliSDK-x64-Release\\Modules\\DateTime.dll",
                        "mowgliSDK-x64-Release\\Modules\\Encoding.dll",
                        "mowgliSDK-x64-Release\\Modules\\JSON.dll",
                        "mowgliSDK-x64-Release\\Modules\\Strings.dll",
                        "mowgliSDK-x64-Release\\Modules\\URI.dll",
                        "mowgliSDK-x64-Release\\Modules\\Utils.dll",
                        "mowgliSDK-x64-Release\\Modules\\Variant.dll",
                        "mowgliSDK-x64-Release\\Modules\\Regex.dll",
                        "mowgliSDK-x86-Release\\Modules\\Binary.dll",
                        "mowgliSDK-x86-Release\\Modules\\DateTime.dll",
                        "mowgliSDK-x86-Release\\Modules\\Encoding.dll",
                        "mowgliSDK-x86-Release\\Modules\\JSON.dll",
                        "mowgliSDK-x86-Release\\Modules\\Strings.dll",
                        "mowgliSDK-x86-Release\\Modules\\URI.dll",
                        "mowgliSDK-x86-Release\\Modules\\Utils.dll",
                        "mowgliSDK-x86-Release\\Modules\\Variant.dll",
                        "mowgli-modules\\x64-Release\\Cache.dll",
                        "mowgli-modules\\x64-Release\\Cryptography.dll",
                        "mowgli-modules\\x64-Release\\DNS.dll",
                        "mowgli-modules\\x64-Release\\HTTPClient.dll",
                        "mowgli-modules\\x64-Release\\HTTPHeader.dll",
                        "mowgli-modules\\x64-Release\\Locks.dll",
                        "mowgli-modules\\x64-Release\\MCPAuth.dll",
                        "mowgli-modules\\x64-Release\\Network.dll",
                        "mowgli-modules\\x64-Release\\SQLite.dll",
                        "mowgli-modules\\x64-Release\\TCP.dll",
                        "mowgli-modules\\x86-Release\\Cache.dll",
                        "mowgli-modules\\x86-Release\\Cryptography.dll",
                        "mowgli-modules\\x86-Release\\DNS.dll",
                        "mowgli-modules\\x86-Release\\HTTPClient.dll",
                        "mowgli-modules\\x86-Release\\HTTPHeader.dll",
                        "mowgli-modules\\x86-Release\\Locks.dll",
                        "mowgli-modules\\x86-Release\\MCPAuth.dll",
                        "mowgli-modules\\x86-Release\\Network.dll",
                        "mowgli-modules\\x86-Release\\SQLite.dll",
                        "mowgli-modules\\x86-Release\\TCP.dll"                        
                   		 ],customCert:"skyhigh"])
			          }
            }
        }
    }
    /**
     * Stage =  Package
     * Skip Stage if jobType is BuildOnly or only extension require, else execute.
     */
    skipStage('Package', buildOnly == true || extensionOnly == true)
    {
        echo message: "[MCP-DEBUG] Packaging"
        /** Build MCP Installer */
        dir("mcp-main/standalonePackage") {
            tokenReplacement = [
                "_V1_"          : majorVersion,
                "_V2_"          : minorVersion,
                "_V3_"          : patchVersion,
                "_VERSION_"     : env.BUILDVERSION,
                "_BUILD_"       : env.ORBIT_BUILD_NUMBER_ONLY,
                "_BUILDNUMBER_" : env.ORBIT_BUILD_NUMBER_ONLY,
                "_BUILDNUM_"    : env.ORBIT_BUILD_NUMBER_ONLY,
                "_PACKAGENUM_"  : env.ORBIT_PACKAGE_NUMBER,
            ]

            utils.searchAndReplace([
                fileToReplace   : "BuildMSI.bat",
                input           : ["D:\\\\Tools\\\\Advanced Installer 7.1.3\\\\AdvancedInstaller.com" : advancedInstallerPath]
            ])
            utils.searchAndReplace([
                fileToReplace   : "EnvarMcpVersion.bat",
                input           : tokenReplacement
            ])
            utils.searchAndReplace([
                fileToReplace   : "PkgCatalog.xml",
                input           : tokenReplacement
            ])
            utils.searchAndReplace([
                fileToReplace   : "SCPDet.McS",
                input           : tokenReplacement
            ])
            utils.searchAndReplace([
                fileToReplace   : "license.txt",
                input           : tokenReplacement
            ])


            bat script: "BuildMSI.bat"
		if(params.FutureCertificate)
		{
			signing.digitalSignSHA256([files: ["ScpInstaller.x64.msi"], certConfig: "CURRENT"])
		}
		else
		{
           		signing.digitalSignSHA256([files: ["ScpInstaller.x64.msi"], customCert:"skyhigh"])
		}
		echo message: "[MCP-DEBUG] Signed mcp installer"
            signing.epoSign46 ([
                signType        : "package",
                fileToEncrypt   : ".mcs",
                xmlName         : "PkgCatalog.xml"
            ])

            scpPackageZip = jobOutputPath + "/scp-win ${env.BUILDVERSION} Build ${env.ORBIT_BUILD_NUMBER_ONLY} Package #${env.ORBIT_PACKAGE_NUMBER} (ENU-LICENSED-RELEASE-MAIN).zip"
            // bat '''dir /s /b | findstr /i /v ".git"'''
            filesToInclude = [
                "SCPDet.McS",
                "SCPIns.McS",
                "ScpInstaller.x64.msi",
                "PkgCatalog.z"
            ]
            ziputils.createZip([
                fileName: scpPackageZip,
                input: filesToInclude.join(',')
            ])
            signing.ePoExtSignAFile([
                files: [scpPackageZip],
            ])
        // bat '''dir /s /b | findstr /i /v ".git"'''

        }
    }
    delivery() {
        echo message: "[MCP-DEBUG] delivery"
        dir(jobOutputPath)
        {
            echo message: "[MCP-DEBUG] Files to be saved in Artifact"
            bat '''dir /s /b | findstr /i /v ".git"'''
            buildProfile.stashForUpload([includes: "**"])
        }
        //bat '''dir /s /b | findstr /i /v ".git"'''
        buildProfile.uploadToArtifactory()
        buildProfile.publishBuildInfo()
    }
}