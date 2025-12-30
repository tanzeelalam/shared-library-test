/**
 * The <code>packaging</code> stage.
 *
 * <p>
 *     This is the packaging stage. It can be used to copy and zip necessary files into a consumable package
 *     which will be marked for upload to Artifactory.
 * </p>
 * See https://confluence.trellix.com/display/PDS/Orbit+Build+Stages
 *
 * @param timeLimit [T:Map] [OPTIONAL] The time limit to apply to the execution of closure.
 *                  Defaults to <code>[time: 5, unit: 'HOURS']</code>.
 *                  See: https://jenkins.io/doc/pipeline/steps/workflow-basic-steps/#timeout-enforce-time-limit
 */
def call(Map timeLimit = [time: 5, unit: 'HOURS'], Closure body) {
    stage('Package', timeLimit, body)
}
