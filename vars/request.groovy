import com.mcafee.orbit.Utils.StringUtils
import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import net.sf.json.JSONNull

/**
 * Performs an HTTP request.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * // Simple GET request
 * request(url: 'http://example.com')
 *
 * // POST request with more options
 * request(
 *     url: 'http://example.com',
 *     method: 'POST',
 *     token: 'my_credential_id',
 *     headers: [Accept: 'text/plain'],
 *     params: [BuildRecordId: 12345],
 *     json: [Key: 'Value'],
 *     fail: true,
 *     error: 'Failed to POST the key value pair to example.',
 * )
 * </pre>
 * @param url [T:String] The URL to send the request to.
 * @param method [T:String] [OPTIONAL] The HTTP method: <code>"GET"</code>, <code>"POST"</code>, etc... [DEFAULT:"GET"]
 * @param token [T:String] [OPTIONAL] The credential ID of the Bearer token for authorization.
 * @param user [T:String] [OPTIONAL] The credential ID of the user for authorization.
 * @param headers [T:Map] [OPTIONAL] The map of HTTP header keys and values.
 * @param params [T:Map] [OPTIONAL] The map of query parameters to append to the URL.
 * @param data [T:String] [OPTIONAL] The text payload to send as the body of the request.
 * @param json [T:Map] [OPTIONAL] The JSON payload to send as the body of the request.
 * @param fail [T:boolean] [OPTIONAL] Whether to fail on error responses from the server.
 * @param master [T:boolean] [OPTIONAL] Whether to execute on the Jenkins master node.
 * @param error [T:String] [OPTIONAL] The error message to log when a request fails.
 * @return [T:Object|String] The server response as JSON, if it can be parsed, or as a string otherwise.
 */
def call(Map options) {
    try {
        arguments(options) {
            addString('url')
            addString('method', 'GET')
            addString('token', '')
            addString('user', '')
            addMap('headers', [:])
            addMap('params', [:])
            addMap('json', null)
            addString('data', null)
            addBoolean('fail', true)
            addBoolean('master', false)
            addString('error', 'Failed to execute HTTP request.')
            parse()
        }
        log.info("Performing HTTP $options.method request to '$options.url'.")
        def number = StringUtils.getPaddedRandomNumber()
        def credentials = []
        if (options.token) {
            // Here we should use the Jenkins Java API to check
            // the type of credential in order to support
            // secret string types as well as username/password.
            credentials += usernamePassword(
                credentialsId: options.token,
                usernameVariable: 'UNUSED',
                passwordVariable: 'REQUEST_TOKEN',
            )
        } else if (options.user) {
            credentials += usernamePassword(
                credentialsId: options.user,
                usernameVariable: 'REQUEST_USERNAME',
                passwordVariable: 'REQUEST_PASSWORD',
            )
        }
        withCredentials(credentials) {
            retry(3) {
                timeout(3) {
                    def request, response
                    if (options.master) {
                        request = "/tmp/http-${number}-request.json"
                        response = "/tmp/http-${number}-response.json"
                    } else {
                        request = "$env.WORKSPACE/.orbit/http-${number}-request.json"
                        response = "$env.WORKSPACE/.orbit/http-${number}-response.json"
                    }
                    List<String> command = [
                        'http',
                        '--request-file',
                        request.toString(),
                        '--response-file',
                        response.toString(),
                    ]
                    def data
                    if (options.master) {
                        new File(request).text = JsonOutput.toJson(options)
                        pypeline.executeOnJenkinsServer(command, [
                            REQUEST_TOKEN: env.REQUEST_TOKEN,
                            REQUEST_USERNAME: env.REQUEST_USERNAME,
                            REQUEST_PASSWORD: env.REQUEST_PASSWORD,
                        ])
                        data = new JsonSlurperClassic().parseText(new File(response).text)
                    } else {
                        writeJSON(file: request, json: options)
                        pypeline.executeOnNode(command)
                        data = fixNullInObject(readJSON(file: response))
                    }
                    if (options.fail && data.status >= 400) {
                        error("Server responded with HTTP status ${data.status}.")
                    }
                    return data.body
                }
            }
        }
    } catch (Throwable e) {
        throw new RuntimeException(options.error, e)
    }
}

/**
 * Shorthand for an HTTP GET request.
 */
def get(Map options) {
    call(options + [method: 'GET'])
}

/**
 * Shorthand for an HTTP POST request.
 */
def post(Map options) {
    call(options + [method: 'POST'])
}

/**
 * Shorthand for an HTTP PUT request.
 */
def put(Map options) {
    call(options + [method: 'PUT'])
}

/**
 * Fixes <code>null</code> values as strings in an object.
 *
 * @param object [T:Object] The object to fix.
 * @return [T:Object] An object with correct null values.
 */
@NonCPS
private def fixNullInObject(def object) {
    if (object instanceof Map) {
        return fixNullInMap(object)
    } else if (object instanceof List) {
        return fixNullInList(object)
    } else if (object instanceof String) {
        return fixNullInString(object)
    } else if (object instanceof JSONNull) {
        return null
    } else {
        return object
    }
}

/**
 * Fixes <code>null</code> values as strings in a map.
 *
 * @param object [T:Map] The map to fix.
 * @return [T:Map] A map with correct null values.
 */
@NonCPS
private def fixNullInMap(def object) {
    def result = [:]
    for (def i=0; i<object.size(); i++) {
        def key = object.keySet()[i]
        def value = object[key]
        result[key] = fixNullInObject(value)
    }
    return result
}

/**
 * Fixes <code>null</code> values as strings in a list.
 *
 * @param object [T:List] The list to fix.
 * @return [T:List] A list with correct null values.
 */
@NonCPS
private def fixNullInList(def object) {
    def result = []
    for (def i=0; i<object.size(); i++) {
        if (object[i] == "null") {
            result[i] = null
        } else {
            result[i] = fixNullInObject(object[i])
        }
    }
    return result
}

/**
 * Fixes <code>null</code> value as string.
 *
 * @param object [T:String] The string to fix.
 * @return [T:String|null] The original string or <code>null</code>.
 */
@NonCPS
private def fixNullInString(def object) {
    if (object == "null") {
        return null
    } else {
        return object
    }
}
