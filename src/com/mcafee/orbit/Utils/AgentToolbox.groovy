package com.mcafee.orbit.Utils

import com.mcafee.orbit.Configuration.BuildConfiguration
import com.mcafee.orbit.Log.Logger
import com.mcafee.orbit.Pipeline.Context
import com.mcafee.orbit.Pipeline.Environment

/**
 * A helper class for loading the orbit-agent-toolbox
 */
final class AgentToolbox {
    private String workspace
    private final BuildConfiguration config
    private final Environment env
    private final Context context
    private final Logger log
    /**
     * Default constructor
     *
     * @param config A {@link BuildConfiguration} object
     * @param context A {@link Context} object
     * @param log A {@link Logger} object
     */
    AgentToolbox(BuildConfiguration config, Environment env, Context context, Logger log) {
        this.config = config
        this.env = env
        this.context = context
        this.log = log
    }
    /**
     * Unpacks the orbit-agent-toolbox wrapper into the workspace
     */
    private void unpack() {
        if (env.WORKSPACE == null) {
            throw new IllegalStateException(
                'Cannot unpack orbit-agent-toolbox. WORKSPACE is NULL.'
            )
        }
        if (workspace != env.WORKSPACE) {
            workspace = env.WORKSPACE
            context.writeFile(
                file: script,
                text: context.libraryResource(
                    'org/mcafee/toolbox/orbit.groovy'
                )
            )
        }
    }
    /**
     * Returns the path to the wrapper script
     *
     * @return The path to the wrapper script
     */
    private String getScript() {
        "$workspace/orbit.groovy"
    }
    /**
     * Returns an orbit-agent-toolbox command
     *
     * @param subcommand The subcommand to execute
     * @return A full orbit-agent-toolbox command, including a subcommand
     */
    String getCommand(String subcommand) {
        unpack()
        "groovy $script $subcommand"
    }
}