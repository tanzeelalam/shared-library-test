/**
 #
 */
private def escapeCLI (String cli){
    if (isUnix()) {
        return cli
    }
    else
    {
        return (this.windowsCLIEscapeString(cli))
    }
}



/**
 Attempts to escape characters in a string for use with Windows CLI (required for Orbit MBP jobs)

 <p>
 <b>Sample usage:</b>
 <br>
 <pre> curlCommand  =  utils.windowsCLIEscapeString(curlCommand)
 ])

 Background: Windows command line calls from Jenkins are equivalent to batch files with the
 strings double-quoted.


 Logic:
 This method will only escape single characters of strings that do not already
 contain the logic to escape the character.
 e.g.,
 The following string would be valid in Unix like systems but not on the Windows command line
 String cmdline = “curl -X \”https://www.batman.com/?%%zipFile.zip\””

 Additional, running this attempting to use this method against this string would not work,
 since it already a valid string (i.e., the second % is escaped by %%)

 assert (cmdline == utils.windowsCLIEscapeString(cmdline))

 Caveats:
 *: Additional escaping may be required for some windows command line utilities e.g., find,
 findstr and some for loops.  See http://www.robvanderwoude.com/escapechars.php for more details.

 * The method does not (currently) attempt to escape ^ characters, since they are also used to
 escape other characters (>, |, ^, &). Additional work to write more complex escape logic tracked
 in Orbit-<****>.

 Escape logic:
 (String in) .... (String out)

 %  .... %%
 %% .... %%
 \  .... \\
 \\ .... \\
 >  ....   ^>
 >> .... ^>^>
 ^> ....   ^>
 | .... ^|
 ^| .... ^|
 & .... ^&
 ^& .... ^&

 </pre>
 @param cliCommandString A command line string which may need to be escaped
 @return winCLIString A command line string which can be used in Windows CLI
 */
private String windowsCLIEscapeString(String cli){
    log.debug("Entering escapeStrescapeStringWindowsCLIingWindowsCLI")

    //input validation
    assert cli instanceof String
    assert !(utils.isNullOrEmptyString(cli)) //test common string null check
    assert !cli.isEmpty() //test is it empty
    assert !cli.trim().isEmpty() //test is it only empty whitespace

    //define and/or assign to local method variables
    String unEscapedCLI = cli
    String winCLIString

    log.debug("attempting to escape:\n" + unEscapedCLI)

    unEscapedCLI = unEscapedCLI.replaceAll('%%','%') //replace any escaping already done to string
    unEscapedCLI = unEscapedCLI.replaceAll('\\^>','>') //replace any escaping already done to string
    unEscapedCLI = unEscapedCLI.replaceAll('\\^<','<') //replace any escaping already done to string
    //unEscapedCLI = unEscapedCLI.replaceAll('\\\\','\\') //replace any escaping already done to string
    unEscapedCLI = unEscapedCLI.replaceAll('\\^\\|','|') //replace any escaping already done to string
    winCLIString = unEscapedCLI
    winCLIString = unEscapedCLI.replaceAll('%','%%') //escape %
    winCLIString = winCLIString.replaceAll('>','^>') //escape >
    winCLIString = winCLIString.replaceAll('<','^<') //escape <
    //winCLIString = winCLIString.replaceAll('\\','\\\\') //escape /
    winCLIString = winCLIString.replaceAll('\\|','^|' ) //rescape |

    //print to log the return string string if debug enabled
    log.debug("returning escaped string:\n" + winCLIString)
    return winCLIString
}