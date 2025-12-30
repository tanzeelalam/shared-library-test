/**
 * Creates an ePO deployable package from the given directory
 *
 * <ol>
 *     <li>Runs signing.epoSign46 on a directory path (with a catalog xml file inside the directory).</li>
 *     <li>Creates a signed .z file from the xml catalog file.</li>
 *     <li>Zips the contents of the directory. The name of the zip file is value of the <code>epoPkgName</code> parameter.</li>
 * </ol>
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * epo.createEPO2048Pkg(
 *     dirToPkg: '${packageRoot}',
 *     epoPkgName: '/path/to/epo_pkg.zip',
 *     licenseType: 'LICENSED',
 * )
 * </pre>
 * @param epoPkgName [T:String] Filepath for the ePO package. If a relative path is given, the file
 * is created relative to the directory to package.
 * @param dirToPkg [T:String] [OPTIONAL] Path to the directory to create package from. [DEFAULT:pwd()]
 * @param pkgCatalogXml [T:String] [OPTIONAL] Name of the catalog File. [DEFAULT:PkgCatalog.xml]
 * @param licenseType [T:String] [OPTIONAL] License type to be updated in PkgCatalog.xml. [DEFAULT:LICENSED]
 */
public void createEPO2048Pkg(arg) {
    assert (!utils.isNullOrEmptyString(arg.epoPkgName)):
        utils.getErrorMessage("arg.epoPkgName is empty")
    String currentDir = pwd()
    String dirToPkg = arg.dirToPkg ?: pwd()
    String pkgCatalogXml = arg.pkgCatalogXml ?: "PkgCatalog.xml"
    String pkgName = arg.epoPkgName
    String licenseType = arg.licenseType ?: "LICENSED"
    String catalogAbsPath  = utils.formatPathForOS("${dirToPkg}\\${pkgCatalogXml}")
    if(fileExists(catalogAbsPath)) {
        try {
            signing.epoSign46([
                signType     : "autopackage",
                fileToEncrypt: ".mcs",
                xmlName      : "${catalogAbsPath}",
                licenseType  : "${licenseType}"
            ])
        } catch (Throwable e) {
            log.error("Could not epoSign:${catalogAbsPath}")
            rethrow(e)
        }
        try {
            dir("${dirToPkg}") {
                ziputils.createZip(
                    [
                        fileName: "${pkgName}",
                        input   : "."
                    ]
                )
            }
        } catch (Throwable e) {
            log.error("Could Not Create EPO package zip ${pkgName} from ${dirToPkg}")
            rethrow(e)
        }
    } else {
        error "File '${catalogAbsPath}' does not exist. Cannot create epoPackage"
    }
}