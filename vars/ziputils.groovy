/**
 * Creates a zip file using 7zip.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * ziputils.createZip(
 *     fileName: "artifact.7z",
 *     input: ".",
 * )
 * </pre>
 *
 * @param fileName [T:String] Name of the zip file to be created.
 * @param input [T:String] Comma separated string of files to be zipped (supports wildcards).
 * @param exclusions [T:String] [OPTIONAL] Comma separated string of files to be excluded. [DEFAULT:null]
 * @param password [T:String] [OPTIONAL] Password used to encrypt the contents of the zipped files. [DEFAULT:null]
 * @param encryptFilenames [T:boolean] [OPTIONAL] Whether to encrypt filenames as well as file contents. Only applicable if password is set and archive type is 7z. [DEFAULT:false]
 * @param debug [T:boolean] [OPTIONAL] Debug value to display listing of zip file. [DEFAULT:false]
 */
void createZip(Map args) {
    arguments(args)
        .addString('fileName')
        .addString('input')
        .addString('exclusions', null)
        .addString('password', null)
        .addBoolean('encryptFilenames', false)
        .addBoolean('debug', false)
        .parse()
    // 7zip seems to do just fine with any extension,
    // defaulting to 7z compression method, so this
    // check seems to be redundant and unnecessarily fatal.
    utils.validateFileFormat(args.fileName, '7z')
    try {
        maskPasswords(varPasswordPairs: [[password: args.password, var: 'PASSWORD']]) {
            String binary = getBinary()
            String flags = getFlags(args)
            String files = getFileList(args)
            log.info("Compressing '${args.fileName}'...")
            execute "$binary $flags a \"${args.fileName}\" $files"
            if (args.debug)
            {
                log.info("Listing contents of '${args.fileName}'...")
                execute "$binary $flags l \"${args.fileName}\""
            }
        }
    } catch (Throwable e) {
        log.error("Failed to create zip file '${args.fileName}'")
        rethrow(e)
    }
}

/**
 * Unzips a file to a target folder using 7zip.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * ziputils.unzipFile(
 *    fileName: "dependencies\\ma_packages\\MA500SDK.zip",
 *    targetDir: "SDKs\\CMA",
 * )
 * </pre>
 * @param fileName [T:String] Name of the zip file to be extracted.
 * @param targetDir [T:String] [OPTIONAL] Target folder in which the zip will be extracted. [DEFAULT:env.WORKSPACE]
 * @param password [T:String] [OPTIONAL] Password used to decrypt the contents of the zipped file. [DEFAULT:null]
 * @param debug [T:boolean] [OPTIONAL] Debug value to display listing of zip file. [DEFAULT:false]
 */
void unzipFile(Map args) {
    if (args.files) {
        // This is part of an old, undocumented API.
        // But people are using it, so can't remove it.
        args.files.each { Map it -> unzipFile(it) }
        return
    }
    arguments(args)
        .addString('fileName')
        .addString('targetDir', env.WORKSPACE)
        .addString('password', null)
        .addBoolean('debug', false)
        .parse()
    try {
        maskPasswords(varPasswordPairs: [[password: args.password, var: 'PASSWORD']]) {
            String binary = getBinary()
            String flags = getFlags(args)
            if (args.debug)
            {
                log.info("Listing contents of '${args.fileName}'...")
                execute "$binary $flags l \"${args.fileName}\""
            }
            log.info("Extracting '${args.fileName}'...")
            execute "$binary $flags x \"${args.fileName}\""
        }
    } catch (Throwable e) {
        log.error("Failed to extract '${args.fileName}' to '${args.targetDir}'.")
        rethrow(e)
    }
}

/**
 * Returns the path to the 7zip binary
 * @return The path to the 7zip binary
 */
private String getBinary() {
    if(isUnix()) {
        if (node.isMac()) {
            return '/usr/local/bin/7za'
        } else {
            return '7za'
        }
    } else {
        return 'C:\\Tools\\7-Zip\\7z.exe'
    }
}

/**
 * Returns 7z flags
 *
 * @param args The arguments from the parent method
 * @return 7z flags
 */
private String getFlags(Map args) {
    List flags = ['-y']
    if (args.targetDir) {
        flags.add("-o\"${args.targetDir}\"")
    }
    if (args.password) {
        flags.add("-p\"${args.password}\"")
        if (args.encryptFilenames) {
            if (args.fileName.endsWith('.7z')) {
                flags.add('-mhe+')
            } else {
                log.warn('ziputils cannot encrypt filenames in non-7z files')
            }
        }
    }
    if (args.exclusions) {
        flags.add(
            args.exclusions.split(',').collect {
                "-xr!\"${it.trim()}\""
            }.join(' ')
        )
    }
    return flags.join(' ')
}

/**
 * Pre-processes a comma separated list of files
 * so that it can be passed in to an executable
 * in a shell.
 *
 * Of course if you think about it for a moment, you'll
 * realise that this makes no sense, but we can't break
 * backwards-compatibility, so here we are...
 *
 * @param args The arguments from the parent method
 * @return A list of files as a string
 */
private String getFileList(Map args) {
    return args.input.split(',').collect {
        "\"${it.trim()}\""
    }.join(' ')
}
