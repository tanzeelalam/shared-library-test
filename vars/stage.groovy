/**
 * Over-rides custom stages in the Jenkins pipeline to update Radar.
 *
 * <h4>Sample usage:</h4>
 * <pre>
 * stage("my_stage") {
 *     // Run code within the custom stage 'my_stage'
 * }
 * </pre>
 *
 * @param name [T:String]The name of the custom stage
 * @param timeLimit [T:Map] [OPTIONAL] The time limit to apply to the execution of closure.
 *                  Defaults to <code>[time: 5, unit: 'HOURS']</code>.
 *                  See: https://jenkins.io/doc/pipeline/steps/workflow-basic-steps/#timeout-enforce-time-limit
 * @param onError [T:String] [OPTIONAL] Behaviour when an error is caught. The stage will always be set to <code>FAILURE</code>. 
 *                [DEFAULT:"ON_ERROR_ABORT"]
 *                <ul>
 *                  <li><code>"ON_ERROR_ABORT"</code>: Sets build result to <code>FAILURE</code> and aborts execution of further stages.</li>
 *                  <li><code>"ON_ERROR_FAIL"</code>: Sets build result to <code>FAILURE</code> and executes the remaining stages.</li>
 *                  <li><code>"ON_ERROR_CONTINUE"</code>: Sets the build result to <code>SUCCESS</code> and executes the remaining stages.</li>
 *                </ul>
 * @param body [T:Closure] The code to execute for this stage.
 */
void call(
    String name,
    Map timeLimit = [time: 5, unit: 'HOURS'],
    String onError = 'ON_ERROR_ABORT',
    Closure body
) {
    def exception = null
    env.current_stage = name
    timeout(timeLimit) {
        steps.stage(name) {
            catchError(stageResult: 'FAILURE', buildResult: onError == 'ON_ERROR_CONTINUE' ? 'SUCCESS' : 'FAILURE') {
                // Record stage in Radar
                if (orbit.buildRecordId()) {
                    String status = "IN_PROGRESS"
                    try {
                        radar.markBuildStep(
                            StepName: name,
                            StepResult: status,
                        )
                        // Call stage commands
                        log.info("Running $name commands")
                        body.call()
                        status = "SUCCESS"
                    } catch (Throwable e) {
                        status = "FAILED"
                        radar.addBuildLog("Error", "Generic_Stage", name, "None", e.message)
                        exception = e
                        throw e
                    } finally {
                        radar.markBuildStep(
                            StepName: name,
                            StepResult: status,
                        )
                    }
                } else {
                    // Don't record stage in Radar
                    log.info("Running $name commands")
                    body.call()
                }
            }
        }
    }
    if (onError == 'ON_ERROR_ABORT' && exception) {
        throw exception
    }
}

/**
 * Overload with the <code>onError</code> parameter in second position.
 */
void call(
    String name,
    String onError,
    Map timeLimit = [time: 5, unit: 'HOURS'],
    Closure body
) {
    stage(name, timeLimit, onError, body)
}