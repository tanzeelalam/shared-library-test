import org.jenkinsci.plugins.workflow.cps.CpsScript

/**
 * A collection of read-only configuration values.
 *
 * This file is defined as a class to correctly override
 * the setProperty method. This allows us to stop users
 * from changing the values defined by the getter methods.
 */
final class global extends CpsScript {
    @Override
    public final void setProperty(String property, Object _) {
        log.warn("The property global.$property is read-only and cannot be updated.")
    }

    public final def getBuildSystemVersion() { '2.11.0' }
    public final def getORBIT_DEBUG() { '4' }
    public final def getORBIT_INFO() { '3' }
    public final def getORBIT_WARN() { '2' }
    public final def getORBIT_ERROR() { '1' }
    public final def getORBIT_FATAL() { '0' }
    public final def getHTTPS_PROXY() { 'proxy.ess.gslb.entsec.com' }
    public final def getHTTPS_PROXY_PORT() { '9090' }

    public final def getPermittedDriveLetters() { ['N', 'O', 'P', 'Q', 'R', 'S', 'T'] }
    public final def getScratchPadDriveLetters() { ['U', 'V', 'W', 'X', 'Y', 'Z'] }
    public static final def driveLettersBeingUsed = [:]

    public final def getOVF_VCENTER() { config("OVF_VCENTER") }
    public final def getORBIT_AUTOMATION_EMAIL() { config("ORBIT_AUTOMATION_EMAIL") }
    public final def getORBIT_CORE_TEAM_EMAIL() { config("ORBIT_CORE_TEAM_EMAIL") }
    public final def getGITHUB_INSTANCES() { config('GITHUB_INSTANCES') }

    public final def getPath() {
        [
            "CAB_ARC_ON_FILES": "E:\\tools\\IExpress\\CABARC.exe",
            "DEFAULT_JARSIGNER_PATH": 'E:\\Tools\\j2sdk_160_00\\bin\\Jarsigner.Exe',
            "GNU_GREP": "E:\\Tools\\GNU\\egrep",
            "MSWDK_6001": 'C:\\Tools\\wdk_6001\\',
            "MSWDK_7600": 'C:\\Tools\\wdk_7600\\',
            "UNIX_AV_SCAN_DAT_DIR": '/tmp/DAT',
            "UNIX_AV_SCAN": '/opt/apps/uvscan',
            "UNIX_BULLSEYE_BIN_DIR": "/opt/apps/BullseyeCoverage/current/bin/",
            "UNIX_GRADLE_PATH": '/opt/apps/gradle/gradle-4.5.1/bin/gradle',
            "UNIX_JDK_1_8_HOME": '/usr/java/jre1.8.0_131',
            "UNIX_MASIGN": '/opt/apps/masign/masign',
            "VS_BASE_DIR": 'C:\\Program Files\\Microsoft Visual Studio',
            "WIN_ANT_194_DIR": 'C:\\Tools\\ANT_194\\bin\\ant',
            "WIN_AV_SCAN_DAT_DIR": 'C:\\Tools\\Scan\\DAT',
            "WIN_AV_SCAN_DIR": 'C:\\Tools\\Scan',
            "WIN_AV_SCAN_EXE": 'C:\\Tools\\Scan\\csscan.exe',
            "WIN_BULLSEYECOVERAGE_BIN_DIR": 'C:\\Tools\\BullseyeCoverage\\bin',
            "WIN_CURL_EXE": 'C:\\Tools\\curl\\curl.exe',
            "WIN_DEVENV_EXE": '\\Common7\\IDE\\devenv.exe',
            "WIN_EPO46_EPOSIGNSE_DIR": 'E:\\Tools\\eposign46',
            "WIN_EPO46_EPOSIGNSE_EXE": 'E:\\Tools\\eposign46\\eposignse.exe',
            "WIN_GETCLEAN_EXE": 'E:\\tools\\\\GetClean\\getclean.exe',
            "WIN_GIT_BIN_DIR": 'C:\\Tools\\Git\\bin',
            "WIN_GIT_EXE": 'C:\\Tools\\Git\\bin\\git.exe',
            "WIN_GRADLE_PATH": 'E:\\Tools\\gradle-4.5\\bin\\gradle',
            "WIN_GROOVYDOC_EXE": 'C:\\Tools\\Groovy\\Groovy-2.4.7\\bin\\groovydoc',
            "WIN_JDK_1_8_HOME": 'C:\\Tools\\jdk1.8.0_77-x64',
            "WIN_MASIGN_EXE": 'E:\\Tools\\Mar_depends\\masdk\\x64\\masign\\masign.exe',
            "WIN_PERL_EXE": 'C:\\Tools\\StrawberryPerl\\perl\\bin\\perl.exe',
            "WIN_PLINK_EXE": 'C:\\Tools\\Putty\\plink.exe',
            "WIN_PSCP_EXE": 'C:\\Tools\\Putty\\pscp.exe',
            "WIN_PYTHON_EXE": 'C:\\Tools\\Python27\\python.exe',
            "WIN_PYTHON_EXE": 'C:\\Tools\\Python27\\python.exe',
            "WIN_SYMBOL_STORE_EXE": 'E:\\tools\\symstore\\symstore.exe',
            "WIN_VERISIGN_BIN_DIR": 'E:\\Tools\\win10sdk\\bin\\x86',
            "WIN_VS_BASE_DIR": 'C:\\Program Files (x86)\\Microsoft Visual Studio',
            "WIN_VS10_DIR": 'C:\\Program Files (x86)\\Microsoft Visual Studio 10.0',
            "WIN_VS11_DIR": 'C:\\Program Files (x86)\\Microsoft Visual Studio 11.0',
            "WIN_VS12_DIR": 'C:\\Program Files (x86)\\Microsoft Visual Studio 12.0',
            "WIN_VS14_DIR": 'C:\\Program Files (x86)\\Microsoft Visual Studio 14.0',
            "WIN_VS9_DIR": 'C:\\Program Files (x86)\\Microsoft Visual Studio 9.0',
            "WIN_WIX_3_10_CANDLE_EXE": 'C:\\Tools\\WiX3.10\\candle.exe',
            "WIN_WIX_3_10_DIR": 'C:\\Tools\\WiX3.10',
            "WIN_WIX_3_10_LIGHT_EXE": 'C:\\Tools\\WiX3.10\\light.exe',
        ]
    }

    public final def getRadarPostURLBase() { env.RADAR_API }
    public final def getRadarCMDBBaseURL() { env.RADAR_API }
    public final def getRadarUrl() {
        [
            'AddBuildLog': 'Build/BuildLog',
            'AddBuildRecordData': 'Build/BuildRecord',
            'AddBuildResult': 'Build/BuildResult',
            'AddBuildSigning': 'buildsigning/create',
            'AddBuildStepData': 'Build/BuildStep',
            'AddProtexResult': 'Protex/ProtexResults',
            'AddSymbolTransaction': 'bom/SymbolTransaction',
            'AddShiftLeftScanDetails' : 'qwietai/scan',
            'AntiVirusDatVer': 'bom/AntiVirusDatVer',
            'ArtifactoryDependency': 'bom/ArtifactoryDependency',
            'Artifacts': 'artifacts/artifacts',
            'AVScanStatus': 'Build/AVScanStatus',
            'BuildRecord': 'Build/BuildRecord',
            'BuildSearch': 'Build/BuildSearch',
            'BuildSearchByType': 'Build/BuildSearchByType',
            'CodeCoverageResults': 'CodeCoverage/CodeCoverageResults',
            'CodeExport': 'bom/CodeExport',
            'EcmDependency': 'bom/EcmDependency',
            'ECMMockedRecordID': 'bom/EcmMockMasterRecord',
            'GetArtifactoryInstanceByName': 'ArtifactoryInstance/getByName',
            'GetBOMRecord': 'bom/Bom',
            'GetBuildByGuid': 'Build/GetBuildByGuid',
            'GetBuildRecord': 'Build/BuildRecord',
            'GetGithubInstances': 'GithubInstance/getAll',
            'GetJobId': 'BuildSystemJob/getBuildSystemJobIdByName',
            'GetProductByShortName': 'Product/getProductByShortName',
            'GetSignType': 'Signing/Type',
            'GetSigningCertificate': 'Signing/Certificate',
            'GetSigningService': 'Signing/Service',
            'GetSigningConfiguration': 'Signing/Config',
            'InitBOM': 'bom/Bom',
            'LockBOM': 'bom/LockBom',
            'nodeRecord': 'Build/Node',
            'OrbitDependency': 'bom/OrbitDependency',
            'ProxySettings': 'Proxy/Settings',
            'SetArtifactoryUrl': 'Build/ArtifactoryUrl',
            'triggerOrbitBuild': 'BuildSystemJob/trigger',
            'UpdateBuildMilestone': 'Build/BuildMilestone',
            'SetNote': 'build/note',
        ].collectEntries { [it.key, radarPostURLBase + '/' + it.value] }
    }

    /**
     * Required to extends the base script.
     */
    @Override
    def run() { }

    /**
     * Required for backwards compatibility.
     */
    public def call() {}
}
