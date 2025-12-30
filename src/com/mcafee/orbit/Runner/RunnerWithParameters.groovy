package com.mcafee.orbit.Runner

import com.mcafee.orbit.Build.BuildInternals
import com.mcafee.orbit.Parameters.Parameters

/**
 * Generic runner with parameters.
 * Extend it and implement the call(T) method.
 *
 * @param <T> A class that extends Parameters
 */
abstract class RunnerWithParameters<T extends Parameters> {
    /**
     * BuildInternals
     */
    protected final BuildInternals build
    /**
     * A class loader for loading instances of the {@link Parameters},
     * which are specific to the implementation
     */
    protected final static GroovyClassLoader loader = new GroovyClassLoader()
    /**
     * Default constructor
     *
     * @param build The build internals
     */
    RunnerWithParameters(BuildInternals build) {
        this.build = build
    }
    /**
     * Implement this when extending to provide your functionality
     *
     * @param args T
     * @return Mixed, depends on the implementation of call(T)
     */
    protected abstract def call(T args)
    /**
     * Exposed run method
     *
     * @param args The arguments to pass to call(T)
     * @return Mixed, depends on the implementation of call(T)
     */
    def run(T args) {
        args.validate()
        call(args)
    }
    /**
     * Exposed run method
     *
     * @param args A Map that will be converted to T and passed to call(T)
     * @return Mixed, depends on the implementation of call(T)
     */
    def run(Map map) {
        T args = getParametersInstance()
        args.fromMap(map)
        run(args)
    }
    /**
     * Exposed run method
     *
     * @param args A Closure that will be converted to T and passed to call(T)
     * @return Mixed, depends on the implementation of call(T)
     */
    def run(Closure closure) {
        T args = getParametersInstance()
        args.fromClosure(closure)
        run(args)
    }
    /**
     * Returns a new instance T, the specific parameters used in the subclass
     * @return A new instance of T
     */
    protected T getParametersInstance() {
        // This is ugly and requires the Parameters class to have
        // the name of the runner class followed by 'Parameters'
        // but there is no way to get the class name in a static context
        // within the Parameters class.
        (T)loader.loadClass(this.class.name + 'Parameters').newInstance()
    }
}