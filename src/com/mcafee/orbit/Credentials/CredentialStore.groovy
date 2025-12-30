package com.mcafee.orbit.Credentials

import com.cloudbees.groovy.cps.NonCPS
import java.lang.reflect.Modifier

/**
 * List of credential names that are available in Jenkins.
 */
final class CredentialStore {
    /**
     * Stores the credentials for mapping buildmaster drives using generic service account
     */
    static final String CREDENTIAL_FOR_BUILDMASTER_DRIVE_MAPPING = 'bmpm_cred'
    /**
     * Stores the credentials for mapping packagemaster drives using generic service account
     */
    static final String CREDENTIAL_FOR_PACKAGEMASTER_DRIVE_MAPPING = 'bmpm_cred'
    /**
     * Stores Intel Artifactory user account
     */
    static final String CREDENTIAL_FOR_INTEL_ARTIFACTORY_AUTHENTICATION ='art_intel_cred'
    /**
     * Stores Trellix Artifactory user account
     */
    static final String CREDENTIAL_FOR_ARTIFACTORY_AUTHENTICATION = 'artifactory_trellix_cred'
    /**
     * Stores the credentials for ecm buildTracker API query
     */
    static final String CREDENTIAL_FOR_ECM_API_AUTHENTICATION = 'ecm_local_cred'
    /**
     * Orbit user
     */
    static final String CREDENTIAL_FOR_RADAR_TOKEN_AUTHENTICATION = 'orbit_radar_cred'
    /**
     * Holds the key for artifactory access
     */
    static final String ARTIFACTORY_API_TOKEN = 'artifactory_api_token'
    /**
     * Stores CDA user account
     */
    static final String CREDENTIAL_FOR_CDA_AUTHENTICATION = 'cda_cred'
    /**
     * Trellix signing server credentials
     */
    static final String CREDENTIAL_FOR_TRELLIX_SIGNING_SERVER = 'trellix_signing_server'
    /**
     * Used to access the orbit signing service in AWS
     */
    static final String CREDENTIAL_FOR_SIGNING_SERVICE = 'signing_service_credential'
    /**
     * Public ssh key passed to ePortal to be added to the build nodes
     */
    static final String ORBIT_NODE_PUBLIC_SSH = 'orbit_node_public_ssh'
    /**
     * Returns the names of all the credentials
     * @return the names of all the credentials
     */
    @NonCPS
    @Deprecated
    static getAllNames() {
	CredentialStore.class
		.getDeclaredFields()
		.findAll { Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers) }
		.collect { it.name }
    }
}