/**
 * Triggers an Orbit job.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * orbit.triggerBuild(
 *    "Radar",
 *    "Radar_Backend",
 *    "MB_Job",
 *    "master",
 *    [
 *        "Bullseye": "true",
 *        "Arch": "x64",
 *    ]
 * )
 * </pre>
 * @param productName [T:String] The product name.
 * @param componentName [T:String] The component name.
 * @param jobName [T:String] The job name to be triggered.
 * @param wait [T:boolean] [OPTIONAL] Wait for the build to complete. [DEFAULT:false]
 * @param timeout [T:int] [OPTIONAL] How long to wait for the build to complete in seconds. [DEFAULT:3600]
 * @param branch [T:String] [OPTIONAL] The branch name if the job is a multi branch pipeline. [DEFAULT:null]
 * @param parameters [T:Map] [OPTIONAL] The list of parameters in a key value pair. [DEFAULT:null]
 * @return [T:Map] A map containing the <code>JobUrl</code> and <code>orbitGUID</code> keys.
 */
Map triggerBuild(String productName, String componentName, String jobName, String branch = null, Map parameters = null, boolean wait=false, int timeout=3600) {
    try {
        //In case params is null set it to an empty map
        parameters = parameters ?: [:]
        log.info(
                "triggerBuild arguments provided: Product name: ${productName}, " +
                        "Component name: ${componentName}, " +
                        "Job name: ${jobName}, " +
                        "Branch: ${branch}, " +
                        "Parameters: ${parameters.size()}" +
                        "Wait: ${wait}"
        )
        //Get job id
        int jobId = radar.getBuildSystemJobId(productName, componentName, jobName)

        log.info("Job id ${jobId} found.")

        //Trigger the job
        return radar.triggerOrbitBuild(jobId, branch, parameters, wait, timeout)
    }
    catch(Throwable e) {
        log.info("Failed to trigger build: " + e.message)
        //Send email
        notify.NotifyOnTriggerFailure(
                productName,
                componentName,
                jobName,
                branch ?: "N/A",
                parameters.size().toString(),
                e.message
        )
        throw e
    }
}

/**
 * Private class to store orbit specific values.
 * Allows for the values to be set only once.
 */
final class Store {
    private static int buildRecordId = 0
    private static String buildSha = null
    private static String buildNumber = null
    private static int buildNumberOnly = 0
    private static int packageNumber = 0
    private static Map buildNodes = [:]

    /**
     * Set the build record id.
     *
     * @param value The value to set the build record id to.
     */
    static synchronized void setBuildRecordId(def value) {
        if (this.@buildRecordId == 0) {
            this.@buildRecordId = value as int
        } else {
            throw oops("build record id")
        }
    }

    /**
     * Set the build sha.
     *
     * @param value The value to set the build sha to.
     */
    static synchronized void setBuildSha(def value) {
        if (this.@buildSha == null) {
            this.@buildSha = value as String
        } else {
            throw oops("build sha")
        }
    }

    /**
     * Set the build number.
     *
     * @param value The value to set the build number to.
     */
    static synchronized void setBuildNumber(def value) {
        if (this.@buildNumber == null) {
            this.@buildNumber = value as String
        } else {
            throw oops("build number")
        }
    }

    /**
     * Set the build number only.
     *
     * @param value The value to set the build number only to.
     */
    static synchronized void setBuildNumberOnly(def value) {
        if (this.@buildNumberOnly == 0) {
            this.@buildNumberOnly = value as int
        } else {
            throw oops("build number only")
        }
    }

    /**
     * Set the package number.
     *
     * @param value The value to set the package number to.
     */
    static synchronized void setPackageNumber(def value) {
        if (this.@packageNumber == 0) {
            this.@packageNumber = value as int
        } else {
            throw oops("package number")
        }
    }

    /**
     * Set the id of a build node by name.
     *
     * @param name The name of the build node.
     * @param id The id of the build node.
     */
    static synchronized void addBuildNode(def name, def id) {
        if (!this.@buildNodes[name]) {
            this.@buildNodes[name] = id
        } else {
            throw oops("build node id")
        }
    }

    /**
     * Get a exception to throw when the user is trying
     * to modify a value that has already been set.
     *
     * @param name The name of the item.
     */
    static Exception oops(String name) {
        return new IllegalStateException(
            "The value '$name' has already been set and cannot be modified."
        )
    }
}

/**
 * Get and optionally set the build record id.
 *
 * @param value The value to set the build record id to.
 * @return The build record id
 */
private def buildRecordId(def value = 0) {
    if (value) {
        Store.buildRecordId = value
        env.BUILD_RECORD_ID = value
    }
    return Store.buildRecordId
}

/**
 * Get and optionally set the build sha.
 *
 * @param value The value to set the build sha to.
 * @return The build sha
 */
private def buildSha(def value = '') {
    if (value) {
        Store.buildSha = value
        env.BUILD_SHA = value
    }
    return Store.buildSha
}

/**
 * Get and optionally set the build number.
 *
 * @param value The value to set the build number to.
 * @return The build number
 */
def buildNumber(def value = '') {
    if (value) {
        Store.buildNumber = value
        env.ORBIT_BUILD_NUMBER = value
    }
    return Store.buildNumber
}

/**
 * Get and optionally set the build number only.
 *
 * @param value The value to set the build number only to.
 * @return The build number only
 */
def buildNumberOnly(def value = 0) {
    if (value) {
        Store.buildNumberOnly = value
        env.ORBIT_BUILD_NUMBER_ONLY = value
    }
    return Store.buildNumberOnly
}

/**
 * Get and optionally set the package number.
 *
 * @param value The value to set the package number to.
 * @return The package number
 */
def packageNumber(def value = 0) {
    if (value) {
        Store.packageNumber = value
        env.ORBIT_PACKAGE_NUMBER = value
    }
    return Store.packageNumber
}

/**
 * Get and optionally set the id of a node given its name.
 *
 * @param name The name of the build node.
 * @param id The id to build node.
 * @return The id of the build node
 */
private def buildNode(def name, def id = 0) {
    if (id) {
        Store.addBuildNode(name, id)
    }
    return Store.buildNodes[name]
}
