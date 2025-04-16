package tech.riadh.lox;

import java.util.List;

/**
 * LoxCallable is an interface to be implemented by any Java representation of a
 * Lox object that can be called like a function, that includes user-defined
 * functions and class objects when called to construct new instances.
 */
interface LoxCallable {
	/**
	 * Returns the number of arguments a function or operation expects.
	 */
	int arity();

	Object call(Interpreter interpreter, List<Object> arguments);
}
