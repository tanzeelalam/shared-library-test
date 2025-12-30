package org.mcafee.orbit.CauseChain

import com.cloudbees.groovy.cps.NonCPS

/**
 * Loads and formats a cause chain for a build
 */
class CauseChain {
    /**
     * A reference to the pipeline
     */
    private def script
    /**
     * The root build in the chain
     */
    private ChainLink root
    /**
     * Default constructor
     *
     * @param script A reference to the pipeline
     */
    CauseChain(def script) {
        this.script = script
    }
    /**
     * Loads the cause chain.
     *
     * Would have been nice to have this in the constructor,
     * but Jenkins is having none of that.
     */
    @NonCPS
    void init() {
        root = getBuildInfo(script.currentBuild.rawBuild)
    }
    /**
     * Loads the specified build information object
     *
     * @param build The build object
     * @return A new ChainLink
     */
    @NonCPS
    private ChainLink getBuildInfo(def build) {
        return new ChainLink(
            name: build.fullDisplayName,
            url: build.absoluteUrl,
            result: build.result ?: 'NOT SET',
            commits: getCommits(build.changeSets),
            causes:  getCauses(build.causes)
        )
    }
    /**
     * Parses the list of causes from the build information
     *
     * @param buildCauses A list of the build causes for the current build
     * @return A list of causes related to the build information object including parent causes
     */
    @NonCPS
    private List<Cause> getCauses(def buildCauses) {
        List<Cause> localCauses = new ArrayList<Cause>()
        for(def cause in buildCauses) {
            ChainLink parent = null
            if(cause.hasProperty("upstreamUrl")) {
                def upstreamJob = Jenkins.getInstance().getItemByFullName(cause.upstreamProject, hudson.model.Job.class)
                if(upstreamJob) {
                    def upstreamBuild =  upstreamJob.getBuildByNumber(cause.upstreamBuild)
                    if(upstreamBuild) {
                        parent = getBuildInfo(upstreamBuild)
                    }
                }
            }
            localCauses.add(new Cause(
                    description: cause.shortDescription,
                    parent: parent
            ))
        }
        return localCauses
    }
    /**
     * Parses the list of commits
     *
     * @param changeSets The build changeSets
     * @return The list of commits related to the build information object
     */
    @NonCPS
    private List<Commit> getCommits(def changeSets) {
        for (def changeSet in changeSets) {
            if (changeSet.kind == 'git') {
                return changeSet.items.collect {
                    new Commit(
                        type: 'git',
                        revision: it.commitId,
                        author: it.author.fullName
                    )
                }
            }
        }
        return null
    }
    /**
     * Format cause chain as HTML
     *
     * @return Cause chain as HTML
     */
    @NonCPS
    String format() {
        if (root != null) {
            root.format()
        } else {
            'Cause chain not available'
        }
    }
}