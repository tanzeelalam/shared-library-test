/**
 * The <code>prepackage</code> stage.
 *
 * <p>
 *     This stage can be used for tasks prior to packaging (post compilation) e.g. signing,
 *     or analysis tasks, file restructuring etc.
 * </p>
 * See https://confluence.trellix.com/display/PDS/Orbit+Build+Stages
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * prepackage {
 *     // pre-packaging steps...
 * }
 * </pre>
 *
 * @param timeLimit [T:Map] [OPTIONAL] The time limit to apply to the execution of closure.
 *                  Defaults to <code>[time: 5, unit: 'HOURS']</code>.
 *                  See: https://jenkins.io/doc/pipeline/steps/workflow-basic-steps/#timeout-enforce-time-limit
 */
def call(Map timeLimit = [time: 5, unit: 'HOURS'], Closure body) {
    stage('Prepackage', timeLimit, body)
}
