package com.mcafee.orbit.Process

/**
 * Base class for handling named shell arguments
 */
abstract class ShellArgument {
    protected final int index
    protected final String name
    protected final String value
    /**
     * Default constructor
     *
     * @param index The index of the argument in the argument list
     * @param name The name of the argument
     * @param value The value of the argument
     */
    ShellArgument(int index, String name, String value) {
        this.index = index
        this.name = name
        this.value = value
    }
    /**
     * Returns the environment variable declaration for the argument
     *
     * @return The environment variable declaration for the argument
     */
    String getVar() {
        'P' + index + '=' + value
    }
    /**
     * Value getter
     *
     * @return Argument value
     */
    String getValue() {
        return value
    }
}