package tech.riadh.lox;

import java.util.ArrayList;
import java.util.List;

/**
 * Environment stores bindings of variables to values.
 */
class Environment {
	final Environment enclosing;
	private final List<Object> values = new ArrayList<>();

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
	 * @param value The value of the variable
	 */
	void define(Object value) {
		values.add(value);
	}

	/**
	 * Returns the value of the variable at a given distance of enclosing
	 * environments.
	 * 
	 * @param distance The distance to go up before getting the value of the
	 *                 variable.
	 * @param slot     The index of the variable in the environment list.
	 * @return The value of the variable
	 */
	Object getAt(int distance, int slot) {
		return ancestor(distance).values.get(slot);
	}

	/**
	 * Assigns a new value to a variable with the given name at the given distance
	 * far from the current environment.
	 *
	 * @param distance The distance to go up before assigning the variable.
	 * @param slot     The index of the variable to assign to, in the environment
	 *                 list.
	 * @param value    The value to assign.
	 */
	void assignAt(int distance, int slot, Object value) {
		ancestor(distance).values.set(slot, value);
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
