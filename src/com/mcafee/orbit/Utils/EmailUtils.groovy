package com.mcafee.orbit.Utils
import com.cloudbees.groovy.cps.NonCPS

/**
 * Utilities for manipulating emails
 */
class EmailUtils {
    /**
     * Reformat McAfee emails to Trellix (Employee & DLs) to prevent email bounces
     * Calls reformatSingleEmail function for each email that is concatenated with a comma
     * 
     * @param emails The emails you need to reformat
     */
    @NonCPS
    static def reformatEmails(def emails) {
        if (emails != null) {
          return emails.split(/[,;\s]+/).collect { reformatSingleEmail(it.trim()) }.findAll { it }.join(', ')
        }
    }

    /**
     * Perform the reformat from McAfee email to Trellix email
     * Replaces the trailing domain in all McAfee instances
     * Replaces the underscore if employee/personal email
     *
     * @param email The single email you need to reformat
     */
    @NonCPS
    static def reformatSingleEmail(def email) {
        email = email.toLowerCase()
        if (email.endsWith("@mcafee.com")) {
            // DLs start with an underscore for both Trellix & McAfee
            if (email.contains("_") && !email.startsWith("_")) {
                email = email.replace("_", ".")
            }
            email = email.replace("@mcafee.com", "@trellix.com")
        }
        return email
    }
}
