package com.mcafee.orbit

import groovy.transform.TupleConstructor

/**
 * User configuration object.
 * Can be passed to the constructor of {@link com.mcafee.orbit.Build}.
 */
@TupleConstructor
final class Configuration {
    /**
     * The poorly named component version
     */
    String BUILD_VERSION
    /**
     * Whether to disable Bulleye coverage
     */
    boolean DISABLE_BULLSEYE = false
}