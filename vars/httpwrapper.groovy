import com.mcafee.orbit.Credentials.CredentialStore
import com.mcafee.orbit.Deprecated

/**
 * Wrapper for Curl POST command
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * String result = httpwrapper.orbitPostCurl(
 *    url: myURL,
 *    fileName: myFile,
 *    outputFile: tempFileName,
 * )
 * </pre>
 * @param url [T:String] URL to connect to
 * @param fileName [T:String] Input file to use for the curl post
 * @param outputFile [T:String] Output file to use for the curl post result
 * @param customHeader [T:String] [OPTIONAL] a custom header to use for the curl post
 * @return [T:String] Text output of curl post
 */
@Deprecated(["request()", "request.post()"])
public String orbitPostCurl(arg) {
    deprecated()
    global()
    log.debug(arg)
    //input validation
    assert !(utils.isNullOrEmptyString(arg.url)) //test common string null check
    assert arg.url instanceof String
    assert !arg.url.isEmpty() //test is it empty
    assert !arg.url.trim().isEmpty() //test is it only empty whitespace

    assert !(utils.isNullOrEmptyString(arg.fileName)) //test common string null check
    assert arg.fileName instanceof String
    assert !arg.fileName.isEmpty() //test is it empty
    assert !arg.fileName.trim().isEmpty() //test is it only empty whitespace

    String curlResult = '{}'
    //define and/or assign to local method variables
    try {
        String curlOpts = ' -X POST -d @'
        String postHeader = ' --header "Content-Type:application/json"'
        String curlUrl = ' "' + arg.url + '"'

        //If custom header is passed in then use it
        if(!utils.isNullOrEmptyString(arg.customHeader))
        {
            postHeader += ' ' + arg.customHeader
        }

        String curlCmd = curlOpts + arg.fileName + ' ' + curlUrl + postHeader
        //+ '> ' + arg.outputFile

        curlResult = this.orbitCurl([curlCmd: curlCmd, outFile: arg.outputFile])
    } catch (Throwable e) {
        rethrow(e, 'Failed to execute CURL command', true)
    }
    log.debug("Exiting orbitPostCurl")
    return curlResult
}

/**
 * Wrapper to execute a Curl command
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * String curlResult = httpwrapper.orbitCurl(curlCmd: curlCmd)
 * </pre>
 * @param curlCmd [T:String] Contains curl options & URL, file, etc.
 * @param customHeader [T:String] [OPTIONAL] Custom header to use for the curl command
 * @param escape [T:boolean] [OPTIONAL] Whether to escape % characters, if running on a windows node
 * @return [T:String] Text output of curl command
 */
@Deprecated(["request()", "request.get()"])
public String orbitCurl(def arg) {
    deprecated()
    global()
    log.debug(arg)
    if (arg.customHeader) {
        arg.curlCmd += ' ' + arg.customHeader
    }
    if (isUnix()) {
        arg.curlCmd = 'curl --fail ' + arg.curlCmd
    } else {
        if (arg.escape) {
            arg.curlCmd = arg.curlCmd.replace('%', '%%')
        }
        arg.curlCmd = '@' + global.path.WIN_CURL_EXE + ' --fail ' + arg.curlCmd
    }
    try {
        retry(3) {
            timeout(3) {
                return execute(
                    script: arg.curlCmd,
                    returnStdout: true,
                )
            }
        }
    } catch (Throwable e) {
        rethrow(e, 'Failed to execute CURL command')
    }
}
