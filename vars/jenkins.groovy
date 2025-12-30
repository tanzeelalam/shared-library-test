import org.apache.commons.io.FilenameUtils
import org.mcafee.orbit.CauseChain.CauseChain

/**
 * Finds and returns the cause of the current build
 *
 * <p>
 *     If the current build was kicked off via another build, constructs a chain of build causes until finding
 *     the one started by an outside source.
 *     Also returns commit info if initial job is triggered by a git change.
 * </p>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * buildChain = jenkins.getCauseChain()
 * </pre>
 * @return [T:String] in html format outlining build cause chain and commit info if applicable
 */
private String getCauseChain() {
    CauseChain chain = new CauseChain(
        this
    )
    chain.init()
    chain.format()
}
/**
 * Method to quickly include a file to the list to tbe archived in Jenkins job
 *
 * <p>jenkins.archiveFile(filepath) is equivalant to archive([includes:filepath])</p>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * jenkins.archiveFile(filepath)
 * </pre>
 * @param [T:String] File path to archive in Jenkins
 */
private void archiveFile(filepath) {
    log.info("Archiving file " + filepath)
    archive([includes: filepath])
}

/**
 For a given filepath, this method returns an object with evaluated attributes PREFIX,BASENAME,ORIGINAL,FILENAME,PARENT_DIR,ABSOLUTE_PATH,EXTENSION,PRESENT_UNDER_JENKINS_WORKSPACE
 <p>
 <b>Sample usage:</b>
 <br>
 <br>
 <pre>
 print jenkins.getFileAsObject("BuildResults\\Release\\Win32\\nailite.dll")
 </pre>
 <p>
 Output will be :
 <pre>{EXISTS=true, PREFIX=, BASENAME=nailite, ORIGINAL=BuildResults\Release\Win32\nailite.dll, FILENAME=nailite.dll, PARENT_DIR=c:\Jenkins\workspace\EP\ESP_CHAIN\ESP\esp_src\Packaging\BuildResults\Release\Win32, ABSOLUTE_PATH=c:\Jenkins\workspace\EP\ESP_CHAIN\ESP\esp_src\Packaging\BuildResults\Release\Win32\nailite.dll, EXTENSION=dll, PRESENT_UNDER_JENKINS_WORKSPACE=true}</pre>
 @param String fileName name of the file to be processed
 @return object with attributes
 */
private getFileAsObject(arg) {
    assert arg.fileName: "ERROR : arg.fileName " + arg.fileName + " not provided"
    def filepath = arg.fileName
    def fileAttr = [:]

    if (!fileExists(filepath)) {
        fileAttr.put("EXISTS", false)
        return fileAttr
    } else {
        fileAttr.put("EXISTS", true)
    }

    String currentDirectory = pwd()
    String parentDir = FilenameUtils.getFullPathNoEndSeparator(filepath)
    String fileNameOnly = FilenameUtils.getName(filepath)
    String prefix = FilenameUtils.getPrefix(filepath)
    String basename = FilenameUtils.getBaseName(filepath)

    if (parentDir == "" || parentDir == null || prefix == null || prefix == "") {
        parentDir = currentDirectory
    }

    String absolutePath = FilenameUtils.concat(parentDir, filepath)
    //incase filepath ="dira/a.exe" absolutepath = currdir+dira/a.exe
    parentDir = FilenameUtils.getFullPathNoEndSeparator(absolutePath)

    if (isUnix()) {
        parentDir = FilenameUtils.separatorsToUnix(parentDir)
        absolutePath = FilenameUtils.separatorsToUnix(absolutePath)
    } else {
        parentDir = FilenameUtils.separatorsToWindows(parentDir)
        absolutePath = FilenameUtils.separatorsToWindows(absolutePath)
    }
    fileAttr.put("PREFIX", prefix)
    fileAttr.put("BASENAME", basename)
    fileAttr.put("ORIGINAL", filepath)
    fileAttr.put("FILENAME", fileNameOnly)
    fileAttr.put("PARENT_DIR", (parentDir))
    fileAttr.put("ABSOLUTE_PATH", (absolutePath))
    fileAttr.put("EXTENSION", FilenameUtils.getExtension(filepath))
    log.debug('env.WORKSPACE: ' + env.WORKSPACE)
    log.debug('absolutePath: ' + absolutePath)
    fileAttr.put("PRESENT_UNDER_JENKINS_WORKSPACE", FilenameUtils.directoryContains(env.WORKSPACE, absolutePath))

    return fileAttr
}

/**
 * Returns `true` if the build is a replay
 */
@NonCPS
private boolean isReplay(def currentBuild) {
    return currentBuild.rawBuild.getCauses().any{ cause ->
        cause.toString().contains(
            "org.jenkinsci.plugins.workflow.cps.replay.ReplayCause"
        )
    }
}

/**
 * Checks if the job configuration uses an SCM or inline script
 * <h4>Sample usage:</h4>
 * <pre>
 * boolean inline_script = jenkins.isInlineScript()
 * </pre>
 */
private boolean isInlineScript() {
    log.debug("Checking if the job is using an inline script...")
    boolean inline = false
    try {
        inline = Jenkins.instance.getItemByFullName(env.JOB_NAME).definition.script.any()
        log.info("INFO: This job is using an inline script")
    } catch (Throwable e) {
        log.info("INFO: This job is using an SCM script")
    }
    return inline
}
