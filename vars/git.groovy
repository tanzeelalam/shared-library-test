import com.mcafee.orbit.Credentials.CredentialStore
import com.mcafee.orbit.Utils.StringUtils
/**
 * Checks out a repository from GitHub
 * <h4>Sample usage:</h4>
 * <pre>
 * git.gitMain(
 *   branch: "jira/BAAS-480",
 *   project: "trellix-utilities/pipeline-common",
 *   instance: "trellix",
 *   tag: "MFS-Supersonic.AAA.1.0.0.135",
 *   target: "workspaceSub",
 *   lfsRepo: "reponame",
 * )
 * </pre>
 * @param project [T:String] Name of the project. The specified project name should be in the standard GitHub hierarchy, it should be preceded by organization name.
 * @param instance [T:String][OPTIONAL] Override GitHub instance: <code>"trellix"</code>, <code>"mcafee"</code> or <code>"shared"</code>. [DEFAULT:"trellix"]
 * @param branch [T:String][OPTIONAL] Name of the branch , eg: "master" or "branches/release_1.0". [DEFAULT:"master"]
 * @param tag [T:String][OPTIONAL] The tag to checkout. Over-rides the branch parameter. [DEFAULT:null]
 * @param target [T:String][OPTIONAL] Target directory for checking out the files. [DEFAULT:pwd()]
 * @param lfsRepo [T:String][OPTIONAL] Artifactory Git LFS repo name. [DEFAULT:null]
 * @param returnBom [T:boolean][OPTIONAL] Returns the entire BOM instead of just the commit SHA. [DEFAULT:false]
 * @param extensions [T:List][OPTIONAL] Over-ride for GIT extension configuration, as used in <code>checkout()</code>. Defaults to <code>null</code>. See "extensions" in "GitSCM" section at https://www.jenkins.io/doc/pipeline/steps/workflow-scm-step/
 * @return [T:String|Map] The commit sha or the SCM BOM if <code>returnBom</code> is <code>true</code>.
 */
def gitMain(Map arg) {
    global()
    env.current_task = 'git'
    //Reporting and Validation
    log.debug("GIT:gitMain: Starting...")

    assert !StringUtils.isNullOrEmpty(arg.project) : "Unspecified required field: arg.project"
    assert global.GITHUB_INSTANCES.containsKey(arg.instance) : "Not a valid Github instance - ${arg.instance}"

    // normalise project name
    arg.project = arg.project.replaceAll(/^\//, '').replaceAll(/\.git$/, '')

    def credentialsId = global.GITHUB_INSTANCES[arg.instance].SshCredentialId
    def cloneUrl = global.GITHUB_INSTANCES[arg.instance].CloneUrl
                        .replace("{0}",global.GITHUB_INSTANCES[arg.instance].Domain)
                        .replace("{1}/{2}", arg.project)
    def server = 'ssh://git@' + global.GITHUB_INSTANCES[arg.instance].Domain
    
    /**The slash direction of the target path is formatted for the node operating system*/
    arg.target = utils.formatPathForOS(arg.target)
    if (isUnix()) {
        log.debug("Linux detected\nGIT:target is now " + arg.target)
    } else {
        log.debug("Windows detected\nGIT:target is now " + arg.target)
    }
    
    if (StringUtils.isNullOrEmpty(arg.lfsRepo)) {
        log.debug("lfsRepo not specified. Checkout with no Git LFS repo")
    }
    try{  
        log.debug("Starting Git Clone process")
        String commitSha = this.gitClone([
            branch: arg.branch,
            credentialsId: credentialsId,
            extensions: arg.extensions,
            cloneUrl: cloneUrl,
            server: server,
            target: arg.target,
            tag: arg.tag,
            lfsRepo: arg.lfsRepo
        ])
        def bomResponse
        //Report the checkout to BOM
        if (env.JOB != "ADMIN/Protex/Protex_Scan") {
            bomResponse = bom.scmBOM([
                bomType: "SCM",
                scmType: "GIT",
                scmCredential: credentialsId,
                scmVersion: getGitVersion(),
                scmCommit: commitSha,
                scmBranch: arg.branch,
                lfsRepository: arg.lfsRepo,
                scmUrl: cloneUrl
            ])
        }
        log.debug("GIT:gitMain: Completed")
        if (arg.returnBom && env.JOB != "ADMIN/Protex/Protex_Scan") {
            return bomResponse
        }
        return commitSha
    } catch (Throwable error) {
        log.error('Failed to pull from git')
        rethrow(error)
    }
}

/**
 * Clones the specified Branch or Tag to a local directory
 * <h4>Sample usage:</h4>
 * <pre>
 * git.gitClone([
 *   project: "organization/test_project",
 *   branch: "master",
 *   credentialsId: "ghe_bserver_ssh",
 *   cloneUrl: "ssh://git@github.trellix.com:trellix-utilities/pipeline-common.git/"
 *   server: "ssh://git@github.trellix.com/",
 *   target: "targerdir",
 *   tag: "ORBIT_TEST_15,
 *   lfsRepo: "reponame"
 * ])
 * </pre>
 * @param cloneUrl The github url to clone
 * @param credentialsId CredentialsID defined in Jenkins Credential store (default: ghe_bserver_ssh)
 * @param tag Creates a clone from the Specified tag instead of a branch
 * @param target The target directory to clone the repository to (default: pwd())
 * @param lfsRepo The name of the LFS repo in artifactory (defaults to none, proceeds with regular checkout)
 * @param extensions (optional) - Over-ride for GIT extension configuration, as used in checkout(). Defaults to none. See "extensions" in "GitSCM" section at https://www.jenkins.io/doc/pipeline/steps/workflow-scm-step/
 * @return The commit sha
 */
private String gitClone(Map arg) {
    log.debug(arg)
    def scmVars
    String commitSha
    //Passed vars are project, branch, credentialsId, cloneUrl, server, target, tag
    String branchVar = arg.branch
    if (!StringUtils.isNullOrEmpty(arg.tag)) {
        branchVar = "refs/tags/" + arg.tag
    }

    log.debug("Starting pipeline checkout with branchVar: " + branchVar)
    //Perform the clone using pipeline checkout. NOTE: git version will likely differ between node and Jenkins.
    try {
        dir(arg.target) {
            utils.setupGitLfs(arg.lfsRepo)
            int lfsTimeout = env.ARTIFACTORY_TIMEOUT ? env.ARTIFACTORY_TIMEOUT.toInteger(): 45 
            int gitCloneTimeout = env.GIT_CLONE_TIMEOUT ? env.GIT_CLONE_TIMEOUT.toInteger(): 30

            timeout(lfsTimeout+gitCloneTimeout) {
                retry(2) {
                    scmVars = checkout(
                        poll: false,
                        scm: [
                            $class: 'GitSCM',
                            branches: [[name: branchVar]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: arg.extensions ?: [
                                [$class: 'CloneOption', timeout: gitCloneTimeout],
                                [$class: 'CheckoutOption', timeout: lfsTimeout],
                                [$class: 'GitLFSPull'],
                                [
                                    $class: 'SubmoduleOption',
                                    disableSubmodules: false,
                                    parentCredentials: true,
                                    recursiveSubmodules: true,
                                    trackingSubmodules: false
                                ]
                            ],
                            submoduleCfg: [],
                            userRemoteConfigs: [[
                                credentialsId: arg.credentialsId,
                                url: arg.cloneUrl
                            ]]
                        ]
                    )
                    commitSha = scmVars.GIT_COMMIT
                    if(commitSha == null) {
                        log.debug("scmVars.GIT_COMMIT was null. Running a git command to get the sha.")
                        def cmd = "git rev-parse HEAD"
                        if(isUnix()) {
                            commitSha = sh(returnStdout: true, script: '#!/bin/sh -e\n' + cmd).trim()
                        }
                        else {
                            def stdout = bat(returnStdout:true , script: cmd).trim()
                            commitSha = stdout.readLines().drop(1).join(" ")
                        }
                        
                    }
                }
            }
        }
        return commitSha
    } catch (Throwable e) {
        radar.addBuildLog(
            "Error",
            "Git_checkout",
            env.current_stage,
            arg.server,
            e.message,
        )
        rethrow(e, 'Failed to clone GIT repo', true)
    }
}

/**
 * Gets the commit message associated with a given sha and repository
 * <h4>Sample usage:</h4>
 * <pre>
 * git.getCommitMessage(
 *    sha: "organization/test_project",
 *    target: "path/to/cloned/repo",
 * )
 * </pre>
 * @param sha [T:String] The sha of the commit to get the message for
 * @param target [T:String] The locally cloned repository
 */
String getCommitMessage(Map arg) {
    try{
        dir(arg.target){
            if (isUnix()) {
                return sh(script: 'git log --format=%B -n 1 ' + arg.sha, returnStdout: true)
            } else {
                return bat(script: '@echo off && git log --format=%%B -n 1 ' + arg.sha, returnStdout: true)
            }
        }
    }
    catch (Throwable e) {
        log.debug("Failed to retrieve git commit message  :" + e)
        return ""
    }
}

/**
 * Gets the git version
 * @return The version of git
 */
private String getGitVersion() {
    String gitVersion
    try {
        retry(5) {
            if (isUnix()) {
                gitVersion = sh(returnStdout: true, script: 'git --version').trim()
            } else {
                gitVersion = bat(returnStdout: true, script: '@git --version').trim()
            }
            gitVersion = gitVersion.toString().split("version")[1]
        }
    }
    catch (Throwable e) {
        rethrow(e, 'Unexpected response from git', true)
    }
    return gitVersion
}
