package tech.riadh.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment stores bindings of variables to values.
 */
class Environment {
	final Environment enclosing;
	private final Map<String, Object> values = new HashMap<>();

	/**
	 * Constructs an environment without an enclosing one, this is used for the
	 * global scope environment which ends the chain.
	 */
	Environment() {
		enclosing = null;
	}

	/**
	 * Constructs an environment given the enclosing one, which creates a chain of
	 * scopes going from the outermost (global) to the innermost.
	 */
	Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}

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

		// If the variable is not found, we'll recursively go up and try enclosing
		// environments
		if (enclosing != null) {
			return enclosing.get(name);
		}

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	/**
	 * Returns the value of the variable at a given distance of enclosing
	 * environments.
	 * 
	 * @param distance The distance to go up before getting the value of the
	 *                 variable.
	 * @param name     The name of the variable.
	 * @return The value of the variable
	 */
	Object getAt(int distance, String name) {
		return ancestor(distance).values.get(name);
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

		// If the variable is not found, we'll recursively go up and try enclosing
		// environments
		if (enclosing != null) {
			enclosing.assign(name, value);
			return;
		}

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	/**
	 * Assigns a new value to a variable with the given name at the given distance
	 * far from the current environment.
	 *
	 * @param distance The distance to go up before assigning the variable.
	 * @param name     The variable name.
	 * @param value    The value to assign.
	 */
	void assignAt(int distance, Token name, Object value) {
		ancestor(distance).values.put(name.lexeme, value);
	}

	/**
	 * Returns the ancestor of the current environment that is a given distance away
	 * from the current one.
	 * 
	 * @param distance The distance to go up from the current environment
	 * @return The distance-th ancestor environment
	 */
	private Environment ancestor(int distance) {
		Environment environment = this;
		for (int i = 0; i < distance; i++) {
			environment = environment.enclosing;
		}
		return environment;
	}
}
