package org.mcafee.orbit.CauseChain

import com.cloudbees.groovy.cps.NonCPS

/**
 * Describes a build in the cause chain
 */
class ChainLink {
    /**
     * Build name
     */
    String name
    /**
     * Build url
     */
    String url
    /**
     * Build result
     */
    String result
    /**
     * A list of causes associated with this build
     */
    List<Cause> causes
    /**
     * A list of commits associated with this build or null
     */
    List<Commit> commits
    /**
     * Format build as HTML
     *
     * @return ChainLink as HTML
     */
    @NonCPS
    String format() {
        String output = "<ul>\n"
        output += "<li>Build name: $name</li>\n"
        output += "<li>Build url: <a href=\"$url\">$url</a></li>\n"
        output += "<li>Build result: $result\n"
        if (commits) {
            for (int i=0; i<commits.size(); i++) {
                output += commits[i].format()
            }
        }
        if (causes) {
            for (int i=0; i<causes.size(); i++) {
                output += causes[i].format()
            }
        }
        output += "</ul>"
        output
    }
}