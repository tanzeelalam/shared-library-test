package com.mcafee.orbit.Utils

import com.cloudbees.groovy.cps.NonCPS

/**
 * String utilities
 */
class StringUtils {
    /**
     * Checks whether an object is null or empty
     *
     * @param object The object to check
     * @return True if the object is null or empty, false otherwise
     */
    @NonCPS
    static boolean isNullOrEmpty(Object object) {
        if (object == null) {
            return true
        }
        try {
            String string = object as String
            return string.trim().size() == 0
        } catch (GroovyCastException) {
            return false
        }
    }
    /**
     * Parses a value to a nullable integer.
     *
     * @param value The object to parse
     * @return An integer value or null if the value cannot be parsed
     */
    @NonCPS
    static def nullableInteger(def value) {
        try {
            return Integer.parseInt(value)
        } catch (NumberFormatException) {
            return null
        }
    }
    /**
     * Parses a value to an integer.
     *
     * @param value The object to parse
     * @param defaultValue The default value to use if the object can't be parsed
     * @return An integer value or null if the value cannot be parsed
     */
    @NonCPS
    static def asInteger(def value, int defaultValue = 0) {
        return value?.toString()?.isInteger() ? value.toString().toInteger() : defaultValue
    }
    /**
     * Returns a random number padded by zeros to 6 characters
     *
     * @return A random number
     */
    @NonCPS
    static String getPaddedRandomNumber() {
        String.format(
            "%06d",
            Math.abs(new Random().nextInt()) % 1000000
        )
    }
    /**
     * Split a command into multiple commands by a flag on a list of values.
     * Used by signing to work around the 8191 character maximum command
     * length of the cmd shell on Windows.
     *
     * The method appends the flag to each value in the provided list and ensures that
     * each resulting command does not exceed the specified length limit.
     *
     * @param list The current list of arguments forming the base of the command.
     * @param flag The flag (e.g., "-f") to append before each value in the `values` list.
     * @param values A list of values to append to the command with the flag. 
     *               If a single value is passed, it is treated as a list with one item.
     * @param limit The maximum allowed output string length for each command chunk.
     * @return A list of lists, where each sublist represents a separate command to execute. 
     *         Each sublist contains the base command and the flag-value pairs, up to the length limit.
     */
    @NonCPS
    static def splitCommandByFlag(def list, def flag, def values, def limit) {
        def commands = []
        def command = list
        if (values instanceof Iterable) {
            for (def value in values) {
                // The number `3` is used because we surround each argument
                // with double quotes and add a space between arguments.
                def commandLength = command.sum { it.toString().length() + 3 }
                // The number `6` is used because we surround each flag and the corresponding value
                // with double quotes and add a space between every flag and argument.
                if (commandLength + flag.toString().length() + value.toString().length() + 6 > limit) {
                    if (command.size() > list.size()) {
                        commands += [command]
                    }
                    command = list + flag + value
                } else {
                    command += [flag, value]
                }
            }
            commands += [command]
        } else {
            commands += [list + [flag, values]]
        }
        return commands
    }
}