@Library("tanzeel-shared-library") _

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
            string(defaultValue: "windows", description: "Project Build Node Label", name: "PROJECT_NODE_LABEL"),
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
echo message: params.PROJECT_NODE_LABEL
buildnode(params.PROJECT_NODE_LABEL) {
}
