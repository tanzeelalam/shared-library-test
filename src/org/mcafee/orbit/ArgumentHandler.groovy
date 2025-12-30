package org.mcafee.orbit

import com.cloudbees.groovy.cps.SerializableScript
import groovy.transform.InheritConstructors

/**
 * This is a utility for parsing a Map of values.
 * It can be used to sanitize the arguments to a method call.
 * Any parameters that are not explicitely defined are ignored.
 *
 * See also: vars/arguments.groovy
 */
final class ArgumentHandler {
    /**
     * A reference to the WorkflowScript
     */
    private SerializableScript script
    /**
     * The arguments to parse and validate
     */
    private Map arguments
    /**
     * The definitions of the expected parameters
     */
    private List<Parameter> parameters
    /**
     * Default constructor
     *
     * @param script A reference to the WorkflowScript
     * @param arguments The arguments to parse and validate
     */
    ArgumentHandler(SerializableScript script, Map arguments) {
        this.script = script
        this.arguments = arguments
        parameters = new ArrayList<Parameter>()
    }
    /**
     * Adds a parmeter to the definitions of the expected parameters
     *
     * @param parameter The parameter to add
     * @return self, for chaining
     */
    private ArgumentHandler add(Parameter parameter) {
        // TODO: error out if we already have a parameter with same name
        parameters.add(parameter)
        this
    }
    /**
     * Adds a mandatory string parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @return self, for chaining
     */
    ArgumentHandler addString(String name) {
        add(new StringParameter(name))
    }
    /**
     * Adds an optional string parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @param defaultValue The default value
     * @return self, for chaining
     */
    ArgumentHandler addString(String name, String defaultValue) {
        add(new StringParameter(name, defaultValue))
    }
    /**
     * Adds a mandatory boolean parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @return self, for chaining
     */
    ArgumentHandler addBoolean(String name) {
        add(new BooleanParameter(name))
    }
    /**
     * Adds an optional boolean parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @param defaultValue The default value
     * @return self, for chaining
     */
    ArgumentHandler addBoolean(String name, Boolean defaultValue) {
        add(new BooleanParameter(name, defaultValue))
    }
    /**
     * Adds a mandatory integer parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @return self, for chaining
     */
    ArgumentHandler addInteger(String name) {
        add(new IntegerParameter(name))
    }
    /**
     * Adds an optional integer parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @param defaultValue The default value
     * @return self, for chaining
     */
    ArgumentHandler addInteger(String name, Integer defaultValue) {
        add(new IntegerParameter(name, defaultValue))
    }
    /**
     * Adds a mandatory double parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @return self, for chaining
     */
    ArgumentHandler addDouble(String name) {
        add(new DoubleParameter(name))
    }
    /**
     * Adds an optional double parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @param defaultValue The default value
     * @return self, for chaining
     */
    ArgumentHandler addDouble(String name, Double defaultValue) {
        add(new DoubleParameter(name, defaultValue))
    }
    /**
     * Adds a mandatory list parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @return self, for chaining
     */
    ArgumentHandler addList(String name) {
        add(new ListParameter(name))
    }
    /**
     * Adds an optional list parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @param defaultValue The default value
     * @return self, for chaining
     */
    ArgumentHandler addList(String name, List defaultValue) {
        add(new ListParameter(name, defaultValue))
    }
    /**
     * Adds a mandatory map parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @return self, for chaining
     */
    ArgumentHandler addMap(String name) {
        add(new MapParameter(name))
    }
    /**
     * Adds an optional map parameter to the current definitions
     *
     * @param name The name of the expected parameter
     * @param defaultValue The default value
     * @return self, for chaining
     */
    ArgumentHandler addMap(String name, Map defaultValue) {
        add(new MapParameter(name, defaultValue))
    }
    /**
     * Parses the arguments, sets defaults and throws on errors
     */
    void parse() {
        if (arguments == null) {
            script.error('Failed to parse arguments. Expected a Map, but got NULL')
        }
        for (int i=0; i<parameters.size(); i++) {
            Parameter parameter = parameters[i]
            String name = parameter.name
            if (parameter.type == String) {
                arguments.put(name, parseString(arguments.get(name), parameter))
            } else if (parameter.type == Integer) {
                arguments.put(name, parseInteger(arguments.get(name), parameter))
            } else if (parameter.type == Boolean) {
                arguments.put(name, parseBoolean(arguments.get(name), parameter))
            } else if (parameter.type == Double) {
                arguments.put(name, parseDouble(arguments.get(name), parameter))
            } else if (parameter.type == List) {
                arguments.put(name, parseList(arguments.get(name), parameter))
            } else if (parameter.type == Map) {
                arguments.put(name, parseMap(arguments.get(name), parameter))
            } else {
                script.log.error(
                    'Unsupported type encountered while parsing arguments'
                )
                script.error('Failed to parse arguments')
            }
        }
    }
    /**
     * Parses an Object to a String
     * @param value The value to parse
     * @param parameter The parameter definition
     * @return The parsed String
     */
    private String parseString(Object value, Parameter parameter) {
        if (value == null) {
            return (String)defaultValue(parameter)
        }
        try {
            String cast = value as String
            if (cast.trim().size() == 0) {
                return (String)defaultValue(parameter)
            }
            return cast
        } catch (Throwable e) {
            return (String)defaultValue(parameter)
        }
    }
    /**
     * Parses an Object to an Integer
     * @param value The value to parse
     * @param parameter The parameter definition
     * @return The parsed Integer
     */
    private Integer parseInteger(Object value, Parameter parameter) {
        if (value == null) {
            return (Integer)defaultValue(parameter)
        }
        try {
            return value as Integer
        } catch (Throwable e) {
            return (Integer)defaultValue(parameter)
        }
    }
    /**
     * Parses an Object to a Boolean
     * @param value The value to parse
     * @param parameter The parameter definition
     * @return The parsed Boolean
     */
    private Boolean parseBoolean(Object value, Parameter parameter) {
        if (value == null) {
            return (Boolean)defaultValue(parameter)
        }
        if (value in String) {
            String canonical = value.trim().toLowerCase()
            if (canonical == 'true') {
                return true
            } else if (canonical == 'false') {
                return false
            }
            return (Boolean)defaultValue(parameter)
        }
        try {
            return value as Boolean
        } catch (Throwable e) {
            return (Boolean)defaultValue(parameter)
        }
    }
    /**
     * Parses an Object to a Double
     * @param value The value to parse
     * @param parameter The parameter definition
     * @return The parsed Double
     */
    private Double parseDouble(Object value, Parameter parameter) {
        if (value == null) {
            return (Double)defaultValue(parameter)
        }
        try {
            return value as Double
        } catch (Throwable e) {
            return (Double)defaultValue(parameter)
        }
    }
    /**
     * Parses an Object to a List
     * @param value The value to parse
     * @param parameter The parameter definition
     * @return The parsed List
     */
    private List parseList(Object value, Parameter parameter) {
        if (value == null) {
            return (List)defaultValue(parameter)
        }
        try {
            List cast = value as List
            if (cast.size() == 0) {
                return (List)defaultValue(parameter)
            }
            return cast
        } catch (Throwable e) {
            return (List)defaultValue(parameter)
        }
    }
    /**
     * Parses an Object to a Map
     * @param value The value to parse
     * @param parameter The parameter definition
     * @return The parsed Map
     */
    private Map parseMap(Object value, Parameter parameter) {
        if (value == null) {
            return (Map)defaultValue(parameter)
        }
        try {
            Map cast = value as Map
            if (cast.size() == 0) {
                return (Map)defaultValue(parameter)
            }
            return cast
        } catch (Throwable e) {
            return (Map)defaultValue(parameter)
        }
    }
    /**
     * Returns the default value for optional parameters.
     * Throws for mandatory parameters.
     *
     * @param parameter The parameter definition
     * @return The default value for a parameter
     */
    private Object defaultValue(Parameter parameter) {
        String type = parameter.type.name.tokenize('.').last()
        if (parameter.isOptional) {
            return parameter.defaultValue
        } else {
            script.log.error(
                "Failed to parse value for mandatory parameter " +
                "'${parameter.name}' of type $type"
            )
            script.error('Failed to parse arguments')
        }
    }
    /**
     * Internal class for dealing with parameter definitions
     *
     * @param <T> The type of the parameter
     */
    private abstract class Parameter<T> {
        /**
         * The name of the parameter
         */
        String name
        /**
         * The default value of the parameter
         */
        T defaultValue
        /**
         * Whether the parameter is optional
         */
        boolean isOptional
        /**
         * Constructor for mandatory parameters
         *
         * @param name The name of the parameter
         */
        Parameter(String name) {
            if (name == null) {
                script.log.error(
                    'Invalid argument name'
                )
                script.error('Failed to set up argument parser')
            }
            isOptional = false
            this.name = name
            defaultValue = null
        }
        /**
         * Constructor for optional parameters
         *
         * @param name The name of the parameter
         * @param defaultValue The default value of the parameter
         */
        Parameter(String name, T defaultValue) {
            if (name == null) {
                script.log.error(
                    'Invalid argument name'
                )
                script.error('Failed to set up argument parser')
            }
            isOptional = true
            this.name = name
            this.defaultValue = defaultValue
        }
        /**
         * Abstract method that returns the type of the parameter.
         * It would have been nice to just do <code>return T.class</code>,
         * but Java doesn't work like that as it erases the type information
         * at runtime, hence we now have all those apparently redundant
         * implementations in other internal classes below.
         *
         * @return The type of the parameter
         */
        abstract Class getType()
    }
    @InheritConstructors
    private final class StringParameter extends Parameter<String> {
        /**
         * See superclass
         */
        Class getType() { String.class }
    }
    @InheritConstructors
    private final class IntegerParameter extends Parameter<Integer> {
        /**
         * See superclass
         */
        Class getType() { Integer.class }
    }
    @InheritConstructors
    private final class BooleanParameter extends Parameter<Boolean> {
        /**
         * See superclass
         */
        Class getType() { Boolean.class }
    }
    @InheritConstructors
    private final class DoubleParameter extends Parameter<Double> {
        /**
         * See superclass
         */
        Class getType() { Double.class }
    }
    @InheritConstructors
    private final class ListParameter extends Parameter<List> {
        /**
         * See superclass
         */
        Class getType() { List.class }
    }
    @InheritConstructors
    private final class MapParameter extends Parameter<Map> {
        /**
         * See superclass
         */
        Class getType() { Map.class }
    }
}
