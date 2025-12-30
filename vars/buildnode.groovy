import com.mcafee.orbit.Deprecated

@Deprecated(["node()"])
def call(def label, def machineAttributes = [:], def closure) {
    deprecated()
    node(label, machineAttributes, closure)
}
