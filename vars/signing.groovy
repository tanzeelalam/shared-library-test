import com.mcafee.orbit.Credentials.CredentialStore
import com.mcafee.orbit.Utils.EmailUtils
import com.mcafee.orbit.Deprecated

/**
 * Generic code signing method.
 *
 * By default this method will sign the specified files using
 * authenticode signing with the current sha2 certificate.
 *
 * By passing additional options, you can over-ride the signing
 * <code>type</code>, <code>certificate</code> and <code>configuration</code>.
 *
 * <h4>Sample usage:</h4>
 * Sign two files using the current sha2 certificate using sha2 signing type.
 * <pre>
 * signing(files: ['path/to/file1', 'path/to/file2'])
 * </pre>
 * Sign a file using the current skyhigh certificate using sha2 signing type.
 * <pre>
 * signing(
 *   files: ['path/to/file1'],
 *   customCert: 'skyhigh',
 * )
 * </pre>
 * Sign a file using the next sha2 certificate using sha2 signing type.
 * <pre>
 * signing(
 *   files: ['path/to/file1'],
 *   configuration: 'next',
 *   type: 'sha2'
 * )
 * </pre>
 * Sign a file using the next sha2 certificate using sha2 signing type and Remove any Existing signatures on the File
 * <pre>
 * signing(
 *   files: ['path/to/file1'],
 *   configuration: 'next',
 *   type: 'sha2',
 *   removeExistingSignatures: true
 * )
 * </pre>
 * Sign a file using the current sha2 certificate
 * using sha2 type with cross signing.
 * <pre>
 * signing(
 *   files: ['path/to/file1'],
 *   type: 'sha2_cs',
 * )
 * </pre>
 * Over-ride all options and sign a file using the next skyhigh certificate
 * using sha2 type with cross signing.
 * <pre>
 * signing(
 *   files: ['path/to/file1'],
 *   customCert: 'skyhigh',
 *   configuration: 'next',
 *   type: 'sha2_cs',
 * )
 * </pre>
 *
 * See also https://confluence.trellix.com/display/PDS/Product+Certificate+Mappings+on+FastSign+Server
 *
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param customCert [T:String] [OPTIONAL] Over-ride the default signing certificate. [DEFAULT:null]
 * @param configuration [T:String] [OPTIONAL] Over-ride the default signing configuration.
 *                      One of <code>"current"</code>, <code>"previous"</code>, <code>"next"</code> or <code>"test"</code>.
 *                      [DEFAULT:"current"]
 * @param removeExistingSignatures [T:Boolean] [OPTIONAL] Removes <strong>ANY</strong> existing Signatures on the files. [DEFAULT : false]
 * @param type [T:String] [OPTIONAL] Over-ride the default signing type. [DEFAULT:"sha2"]
 */
public void call(Map args) {
    log.debug(args)
    arguments(args)
      .addString('configuration', 'current')
      .addString('customCert', null)
      .addString('type', 'sha2')
      .addBoolean('removeExistingSignatures', false) // Applicable only to sha2 and sha2_ph sign types when signing in AWS
      .addList('files')
      .parse()
    def customCert = args.certificate // Undocumented Parameter for backward compatibility
    if(args.customCert) {
        customCert = args.customCert
    }
    def config_details = radar.getSigningConfiguration(args.type, args.configuration, customCert)
    def cert_name = config_details.Certificate.Name
    log.info("Resolved signing configuration to certificate with name '$cert_name'.")
    pypeline.signing(cert_name, args.type, args.files, args.removeExistingSignatures)
}

/**
 * Sign files with both SHA1 and SHA256 certificates.
 * SHA1 cross signing has been deprecated and will be skipped.
 *
 * <h4>Sample Usage:</h4>
 * <pre>
 * signing.dualSigning(filesToSign: ["path/to/file1","path/to/file2"])
 * </pre>
 * <pre>
 * signing.dualSigning(
 *     filesToSign:["path/to/file1","path/to/file2"],
 *     crossSign: true,
 *     pageHash: true,
 * )
 * </pre>
 * @param filesToSign [T:List&lt;String&gt;] The list of files to sign.
 * @param crossSign [T:boolean] [OPTIONAL] Set true if you want to enable cross signing for all files.
 * By default .sys and .cat files are cross signed.
 * @param pageHash [T:boolean] [OPTIONAL] Set true if you want to enable page hashing when applicable. [DEFAULT:false]
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void dualSigning(def args) {
    deprecated()
    def type = getSignType('sha2', args)
    call(
        configuration: args.certConfig,
        files: args.filesToSign,
        type: type,
    )
}

/**
 * MA sign XML files
 *
 * <h4>Sample Usage:</h4>
 * <pre>
 * signing.MASigning(files: ["path/to/file1","path/to/file2"])
 * </pre>
 * <pre>
 * signing.MASigning(files: "path/to/file1,path/to/file2")
 * </pre>
 *
 * @param files [T:List|String] XML files for which the .sig file is to be generated. Can be passed as a
 * List or comma separated string. Minimum of 1 file needs to be passed.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void MASigning(def args) {
    deprecated()
    List files = []
    if (args.files.class == String) {
        files = args.files.split(',')
    } else {
        files = args.files
    }
    call(
        configuration: args.certConfig,
        files: files,
        type: 'masign',
    )
}

/**
 * Obsolete MA signing method.
 */
@Deprecated(["signing()"])
public void serverSignFileMA(String fileToSign) {
    if (!config('ALLOW_DEPRECATED_METHODS')?.toBoolean()) {
        error("serverSignFileMA has been deprecated. Use MASigning")
    }
    deprecated()
    MASigning(files: [fileToSign])
}

/**
 * Obsolete MA signing method.
 */
@Deprecated(["signing()"])
public void localMASign(def args) {
    if (!config('ALLOW_DEPRECATED_METHODS')?.toBoolean()) {
        error("localMASign has been deprecated. Use MASigning")
    }
    deprecated()
    MASigning(args)
}

/**
 * Sign a single ePO extension file using FIPS options and ORION options
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.epoFipsExtSignAFile("file_to_be_signed", "E:\\Tools\\j2sdk_160_00\\bin\\Jarsigner.Exe")
 * </pre>
 * @param fileToSign [T:String] The file to be signed.
 * @param [T:String][OPTIONAL] Path to the jarSigner according to the java version used for the build. [DEFAULT:"E:\\Tools\\j2sdk_160_00\\bin\\jarsigner.exe"]
 */
@Deprecated(["signing()"])
public void epoFipsExtSignAFile(String fileToSign, String jarsignerAbsPath = "") {
    deprecated()
    epoFipsExtSignAFile([files: [fileToSign]])
}
/**
 * Sign ePO fips extension files.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.epoFipsExtSignAFile(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]

 */
@Deprecated(["signing()"])
public void epoFipsExtSignAFile(def args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'fips_ext_sign',
    )
}
/**
 * Sign ePO extension files on the remote signing server
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.ePoExtSignAFile(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void ePoExtSignAFile(def args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'epo_ext_sign',
    )
}

/**
 * Obsolete epo extension signing method.
 */
@Deprecated(["signing()"])
public void localePoExtSignAFile(Map args) {
    if(!config('ALLOW_DEPRECATED_METHODS')?.toBoolean()) {
        error("localePoExtSignAFile has been deprecated. Use ePoExtSignAFile")
    }
    deprecated()
    ePoExtSignAFile(args)
}

/**
 * Cross sign files along with SHA2 digital signing
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.SignLssMcAfeeSHA2_CS(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void SignLssMcAfeeSHA2_CS(Map args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'sha2_cs',
    )
}

/**
 * Sign files with SHA2 digital signing
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.SignLssMcAfeeSHA2(files: ["file.txt"])
 * </pre>
 * @param files List - files to be signed
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void SignLssMcAfeeSHA2(Map args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'sha2',
    )
}

/**
 * Provides 2048-bit ePO package signing. Uses an internally generated McAfee certificate.
 *
 * <p>If signType is "encrypt", it takes fileName/extension to be encrypted and runs eposign /encrypt.</p>
 *
 * <p>If signType is package/autopackage, it takes xmlFileName (package catalog),
 *     files to encrypt, license type and:
 *     <ol>
 *          <li>Updates the pkgcatalog.xml with License Type</li>
 *          <li>Create package (.z file) from the provided package catalog (xml file)</li>
 *          <li>Signs the created package (.z file)</li>
 *      </ol>
 * </p>
 * <h4>Sample usage:</h4>
 * <pre>
 * // Encrypt only
 * epoSign46 (
 *    signType: "encrypt",
 *    fileToEncrypt: "_PATH_\\data.mcs",
 * )
 * </pre>
 * <pre>
 * // Package / autopackage
 * signing.epoSign46 (
 *     signType: "package",
 *     fileToEncrypt:".mcs",
 *     xmlName:"_PATH_\\PkgCatalog.xml",
 * )
 * </pre>
 * <pre>
 * signing.epoSign46 (
 *     signType: "autopackage",
 *     fileToEncrypt: ".mcs",
 *     xmlName: "_PATH_\\PkgCatalog.xml",
 * )
 * dir(epoPkgDir) {
 *     signing.epoSign46 (
 *         signType: "autopackage",
 *     )
 * }
 * </pre>
 * @param signType [T:String] One of <code>"encrypt"</code>, <code>"package"</code> or <code>"autopackage"</code>. [DEFAULT:"package"]
 * @param fileToEncrypt [T:String] Name of the file/extentions to Encrypt. Mandatory when using <code>"encrypt"</code> signType.
 * Optional when signType is <code>"package"</code> or <code>"autopackage"</code>. Defaults to <code>".mcs"</code> for package/autopackage.
 * @param xmlName [T:String] [OPTIONAL] package name of the xmlFile to Sign. Defaults to <pwd>/PkgCatalog.xml
 * @param licenseType [T:String] [OPTIONAL] License Type to be updated in pkgcatalog.xml. [DEFAULT:"LICENSED"]
 * @param sha1Only [T:List] [OPTIONAL] list of files to only sha1 (DSA 1024 ) sign. By default RSA 2048 is
 * used.
 */
public void epoSign46(arg) {
    /*
        If signType is encrypt ,
              it takes fileName/extenstion to be encrypted and runs eposign /encrypt
        If signType is package/autopackage ,
              it takes xmlFileName ( package catalog ) , files To encrypt , license type and :
              1. updates the pkgcatalog.xml with License Type
              2. Create package (.z file) from the provided package catalog (xml file)
              3. Signs the created package (.z file)
     */
    assert (!utils.isNullOrEmptyString(arg.signType)): "ERROR arg.signType is empty"
    List supportedSignTypes = ["encrypt", "package", "autopackage"]
    String pkgZName = "PkgCatalog.z"
    String currentDir = pwd()
    String signType = arg.signType ? arg.signType.toLowerCase() : "package"
    assert supportedSignTypes.contains(signType):
        utils.getErrorMessage("ERROR: SIGNTYPE : ${signType} is not supported")

    String fileToEncrypt, xmlName, licenseType
    List sha1OnlyFiles
    Map xmlFileObj

    global()
    if (signType.equals("encrypt")) {
        if (!arg.fileToEncrypt || utils.isNullOrEmptyString(arg.fileToEncrypt)) {
            utils.getErrorMessage(
                "fileToEncrypt is empty. It is mandatory to provide file name/extenstion in " +
                    "case of encrypt only"
            )
        }
        fileToEncrypt = utils.formatPathForOS(arg.fileToEncrypt)
    } else {
        if (!arg.xmlName || utils.isNullOrEmptyString(arg.xmlName)) {
            log.info("xmlName was not passed. Will look for PkgCatalog.xml in current directory")
            xmlName = "${currentDir}\\PkgCatalog.xml"
        } else {
            xmlName = arg.xmlName
        }
        xmlName = utils.formatPathForOS(xmlName)
        fileToEncrypt = arg.fileToEncrypt ?: ".mcs"
        licenseType = arg.licenseType ?: "LICENSED"
        sha1OnlyFiles = arg.sha1Only ?:[]
    }

    if (isUnix()) {
        String unixDir = pwd()
        String unsigned_files = "unsigned_" + Math.abs(new Random().nextInt() % 1000) + 1
        String signed_files = "signed_" + Math.abs(new Random().nextInt() % 1000) + 1
        Map epoSignArgs = [:]
        epoSignArgs.put("signType", signType)

        stash includes:'**' , name:unsigned_files
        node(config('EPO_SIGN_NODE_LABEL')) { 
            dir("unsigned") {
                //Create a folder for the files that will be stashed
                bat 'mkdir stashed_files'
                String winDir = pwd()
                unstash unsigned_files
                def filesToStash = []
                String encryptRelativePath,xmlRelativePath
                if(signType.equals("encrypt")) {
                    //Encrypt does not accept wildcards. So it's either a relative or absolute path
                    //replace unix path with windows path.
                    encryptRelativePath = fileToEncrypt.replace("${unixDir}/", "")
                    epoSignArgs.put("fileToEncrypt", "${winDir}\\${encryptRelativePath}")
                    //Stage the encrypted file to be stashed
                    filesToStash.add("${winDir}\\${encryptRelativePath}")
                } else {
                    xmlRelativePath = xmlName.replace("${unixDir}/", "")
                    encryptRelativePath = fileToEncrypt.replace("${unixDir}/", "")
                    for (int i = 0; i < sha1OnlyFiles.size(); i++) {
                        sha1OnlyFiles[i] = sha1OnlyFiles[i].replace("${unixDir}/", "")
                    }
                    epoSignArgs.put("licenseType", licenseType)
                    epoSignArgs.put("xmlName", "${winDir}\\${xmlRelativePath}")
                    if (fileExists("${winDir}\\${encryptRelativePath}")) {
                        epoSignArgs.put("fileToEncrypt", "${winDir}\\${encryptRelativePath}")
                        //Stage the encrypted file to be stashed
                        filesToStash.add("${winDir}\\${encryptRelativePath}")
                    } else {
                        epoSignArgs.put("fileToEncrypt", "${encryptRelativePath}")
                        //Stage the encrypted file to be stashed
                        filesToStash.add("*${encryptRelativePath}")
                    }
                    for (int i = 0; i < sha1OnlyFiles.size(); i++) {
                        sha1OnlyFiles[i] = "${winDir}\\${sha1OnlyFiles[i]}"
                    }
                    epoSignArgs.put("sha1Only", sha1OnlyFiles)
                    //Stage the Pkg Z file to be stashed
                    filesToStash.add("${winDir}\\${pkgZName}")
                }
                this.epoSign46(epoSignArgs)
                //Copy all files to be stashed into the stage directory
                filesToStash.each {
                    bat "copy $it stashed_files"
                }
                //Stash all files found inside the folder
                dir("stashed_files") {
                    stash includes: "**", name: signed_files
                }
            }
        }
        unstash signed_files
        if(!signType.equals("encrypt")) {
            if(fileExists(xmlName)) {
                utils.silentCmd("rm ${xmlName}")
            }
        }
        return
    }

    String epoSignDir = global.path.WIN_EPO46_EPOSIGNSE_DIR
    String epoSignRSAZip = "${epoSignDir}\\sm2048McAfee.zip"
    String epoSignDSAZip = "${epoSignDir}\\smMcAfee.zip"
    String epoSignCmd = global.path.WIN_EPO46_EPOSIGNSE_EXE
    try {
        if (signType.equals("encrypt")) {
            log.debug("Encrypting files")
            String epoEncryptCmd = epoSignCmd + " /encrypt ${arg.fileToEncrypt}"
            utils.silentCmd(epoEncryptCmd)
            log.info("Finished encrypting file " + arg.fileToEncrypt)
            log.info("Adding build signing to Radar for epoSign46")
            radar.addBuildSigningInRadar(
                CertificateIdentity: "epoSign46",
                Tool: "eposignse",
                Status: "Success",
                FileName: pkgZName,
            )
        } else {
            log.debug("Packaging/Autopackaging files")
            String sha1OnlyOpt = ""
            if (arg.sha1Only && arg.sha1Only.size() > 0) {
                for (def eachFile in arg.sha1Only) {
                    sha1OnlyOpt = sha1OnlyOpt + "/Sha1Only:\"${eachFile}\" "
                }
            }

            xmlFileObj = jenkins.getFileAsObject(["fileName": xmlName])
            if (!xmlFileObj.EXISTS) {
                utils.getErrorMessage("catalog file " + xmlName + " does not exist")
            }

            //Make sure Pkgcatalog.xml  is writable and Update pkgcatalog.xml with the license type
            try {
                String updatePkgCatalogCmd = "attrib -r \"${xmlName}\" && ${epoSignCmd} " +
                    "/UpdatePkgCatalog \"${xmlName}\" /DistributionType:${licenseType}"
                utils.silentCmd(updatePkgCatalogCmd)
            } catch (Throwable e) {
                log.error("Could Not update Package catalog : ${xmlName}")
                rethrow(e)

            }

            //Create packageCatalog.z file from package catalog.xml
            try {
                String pkgCmd = "${epoSignCmd} /${signType} \"${xmlName}\" \"${fileToEncrypt}\" " +
                    "${sha1OnlyOpt}"
                utils.silentCmd(pkgCmd)
            } catch (Throwable e) {
                log.error(".z creation failed for ${xmlName}")
                rethrow(e)
            }

            //Sign the packageCatalog.z
            try {
                String pkgZSignCmd = epoSignCmd +
                    " /sign \"${xmlFileObj.PARENT_DIR}\\${pkgZName}\" " +
                    "/RSA2048:${epoSignRSAZip} /DSA1024:${epoSignDSAZip}"
                utils.silentCmd(pkgZSignCmd)
            } catch (Throwable e) {
                log.error("Signing failed for ${xmlFileObj.PARENT_DIR}\\${pkgZName}")
                rethrow(e)
            }

            //Verify the packageCatalog.z
            try {
                String pkgZVerifyCmd = epoSignCmd +
                    " /verify \"${xmlFileObj.PARENT_DIR}\\${pkgZName}\" " +
                    "/RSA2048:${epoSignRSAZip} /DSA1024:${epoSignDSAZip}"
                utils.silentCmd(pkgZVerifyCmd)
            } catch (Throwable e) {
                log.error("verification failed for ${xmlFileObj.PARENT_DIR}\\${pkgZName}")
                rethrow(e)
            }
            log.info("Adding build signing to Radar for epoSign46")
            radar.addBuildSigningInRadar(
                CertificateIdentity: "epoSign46",
                Tool: "eposignse",
                Status: "Success",
                FileName: pkgZName,
            )

            //delete the xml file
            if(fileExists(xmlName)) {
                utils.silentCmd("del ${xmlName}")
            }
        }
    } catch (Throwable e) {
        log.error("Failed to execute ePO signing")
        log.info("Adding build signing to Radar for epoSign46")
        radar.addBuildSigningInRadar(
            CertificateIdentity: "epoSign46",
            Tool: "eposignse",
            Status: "Failure",
            Message: e.getMessage(),
            FileName: pkgZName,
        )
        rethrow(e)
    }
}

/**
 * Signs RPM packages using GPG.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.signRPM(
 *     filesToSign: ["file1", "file2", "file3"]
 * )
 * </pre>
 * @param filesToSign [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void signRPM(arg) {
    deprecated()
    assert (arg.filesToSign && arg.filesToSign.size() > 0):
            "ERROR:The list of files to sign arg.FilesToSign is not present / empty"
    call(
        configuration: arg.certConfig,
        files: arg.filesToSign,
        type: 'rpm_sign',
    )
}

/**
 * Signs debian packages using GPG.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.signDeb(
 *     filesToSign: ["file1", "file2", "file3"],
 * )
 * </pre>
 * @param filesToSign [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void signDeb(arg) {
    deprecated()
    assert (arg.filesToSign && arg.filesToSign.size() > 0):
        "ERROR:The list of files to sign arg.FilesToSign is not present / empty"
    call(
        configuration: arg.certConfig,
        files: arg.filesToSign,
        type: 'deb_sign',
    )
}

/**
 * Signs elf files using GPG.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.signElf(
 *     filesToSign: ["file1", "file2", "file3"],
 * )
 * </pre>
 * @param filesToSign [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void signElf(arg) {
    deprecated()
    assert (arg.filesToSign && arg.filesToSign.size() > 0):
            "ERROR:The list of files to sign arg.FilesToSign is not present / empty"
    call(
        configuration: arg.certConfig,
        files: arg.filesToSign,
        type: 'elf_sign',
    )
}
/**
 * Obsolete gpg signing method
 */
@Deprecated(["signing()"])
public void gpgSign(def arg) {
    error("The signing.gpgSign() method is obsolete. Please use the signing() method.")
}

/**
 * Signs files with a SHA256 signature and verifies the signature on the files.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.digitalSignSHA256(
 *     files: ["path/to/file1", "path/to/file2"],
 * )
 * </pre>
 * <pre>
 * signing.digitalSignSHA256(
 *     files: ["path/to/file1", "path/to/file2"],
 *     crossSign: true,
 *     pageHash:true,
 * )
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param crossSign [T:boolean] [OPTIONAL] Set to true to enable cross sign. [DEFAULT:false]
 * @param pageHash [T:boolean] [OPTIONAL] Set to true to enable cross sign. [DEFAULT:false]
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 * @param customCert [T:String] [OPTIONAL] Specify a custom sha2 cert eg. SKYHIGH. If not specified it defaults to SHA2 configuration in Radar
 */
@Deprecated(["signing()"])
public void digitalSignSHA256(def args) {
    deprecated()
    def type = getSignType('sha2', args)    
    call(
        certificate: args.customCert,
        configuration: args.certConfig,
        files: args.files,
        type: type,
    )
}

/**
 * Signs files with a PPL signature.
 *
 * <p>See https://confluence.trellix.com/display/ISecGEOSSTeam/PPL+Signing for more details.</p>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.PPLSigning(
 *     files: ["path/to/file1", "path/to/file2"]
 * )
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param pageHash [T:boolean] [OPTIONAL] Set to true to enable page hash. [DEFAULT:false]
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void PPLSigning(def args) {
    deprecated()
    def type = 'ppl_sign'
    if (args.pageHash) {
        type += '_ph'
    }
    call(
        configuration: args.certConfig,
        files: args.files,
        type: type,
    )
}

/**
 * Sign ePO extension files on the remote signing server.
 * Creates .sig file while signing the .jar.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.ePOFipsSigExtSignAFile(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void ePOFipsSigExtSignAFile(def args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'fips_sig_ext_sign',
    )
}
/**
 * This function is used for notarizing OSX binaries.
 * It takes the binaries from current build, notarizes them
 * and publishes them to the artifactory repository of
 * the parent build in the folder "notarizedFiles".
 *
 * It's advised to call this method from the delivery stage after invoking
 * <code>buildProfile.uploadToArtifactory()</code>
 * to avoid unnecessary overwrite of files while notarization build is going on.
 *
 * <h4>Sample usage</h4>
 * <pre>
 * signing.notarizeFiles(files: "path/to/file1,path/to/file2")
 * </pre>
 *
 * Over-ride the team email DL.
 * <pre>
 * signing.notarizeFiles(
 *   files: "path/to/file1,path/to/file2",
 *   mailDL: "your.team.dl@trellix.com",
 * )
 * </pre>
 *
 * Wait for notarization to complete.
 * <pre>
 * signing.notarizeFiles([files: "path/to/file1,path/to/file2"], true)
 * </pre>
 *
 * @param files Comma separated list of files to notarize
 * @param mailDL Over-ride env.EMAIL_DL for receiving notarization status via email
 * @param wait Optionally, wait for the notarization to complete. [DEFAULT:false]
 */
public void notarizeFiles(Map args, boolean wait=false) {
    assert !utils.isNullOrEmptyString(args.files): "ERROR notarizeFiles: files to notarize not provided."
    def splitJobName = utils.splitJenkinsJobName(env.JOB_NAME)
    String _devEmailDL
    String _componentName = splitJobName.component
    String _orbitBuildOverrride = "${env.BUILDVERSION},${orbit.buildNumber()}"
    String _files = args.files
    String repositoryFullName = artifacts.getRepositoryNameFromComponent(_componentName)
    String artifactoryRepositoryPath = repositoryFullName + "/" + _componentName + "_" + env.BUILDVERSION
    String _artifactoryOverride = utils.isNullOrEmptyString(env.ARTIFACTORY_REPOSITORY_OVERRIDE) ? artifactoryRepositoryPath : env.ARTIFACTORY_REPOSITORY_OVERRIDE
    if (utils.isNullOrEmptyString(args.mailDL)) {
        if (utils.isNullOrEmptyString(env.EMAIL_DL)) {
            utils.getErrorMessage("ERROR notarizeFiles: EmailDL not present, please set the env.EMAIL_DL in the Jenkinsfile else provide mailDL while calling this method.")
        } else if (!utils.isNullOrEmptyString(env.EMAIL_DL)) {
            _devEmailDL = EmailUtils.reformatEmails(env.EMAIL_DL);
        }
    } else {
        _devEmailDL = args.mailDL
    }

    artifacts.upload([
            files       : _files,
            flat        : "false",
            recursive   : "true"
    ])
    //Trigger notarization job
    try {
        orbit.triggerBuild(
            "Notarization",
            "MAC_Notarization",
            "Notarization_Job",
            "master",
            [
                execute: true,
                orbitComponentName: _componentName,
                orbitBuildOverrride: _orbitBuildOverrride,
                files: _files,
                devEmailDL: _devEmailDL,
                artifactoryRepository: _artifactoryOverride,
            ],
            wait
        )
    }
    catch (Throwable e) {
        log.debug(e.getMessage())
        log.info("Adding build signing to Radar for notarizeFiles")
        radar.addBuildSigningInRadar(
            CertificateIdentity: "notarizeFiles",
            Tool: "Notarization_Job",
            Status: "Failure",
            Message: e.getMessage(),
            FileName: _files,
        )
        rethrow(e)
    }
}
/**
 * Returns the name of the cert to be used for signing from the configuration.
 * <p><b>Sample usage </b>
 * <pre>
 * signing.getCertByType("CURRENT", "SHA2")
 * </pre>
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 * @param signType [T:String] The signType - SHA2, MASIGN
 */
@Deprecated(["signing()"])
private String getCertByType(String certConfig, String signType) {
    certConfig = certConfig.toUpperCase()
    signType = signType.toUpperCase()
    if (!config.array('ALLOWED_SIGNING_CERT_TYPE').contains(certConfig)) {
        certConfig = 'CURRENT'
    }
    return config("SIGNING_${signType}_CERT_${certConfig}")
}
/**
 * Signs an jar file.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.jarSign256(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void jarSign256(def args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'jar_sign_256',
    )
}
/**
 * Signs an SIA extension file.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.SIAExtSign(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void SIAExtSign(def args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'sia_ext_sign',
    )
}
/**
 * Signs an XML file.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.XMLSign(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void XMLSign(def args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'xml_sign',
    )
}
/**
 * Signs an EV file.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.EVSign(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void EVSign(def args) {
    deprecated()
    call(
        certificate: 'evsign',
        configuration: args.certConfig,
        files: args.files,
        type: 'sha2',
    )
}
/**
 * Signs an hlk file.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.HLKSign(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void HLKSign(def args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'hlk_sign',
    )
}
/**
 * Signs files with a Platform cert.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.PlatformSign(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * <pre>
 * signing.PlatformSign(
 *     files: ["path/to/file1", "path/to/file2"],
 *     crossSign: true,
 *     pageHash: true,
 * )
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param crossSign [T:boolean] [OPTIONAL] Set to true to enable cross sign. [DEFAULT:false]
 * @param pageHash [T:boolean] [OPTIONAL] Set to true to enable cross sign. [DEFAULT:false]
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void PlatformSign(def args) {
    deprecated()
    def type = getSignType('sha2', args)
    call(
        certificate: 'platform',
        configuration: args.certConfig,
        files: args.files,
        type: type,
    )
}
/**
 * FedRamp XML Signing.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.FedRampXMLSign(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void FedRampXMLSign(def args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'fed_xml_sign',
    )
}
/**
 * PCL Signing.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.PCLSign(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void PCLSign(def args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'pcl_sign',
    )
}
/**
 * SIACSR Signing.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.SIACSRSign(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void SIACSRSign(def args) {
    deprecated()
    call(
        configuration: args.certConfig,
        files: args.files,
        type: 'sia_csr_sign',
    )
}
/**
 * Signs an android file.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * signing.androidSign(files: ["path/to/file1", "path/to/file2"])
 * </pre>
 * @param files [T:List&lt;String&gt;] The list of files to sign.
 * @param customCert [T:String] [OPTIONAL] Specify a custom android cert eg. MEA. If not specified it defaults to ANDROIDSIGN configuration in Radar
 * @param certConfig [T:String] [OPTIONAL] One of <code>"CURRENT"</code>, <code>"NEXT"</code>, <code>"PREVIOUS"</code> or <code>"TEST"</code>. [DEFAULT:"CURRENT"]
 */
@Deprecated(["signing()"])
public void androidSign(def args) {
    deprecated()
    call(
        certificate: args.customCert,
        configuration: args.certConfig,
        files: args.files,
        type: 'android_sign',
    )
}

/**
 * Update the default signing type, if the pageHash or crossSign options are specified.
 *
 * @param type The default signing type.
 * @param args The options map.
 * @return The updated signing type.
 */
private def getSignType(def type, def args) {
    if (args.pageHash || args.crossSign) {
        type += '_'
        if (args.crossSign) {
            type += 'cs'
        }
        if (args.pageHash) {
            type += 'ph'
        }
    }
    return type
}

/**
 * Obsolete eCM signing method
 */
@Deprecated
private void serverSignFiles(def args) {
    error('The signing.serverSignFiles() method is no longer available.')
}

/**
 * Obsolete JAR signing method
 */
@Deprecated
private void jarSigning(Map args) {
    error('The signing.jarSigning() method is no longer available.')
}

/**
 * Obsolete sha1 signing method
 */
@Deprecated
private void SignLssMcAfeeSHA1_CS(args) {
    error("The SHA1 signing methods are no longer available.")
}

/**
 * Obsolete sha1 signing method
 */
@Deprecated
private void SignLssMcAfeeSHA1(args) {
    error("The SHA1 signing methods are no longer available.")
}

/**
 * Obsolete sha1 signing method
 */
@Deprecated
private void digitalSign(args) {
    error("The SHA1 signing methods are no longer available.")
}
