import java.util.concurrent.TimeUnit

/**
 * Executes a command on a flyweight executor of the master node
 *
 * @param command The command to execute
 * @param environment A map of environment variables to add to the existing environment
 * @param workingDirectory The working directory where to run the command. Defaults to '/'
 * @param stdin Any data to write to STDIN
 * @return Map containing the exit code, stdout and stderr
 */
@NonCPS
private Map execute_command(List<String> command, Map environment = [:], File workingDirectory = null, String stdin = null) {
    Process proc = create_process(command, environment, workingDirectory)
    if (stdin != null) {
        OutputStream stdin_buffer = proc.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin_buffer));
        writer.write(stdin);
        writer.flush();
        writer.close();
    }
    StringBuilder out = new StringBuilder()
    StringBuilder err = new StringBuilder()
    proc.consumeProcessOutput(out, err)
    proc.waitFor(5, TimeUnit.MINUTES)
    return ["exitCode": proc.exitValue(), "stdout": out, "stderr": err]
}

/**
 * Creates a Process for a command to run on the master node
 *
 * @param command The command to execute
 * @param environment A map of environment variables to add to the existing environment
 * @param workingDirectory The working directory where to run the command. Defaults to '/'
 * @return The process
 */
@NonCPS
private Process create_process(List<String> command, Map environment = [:], File workingDirectory = null) {
    ProcessBuilder pb = new ProcessBuilder(command)
    if (workingDirectory != null) {
        pb.directory(workingDirectory)
    }
    Map<String, String> procEnv = pb.environment()
    for (pair in environment) {
        procEnv.put(pair.key, pair.value ?: "")
    }
    return pb.start()
}
