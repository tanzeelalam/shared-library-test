/**
 * This static class is used as a cache.
 */
class Cache {
    // This list contains all the configuration values in Radar.
    static def values = []
}

/*
 * Loads the configurationvalues from Radar, if not yet available.
 */
private void init() {
    if (Cache.values.isEmpty()) {
        Cache.values = request.get(
            url: "${env.RADAR_API}/BuildConfiguration/GetValues",
            error: 'Failed to get configuration values from Radar.',
            token: 'orbit_radar_cred',
            master: true,
        )
        Cache.values += [
            Key: 'GITHUB_INSTANCES',
            Value: radar.getGithubInstances("${env.RADAR_API}/GithubInstance/getAll").collectEntries {
                [it.Name.toLowerCase(), it]
            },
            Groups: [],
        ]
        // Set environment variables from Radar build configuration
        def keys = [
            'ARTIFACTORY_INSTANCE',
            'ARTIFACTORY_TIMEOUT',
            'ARTIFACTORY_URL',
            'DEBUGLEVEL',
            'GIT_CLONE_TIMEOUT',
            'ORBIT_BUILD_ENV',
            'ORBIT_BUILD_ENV_OC',
        ].collectEntries { [it, it] }
        // The below are different in Radar and the Orbit environment
        keys += [
            'linux_signing_server': 'FS_LINUX_SIGNING_SERVER_URL',
            'mac_signing_server': 'FS_MAC_SIGNING_SERVER_URL',
            'RadarEnvironment' : 'RADAR_ENVIRONMENT',
            'skipECM' : 'SKIP_ECM',
            'windows_signing_server': 'FS_WINDOWS_SIGNING_SERVER_URL',
        ]
        for (def item in keys.entrySet()) {
            if (env[item.key] == null) {
                // set the env variable if it has no value
                env[item.key] = call(item.value)
            }
        }
    }
}

/**
 * Retrieve the value of a Radar setting.
 * See also: https://radar.orbit.corp.entsec.com/Admin/Configuration
 *
 * @param key [T:String] The key of the value to return.
 * @return [T:String]
 */
String call(String key) {
    init()
    return Cache.values.find { it.Key == key }.Value
}

/**
 * Retrieve a group of Radar settings as a Map.
 * See also: https://radar.orbit.corp.entsec.com/Admin/Configuration
 *
 * @param key [T:String] The key of the value to return.
 * @return [T:Map]
 */
Map group(String group) {
    init()
    return Cache.values.findAll { it.Groups.contains(group) }
        .collectEntries { [it.Key, it.Value] }
}

/**
 * Retrieve a group of Radar settings as an Array.
 * The keys of the values will be absent in the result.
 * See also: https://radar.orbit.corp.entsec.com/Admin/Configuration
 *
 * @param group [T:String] The group name.
 * @return [T:List]
 */
List array(String group) {
    init()
    return Cache.values.findAll { it.Groups.contains(group) }
        .collect { it.Value.toString() }
}
