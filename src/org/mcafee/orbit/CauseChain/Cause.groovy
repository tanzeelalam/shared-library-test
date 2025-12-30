package org.mcafee.orbit.CauseChain

import com.cloudbees.groovy.cps.NonCPS

/**
 * Describes the cause of a build
 */
class Cause {
    /**
     * The description of the cause
     */
    String description
    /**
     * The next item in the cause chain, i.e.: the upstream build
     */
    ChainLink parent
    /**
     * Format cause as HTML
     *
     * @return Cause as HTML
     */
    @NonCPS
    String format() {
        String output = "<li>\n"
        output += "Cause: $description\n"
        if (parent != null) {
            output += parent.format()
        }
        output += "</li>\n"
        output
    }
}