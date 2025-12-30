/**
 * The <code>delivery</code> stage.
 *
 * <p>
 *     This stage is used to upload all artifacts to Artifactory.
 * </p>
 * See https://confluence.trellix.com/display/PDS/Orbit+Build+Stages
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * delivery {
 *     // delivery commands...
 * }
 * </pre>
 * @param timeLimit [T:Map] [OPTIONAL] The time limit to apply to the execution of closure.
 *                  Defaults to <code>[time: 5, unit: 'HOURS']</code>.
 *                  See: https://jenkins.io/doc/pipeline/steps/workflow-basic-steps/#timeout-enforce-time-limit
 */
def call(Map timeLimit = [time: 5, unit: 'HOURS'], Closure body) {
    stage('Delivery', timeLimit, body)
}
