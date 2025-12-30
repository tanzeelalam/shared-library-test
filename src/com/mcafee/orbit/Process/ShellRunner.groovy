package com.mcafee.orbit.Process

import com.mcafee.orbit.Log.Logger
import com.mcafee.orbit.Pipeline.Context

/**
 * Runner for shell commands with named arguments.
 *
 * The argument values are loaded into environment
 * variables and references in the command.
 *
 * Example:
 * <pre>
 * new ShellRunner(this)
 * .setDescription('My awesome command') // Printed in case of failure
 * .setCommand('my-awesome-binary') // What to execute
 * .addArgs([ // Actual rgument values
 *   logLevel: 'INFO',
 *   items: ['foo', 'bar']
 * ])
 * .run() // Shell out
 * </pre>
 *
 * On Unix will execute:
 * <pre>
 * $ my-awesome-binary --logLevel=INFO --items=foo --items=bar
 * </pre>
 */
final class ShellRunner {
    private final Context context
    private final Logger log
    private String description = 'Shell command'
    private String command = ''
    private List<ShellArgument> args = []
    private boolean softFail = false
    /**
     * Default constructor
     *
     * @param context The Jenkins pipeline script
     *                that this class runs in the context of
     */
    ShellRunner(Context context, Logger log) {
        this.context = context
        this.log = log
    }

    /**
     * Sets the command description.
     * Printed back if argument validation fails
     *
     * @param description The description of the command
     * @return this, for chaining
     */
    ShellRunner setDescription(String description) {
        this.description = description
        this
    }
    /**
     * Sets the command to run
     *
     * @param command The command to run
     * @return this, for chaining
     */
    ShellRunner setCommand(String command) {
        this.command = command
        this
    }
    /**
     * Sets a flag to avoid failing on non-zero exit code
     *
     * @return this, for chaining
     */
    ShellRunner setSoftFail() {
        this.softFail = true
        this
    }
    /**
     * Add a map of arguments to add to the command.
     * Can contain primitives and lists of primitives.
     *
     * @param config The arguments to add to the command
     * @return this, for chaining
     */
    ShellRunner addArgs(Map config) {
        int index = args.size()
        for (int i=0; i<config.keySet().size(); i++) {
            String key = config.keySet()[i]
            def arg = config[key]
            if (arg in List) {
                for (int j=0; j<arg.size(); j++) {
                    addArg(index, key, arg[j] as String)
                    index++
                }
            } else if (arg != null) {
                addArg(index, key, arg as String)
                index++
            }
        }
        this
    }
    /**
     * Execute the shell command
     */
    boolean run() {
        try {
            context.withEnv(env) {
                if (context.isUnix()) {
                    context.sh runnable
                } else {
                    context.bat runnable
                }
            }
            return true
        } catch (Exception e) {
            if (!softFail) {
                throw e
            } else {
                log.warn(e.message)
            }
            return false
        }
    }
    /**
     * Adds a single named argument to the command to execute
     *
     * @param index The index of the argument in the argument list
     * @param name The name of the argument
     * @param value The value of the argument
     */
    private void addArg(int index, String name, String value) {
        if (context.isUnix()) {
            args.add(new UnixShellArgument(index, name, value))
        } else {
            args.add(new WindowsShellArgument(index, name, value))
        }
    }
    /**
     * Retuns a list of environment variables to use
     * for the context of the executing command
     *
     * @return List of environment variables
     */
    private List<String> getEnv() {
        List<String> env = []
        for (int i=0; i<args.size(); i++) {
            env.add(args[i].var)
        }
        env
    }
    /**
     * Returns the full command with arguments as a String
     *
     * @return The full command with arguments as a String
     */
    private String getRunnable() {
        StringBuilder sb = new StringBuilder(command)
        for (int i=0; i<args.size(); i++) {
            sb.append(' ')
            sb.append(args[i].toString())
        }
        sb.toString()
    }
}