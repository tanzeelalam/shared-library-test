/**
 * Initialize BOM
 */
private def initBOM() {
    global()
    log.info('Initializing BOM for the build.')
    return request.post(
        url: global.radarUrl.InitBOM,
        error: 'Failed to initialize BOM in Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordId: orbit.buildRecordId(),
            BomType: "BuildSystem Environment",
            BuildSystem: "Orbit",
            BuildSystemVersion: global.buildSystemVersion,
        ],
    )
}

/**
 * Creates a record of any code export to create a BOM in the Radar
 * for reproducibility, tracibility and general analysis purposes.
 * See Radar CMDB API for more information.
 *
 * <p><b>Sample Usage </p></b>
 * <pre>
 * bom.scmBOM(
 *     scmType: "GIT",
 *     scmVersion: "1.8",
 *     scmCommit: "REVISION",
 *     scmBranch: "main",
 *     scmCredential: "CREDS_USED",
 *     scmUrl: "https://github.trellix.com/trellix-products/your-product.git",
 *     lfsRepository: "some-repo",
 * )
 * </pre>
 * @param scmType Source Code Tool e.g.: "GIT"
 * @param scmCommit The hash of the checked out commit
 * @param scmCredential The id of the credential that was used
 * @param lfsRepository The name of the LFS repository
 * @param scmUrl The url for the scm repository
 */
private def scmBOM(Map arg) {
    global()
    def payload = [
        BuildRecordID: orbit.buildRecordId(),
        BomType: arg.bomType,
        ScmType: arg.scmType,
        ScmVersion: arg.scmVersion,
        ScmCommit: arg.scmCommit,
        ScmBranch: arg.scmBranch,
        Credential: arg.scmCredential,
        ScmUrl: arg.scmUrl,
        LfsRepository: null,
        LfsArtifactoryInstance: null,
    ]
    if (arg.lfsRepository) {
        payload.LfsRepository = arg.lfsRepository
        payload.LfsArtifactoryInstance = env.ARTIFACTORY_INSTANCE
    }
    return request.post(
        url: global.radarUrl.CodeExport,
        error: 'Failed to record SCM dependency in Radar.',
        token: 'orbit_radar_cred',
        json: payload,
    )
}

/**
 * Creates a record of Orbit dependency for the BOM in the radar.CMBD to be used for reproducibility, tracibility and general analysis purposes
 * See Radar CMDB API for more information.
 * <p><b>Sample Usage </p></b>
 * <pre>
 * bom.eCMBOM(
 *     ecmMasterId: "600000",
 *     ecmProjectName: "secretcmproj",
 *     ecmBuildNumber: "1000",
 *     ecmPackageNumber: "1",
 *     ecmVersion: "1.0.0"
 * )
 * </pre>
 *
 * @param ecmMasterId eCM Master ID
 * @param ecmProjectName eCM Project name
 * @param ecmBuildNumber eCM Build number
 * @param ecmPackageNumber (optional) eCM Package number
 * @param ecmVersion eCM build or package Version
 */
private def eCMBOM(Map arg) {
    global()
    if (!arg.ecmPackageNumber) {
        arg.ecmPackageNumber = "0"
    }
    return request.post(
        url: global.radarUrl.EcmDependency,
        error: 'Failed to record eCM dependency in Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordID: orbit.buildRecordId(),
            BomType: "SCM",
            EcmMasterId: arg.ecmMasterId,
            EcmProjectName: arg.ecmProjectName,
            EcmBuildNumber: arg.ecmBuildNumber,
            EcmPackageNumber: arg.ecmPackageNumber,
            EcmVersion: arg.ecmVersion,
        ],
    )
}

/**
 * Creates a record of Orbit dependency for the BOM in the radar.CMBD to be used for reproducibility, tracibility and general analysis purposes
 * See Radar CMDB API for more information.
 * <p><b> Sample usage:</b></p>
 * <pre>
 * bom.orbitBOM(
 *     dependencyBuildRecordId: 12345,
 * )
 * </pre>
 * @param dependencyBuildRecordId Source Code Tool e.g.: "GIT"
 */
private def orbitBOM(Map arg) {
    global()
    return request.post(
        url: global.radarUrl.OrbitDependency,
        error: 'Failed to record Orbit dependency in Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordID: orbit.buildRecordId(),
            BomType: "Orbit",
            DependencyBuildRecordId: arg.dependencyBuildRecordId,
        ],
    )
}

/**
 * AV Dat Version BOM update.
 *
 * Creates a record of AV Dat version used to scan the build in the radar's CMBD.
 * See Radar CMDB API for more information.
 * <h4>Sample usage:</h4>
 * <pre>
 * bom.avDatVersion(
 *     avDatver: "8509",
 * )
 * </pre>
 * @param avDatVer The Antivirus Dat version
 */
private def avDatVersion(Map arg) {
    global()
    return request.post(
        url: global.radarUrl.AntiVirusDatVer,
        error: 'Failed to record antivirus version in Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordID: orbit.buildRecordId(),
            AntiVirusDatVer: arg.avDatVer,
        ],
    )
}

/**
 * Record the mocked eCM id.
 *
 * Create a record of eCM 'masterID' for the corrosponding mocked eCM build.
 * The Radar backend API uses this record ID to keep the build milestones in Sync
 * between Radar and eCM. 
 * <h4>Sample usage:</h4>
 * <pre>
 * bom.ecmMockedRecordID(
 *     ecmMockMasterRecordId: "658958",
 * )
 * </pre>
 * @param ecmMasterBuildID The eCM MasterID for the corrosponding mocked eCM Build
 */
private def ecmMockedRecordID(Map arg) {
    global()
    return request.post(
        url: global.radarUrl.ECMMockedRecordID,
        error: 'Failed to mock eCM dependency in Radar.',
        token: 'orbit_radar_cred',
        json: [
            BuildRecordID: orbit.buildRecordId(),
            EcmMockMasterRecordId: arg.ecmMockMasterRecordId,
        ],
    )
}

/**
 * Locks the BOM for the current build
 * <h4>Sample usage:</h4>
 * <pre>
 *    bom.lockBOM()
 * </pre>
 */
private def lockBOM() {
    global()
    return request.post(
        url: global.radarUrl.LockBOM,
        error: 'Failed to lock the BOM in Radar.',
        token: 'orbit_radar_cred',
        json: [ BuildRecordID: orbit.buildRecordId() ],
    )
}
