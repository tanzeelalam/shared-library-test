package com.mcafee.orbit.Parameters

import com.cloudbees.groovy.cps.NonCPS
import com.mcafee.orbit.Utils.Mappable

import java.lang.reflect.Field

/**
 * Base class for Parameters classes.
 * Performs to/from Map and from Closure conversions.
 *
 * When extending this class, any defined fields
 * should use boxed types for primitives and
 * implementations, not interfaces for other types.
 *
 * Subclasses fields that have a value assigned
 * or that are annotated with @AllowEmpty
 * or @Internal are optional.
 *
 * This is good:
 * <pre>
 *     class MyParameters extends Parameters {
 *         Integer myIntField
 *         Boolean myBoolField
 *         ArrayList<String> myListField
 *         LinkedHashMap myMapField
 *     }
 * </pre>
 *
 * This will fail:
 * <pre>
 *     class MyParameters extends Parameters {
 *         int myIntField
 *         boolean myBoolField
 *         List myListField
 *         Map myMapField
 *     }
 * </pre>
 */
abstract class Parameters extends Mappable {
    /**
     * Validates that mandatory parameters are not null
     */
    @NonCPS
    void validate() {
        for (int i=0; i<properties.keySet().size(); i++) {
            String key = properties.keySet()[i]
            if (key != 'class') {
                if (this[key] == null && !isOptional(key)) {
                    throw new IllegalArgumentException(
                        "Mandatory parameter '$key' for class '${this.class.name}' not provided."
                    )
                }
            }
        }
    }
    /**
     * Loads values from a Map object into this Parameters object
     *
     * @param map The Map to load values from
     */
    @NonCPS
    void fromMap(Map map) {
        for (int i = 0; i < properties.keySet().size(); i++) {
            String key = properties.keySet()[i]
            if (key != 'class') {
                if (map == null) {
                    if (this[key] == null && !isOptional(key)) {
                        throw new IllegalArgumentException(
                            "Class '${this.class.name}' has mandatory parameters, but found NULL."
                        )
                    }
                } else {
                    if (map[key] != null) {
                        typeCheck(key, map[key])
                        this[key] = map[key]
                    } else if (this[key] == null && !isOptional(key)) {
                        throw new IllegalArgumentException(
                            "Mandatory parameter '$key' for class '${this.class.name}' not provided."
                        )
                    }
                }
            }
        }
    }
    /**
     * Loads values from a Closure object into this Parameters object
     *
     * @param map The Closure to load values from
     */
    void fromClosure(Closure closure) {
        Map map = [:]
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = map
        closure()
        fromMap(map)
    }
    /**
     * Type checking for conversions from a Map
     *
     * @param fieldName The name of the field to check
     * @param object The object to check
     */
    @NonCPS
    protected void typeCheck(String fieldName, Object object) {
        Field field = this.class.getDeclaredField(fieldName)
        try {
            field.type.cast(object)
            if (field.type == String && !isOptional(fieldName)) {
                String str = object as String
                if (str.trim().size() == 0) {
                    throw new IllegalArgumentException(
                        "Parameter '$fieldName' for class '${this.class.name}' expected to be a non-empty String.",
                    )
                }
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                "Parameter '$fieldName' for class '${this.class.name}' expected to be of type '${field.type.name}'.",
                e
            )
        }
    }
    /**
     * Checks if a field is annotated as optional
     *
     * @param fieldName The name of the field to check
     * @return True if the field is optional, false otherwise
     */
    @NonCPS
    protected boolean isOptional(String fieldName) {
        Field field = this.class.getDeclaredField(fieldName)
        field.getAnnotation(AllowEmpty.class) != null || field.getAnnotation(Internal.class) != null
    }
}