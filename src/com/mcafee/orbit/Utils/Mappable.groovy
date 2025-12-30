package com.mcafee.orbit.Utils

import com.cloudbees.groovy.cps.NonCPS

/**
 * When subclasses, add a toMap method that collects all
 * defined class properties into a map and returns it.
 */
abstract class Mappable {
    /**
     * Returns a Map representation of the object
     *
     * @return Map representation of the object
     */
    @NonCPS
    Map toMap() {
        Map map = [:]
        for (int i=0; i<properties.keySet().size(); i++) {
            String key = properties.keySet()[i]
            if (key != 'class') {
                map[key] = properties[key]
            }
        }
        map
    }
}