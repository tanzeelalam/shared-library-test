import org.mcafee.orbit.ArgumentHandler

/**
 * This is a utility for parsing a Map of values.
 * It can be used to sanitize the arguments to a method call.
 * Any parameters that are not explicitely defined are ignored.
 *
 * It can:
 * <ul>
 *   <li>validate that no mandatory values are missing</li>
 *   <li>type check values</li>
 *   <li>assign default values</li>
 * </ul>
 *
 * Supported types are:
 * <ul>
 *     <li>String</li>
 *     <li>boolean</li>
 *     <li>double</li>
 *     <li>int</li>
 * </ul>
 *
 * See below for sample syntax or, better, check the API at org.mcafee.orbit.ArgumentHandler
 */

/**
 * Creates an ArgumentHandler and returns it.
 *
 * Sample usage:
 * <pre>
 *    Map args = [foo:'bar', something: 50]
 *    arguments(args)             // Instanciates the argument handler
 *      .addString('foo')         // This requires a mandatory parameter 'foo'
 *      .addBoolean('flag', true) // This allows an optional parameter 'flag' with a default value of 'true'
 *      .parse()                  // Parses the 'args' variable and prints a warning as the flag value is missing
 *    println(args)               // Prints: [foo:'bar', flag:true, something: 50]
 *    // Note variable 'something' has not been checked as it was not defined using the add* methods
 * </pre>
 *
 * Type casting:
 * <pre>
 *    Map args = [num:'123', flag:'false']
 *    arguments(args)             // Instanciates the argument handler
 *      .addInteger('num')        // This requires a mandatory parameter 'num'
 *      .addBoolean('flag')       // This requires a mandatory parameter 'flag'
 *      .parse()                  // Parses the 'args' variable
 *    println(args)               // Prints: [num:123, flag:false]
 *          // Note that the string values in the original
 *          // variables have been cast to the required types
 * </pre>
 *
 * Error:
 * <pre>
 *    Map args = [something:50]
 *    arguments(args)             // Instanciates the argument handler
 *      .addDouble('num')         // This requires a mandatory parameter 'num'
 *      .parse()                  // Parses the 'args' variable
 *    // An error is throw, as the required variable 'num' is not provided
 * </pre>
 *
 * @param args [T:Map] The arguments to sanitize
 * @return [T:ArgumentHandler]
 */
def call(Map args) {
    new ArgumentHandler(this, args)
}

/**
 * Creates an ArgumentHandler and returns it.
 *
 * Sample usage:
 * <pre>
 *    Map args = [foo:'bar', something: 50]
 *    arguments(args) {           // Instanciates the argument handler
 *      addString('foo')          // This requires a mandatory parameter 'foo'
 *      addBoolean('flag', true)  // This allows an optional parameter 'flag' with a default value of 'true'
 *      parse()                   // Parses the 'args' variable and prints a warning as the flag value is missing
 *    }
 *    println(args)               // Prints: [foo:'bar', flag:true, something: 50]
 *    // Note variable 'something' has not been checked as it was not defined using the add* methods
 * </pre>
 *
 * Type casting:
 * <pre>
 *    Map args = [num:'123', flag:'false']
 *    arguments(args) {           // Instanciates the argument handler
 *       addInteger('num')        // This requires a mandatory parameter 'num'
 *       addBoolean('flag')       // This requires a mandatory parameter 'flag'
 *       parse()                  // Parses the 'args' variable
 *    }
 *    println(args)               // Prints: [num:123, flag:false]
 *          // Note that the string values in the original
 *          // variables have been cast to the required types
 * </pre>
 *
 * Error:
 * <pre>
 *    Map args = [something:50]
 *    arguments(args) {           // Instanciates the argument handler
 *       addDouble('num')         // This requires a mandatory parameter 'num'
 *       parse()                  // Parses the 'args' variable
 *    }
 *    // An error is throw, as the required variable 'num' is not provided
 * </pre>
 *
 * @param args [T:Map] The arguments to sanitize.
 * @param closure [T:Closure] The closure that operates on the ArgumentHandler.
 * @return [T:void|ArgumentHandler]
 */
def call(Map args, Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = call(args)
    closure()
}