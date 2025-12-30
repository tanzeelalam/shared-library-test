import org.w3c.dom.*
import javax.xml.parsers.*
import javax.xml.transform.*
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Generates a SWID file and uploads it to artifactory.
 *
 * <p>See https://confluence.trellix.com/display/PDS/SWID+Tag</p>
 * <p>The file name can be provided in the Jenkinsfile by setting <code>env.SWIDFILE</code>.</p>
 * <p>The file location can be provided in the Jenkinsfile by setting <code>env.SWIDLOCATION</code>.</p>
 * <p>The name can be provided in the Jenkinsfile by setting <code>env.SWIDPRODUCTID</code>.</p>
 *
 * <h4>Sample usage:</h4>
 * With all defaults:
 * <pre>
 * swid()
 * </pre>
 *
 * With all available over-rides:
 * <pre>
 * env.SWIDLOCATION = 'Packaging'
 * env.SWIDFILE = 'com.trellix_MyProduct.swidtag'
 * env.SWIDPRODUCTID = 'Orbit_Test_Project'
 * env.SWIDLANGUAGE = 'en'
 * swid()
 * </pre>
 */
void call() {
    log.info("Generating SWID file.")
    def regId = config('SWIDTAG_REGID')
    def params = utils.splitJenkinsJobName(env.JOB_NAME)
    def productId = params.component
    if (env.SWIDPRODUCTID) {
        productId = env.SWIDPRODUCTID
    }
    def filename = regId + '_' + productId + '.swidtag'
    if (env.SWIDFILE) {
        filename = env.SWIDFILE
    }
    def language = 'en'
    if (env.SWIDLANGUAGE) {
        language = env.SWIDLANGUAGE
    }
    dir(env.SWIDLOCATION ?: env.WORKSPACE) {
        def options = [
            creator: config('SWIDTAG_SW_CREATOR'),
            language: language,
            productId: productId,
            productVersion: "${env.BUILDVERSION}.${orbit.buildNumber()}",
            regId: regId,
        ]
        log.debug(options)
        writeFile(
            file: filename,
            text: generate(options),
            encoding: 'UTF-8',
        )
        artifacts.upload(files: filename)
    }
    log.info("SWID file generated and uploaded to artifactory.")
}

/**
 * Generates the content for the SWID XML file.
 *
 * @param options The data for the XML file.
 */
@NonCPS
private def generate(def options) {
    def dbf = DocumentBuilderFactory.newInstance()
    def builder = dbf.newDocumentBuilder()
    def doc = builder.newDocument()
    def element = doc.createElement('SoftwareIdentity')
    element.setAttribute('xmlns', 'https://standards.iso.org/iso/19770/-2/2015/schema.xsd')
    element.setAttribute('xml:lang', options.language)
    element.setAttribute('name', options.productId)
    element.setAttribute('tagid', "${options.regId}_${options.productId}")
    element.setAttribute('version', options.productVersion)
    element.setAttribute('corpus', 'false')
    element.setAttribute('patch', 'false')
    element.setAttribute('supplemental', 'false')
    doc.appendChild(element)
    def entityElement = doc.createElement('Entity')
    element.appendChild(entityElement)
    entityElement.setAttribute('name', options.creator)
    entityElement.setAttribute('regid', options.regId)
    entityElement.setAttribute('role', options.regId)
    def tf = TransformerFactory.newInstance().newTransformer()
    tf.setOutputProperty(OutputKeys.ENCODING, 'UTF-8')
    tf.setOutputProperty(OutputKeys.INDENT, 'yes')
    tf.setOutputProperty('{http://xml.apache.org/xslt}indent-amount', '4')
    def writer = new StringWriter()
    tf.transform(new DOMSource(doc), new StreamResult(writer))
    return writer.toString()
}
