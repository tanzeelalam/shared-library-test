package org.mcafee.orbit.CauseChain

import com.cloudbees.groovy.cps.NonCPS

/**
 * Describes a commit related to a build
 */
class Commit {
    /**
     * 'git'
     */
    String type
    /**
     * The name of the commit author
     */
    String author
    /**
     * The revision
     */
    String revision
    /**
     * Format commit as HTML
     *
     * @return Commit as HTML
     */
    @NonCPS
    String format() {
        "<li>Commit ${revision} by ${author} to ${type}</li>\n"
    }
}
