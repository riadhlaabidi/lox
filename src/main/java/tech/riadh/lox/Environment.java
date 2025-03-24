package tech.riadh.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment stores bindings of variables to values.
 */
class Environment {
	private final Map<String, Object> values = new HashMap<>();

	/**
	 * Defines a variable.
	 *
	 * @param name  The name of the variable
	 * @param value The value of the variable
	 */
	void define(String name, Object value) {
		values.put(name, value);
	}

	/**
	 * Returns the value of the variable if it exists, otherwise throws a
	 * runtime error.
	 * 
	 * @param name the variable token
	 * @return the value of the variable
	 * @throws RuntimeError If the variable does not exist
	 */
	Object get(Token name) {
		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}
		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	/**
	 * Assigns a value to an existing variable. Unlike
	 * {@link #define(String, Object) Define}, this method is not allowed to create
	 * a new variable.
	 * 
	 * @param name  The variable name token
	 * @param value The value to be assigned to the variable
	 * @throws RuntimeError If the variable does not already exist in the values map
	 */
	void assign(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			values.put(name.lexeme, value);
			return;
		}
		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

}
