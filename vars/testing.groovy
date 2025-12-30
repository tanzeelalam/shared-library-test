import com.mcafee.orbit.Deprecated

/**
 * Overload for <code>testing</code> stage.
 */
@Deprecated(["testing(String&comma;&nbsp;boolean&comma;&nbsp;Map&comma;&nbsp;Closure)"])
void call(boolean failBuild, Closure body) {
    deprecated()
    stage('Testing', [time: 5, unit: 'HOURS'], failBuild ? 'ON_ERROR_ABORT' : 'ON_ERROR_CONTINUE', body)
}
/**
 * Overload for <code>testing</code> stage.
 */
@Deprecated(["testing(String&comma;&nbsp;boolean&comma;&nbsp;Map&comma;&nbsp;Closure)"])
void call(Map timeLimit, Closure body) {
    deprecated()
    stage('Testing', timeLimit, 'ON_ERROR_ABORT', body)
}
/**
 * Overload for <code>testing</code> stage.
 */
@Deprecated(["testing(String&comma;&nbsp;boolean&comma;&nbsp;Map&comma;&nbsp;Closure)"])
void call(boolean failBuild, Map timeLimit, Closure body) {
    deprecated()
    stage('Testing', timeLimit, failBuild ? 'ON_ERROR_ABORT' : 'ON_ERROR_CONTINUE', body)
}
/**
 * Overload for <code>testing</code> stage.
 */
@Deprecated(["testing(String&comma;&nbsp;boolean&comma;&nbsp;Map&comma;&nbsp;Closure)"])
void call(String label, boolean failBuild, Closure body, Map timeLimit) {
    deprecated()
    stage(label, timeLimit, failBuild ? 'ON_ERROR_ABORT' : 'ON_ERROR_CONTINUE', body)
}

/**
 * The <code>testing</code> stage.
 * <p>See https://confluence.trellix.com/display/PDS/Orbit+Build+Stages</p>
 * <h4>Sample usage:</h4>
 * <pre>
 * testing {
 *     // run some tests
 * }
 * </pre>
 *
 * <p><b>With all optional parameters:</b></p>
 * <pre>
 * testing('Smoke tests', false, [time: 20, unit: 'MINUTES']) {
 *     // run some tests
 * }
 * </pre>
 *
 * @param label [T:String] The label to be used for the stage.
 * @param failBuild [T:boolean] [OPTIONAL] Set to true to failBuild the build if an exception is thrown within the stage,
 *                  true to <code>ON_ERROR_CONTINUE</code> if an exception is thrown. [DEFAULT:true]
 * @param timeLimit [T:Map] [OPTIONAL] The time limit to apply to the execution of closure.
 *                  Defaults to <code>[time: 5, unit: 'HOURS']</code>.
 *                  See: https://jenkins.io/doc/pipeline/steps/workflow-basic-steps/#timeout-enforce-time-limit
 * @param body [T:Closure] The closure to execute.
 */
void call(String label = 'Testing', boolean failBuild = true, Map timeLimit = [time: 5, unit: 'HOURS'], Closure body) {
    stage(label, timeLimit, failBuild ? 'ON_ERROR_ABORT' : 'ON_ERROR_CONTINUE', body)
}
