import com.mcafee.orbit.Deprecated

/**
 * The <code>predelivery</code> stage.
 */
@Deprecated(["stage()"])
def call(Map timeLimit = [time: 5, unit: 'HOURS'], Closure body) {
    deprecated()
    stage('Predelivery', timeLimit, body)
}
