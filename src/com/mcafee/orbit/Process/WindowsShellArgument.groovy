package com.mcafee.orbit.Process

import groovy.transform.InheritConstructors

/**
 * Class for handling Windows named shell arguments
 */
@InheritConstructors
final class WindowsShellArgument extends ShellArgument {
    /**
     * Returns the string representation of the named argument
     *
     * @return The string representation of the named argument
     */
    @Override
    String toString() {
        '--' + name + '="%P' + index + '%"'
    }
}