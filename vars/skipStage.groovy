import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

/**
 * Creates a dummy stage in Jenkins and sets it as skipped in the Jenkins UI & in Radar.
 *
 * <p>If already in a stage or the supplied stage name is not approved, it will gracefully warn.</p>
 * <p>Allowed stages (case sensitive) to be skipped are:
 * <ul>
 *     <li><code>Compile</code></li>
 *     <li><code>Prepackage</code></li>
 *     <li><code>Package</code></li>
 *     <li><code>Predelivery</code></li>
 *     <li><code>Delivery</code></li>
 * </ul>
 *
 * <h4>Sample usage</h4>
 * <pre>
 * // Skip compile stage if the JOB TYPE == 'Package'
 * if (env.ORBIT_JOB_TYPE == "Package"){
 *     skipStage("Compile")
 * } else {
 *     compile {
 *         //Build normally
 *     }
 * }
 * </pre>
 * <pre>
 * // Skip compile stage if the JOB TYPE == 'Package'
 * skipStage("Compile", (env.ORBIT_JOB_TYPE == "Package")){
 *     // What would Build normally if env.ORBIT_JOB_TYPE were not Package
 * }
 * </pre>
 * @param stageName [T:String] Name of the stage you wish to skip
 * @param skipIt [T:boolean] [OPTIONAL] Can be used in place of if/else logic block for conditional stage
 * execution. [DEFAULT:true]
 * @param body [T:Closure] [OPTIONAL] If provided, and <code>skipIt</code> is <code>false</code>, the body will execute in the proper stage.
 */
public void call(String stageName, boolean skipIt = true, Closure body = {}) {
    //env.STAGE_NAME : https://issues.jenkins-ci.org/browse/JENKINS-44456
    assert utils.isNullOrEmptyString(env.STAGE_NAME) : "Error: skipStage cannot be called within an existing stage."
    switch(stageName.toLowerCase()){
        case 'prebuild':
            if(skipIt) {
                mockStage("Prebuild")
            } else {
                prebuild(body)
            }
            break
        case ['compile', 'build']:
            if(skipIt) {
                mockStage("Build")
            } else {
                compile(body)
            }
            break
        case 'prepackage':
            if(skipIt) {
                mockStage("Prepackage")
            } else {
                prepackage(body)
            }
            break
        case ['package', 'packaging']:
            if(skipIt) {
                mockStage("Package")
            } else {
                packaging(body)
            }
            break
        case 'predelivery':
            if(skipIt) {
                mockStage("Predelivery")
            } else {
                predelivery(body)
            }
            break
        case 'delivery':
            if(skipIt) {
                mockStage("Delivery")
            } else {
                delivery(body)
            }
            break
        default:
            error "Incorrect usage, reference: https://docs.orbit.corp.entsec.com/public/orbit/staging/vars/skipStage.html#call(def)"
            break
    }
}

/**
 * Optional overloading
 */
public void call(String stageName, Closure body) { this.call(stageName, true, body) }

/**
 * Closure declarations in the call were causing No such DSL method 'mockStage' found among steps.
 */
private Closure mockStage(String name) {
    stage(name){
        log.info("Skipped stage '$name'")
        Utils.markStageSkippedForConditional(name)
    }
}
