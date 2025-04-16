package tech.riadh.lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
	final String name;
	final LoxClass superclass;
	private final Map<String, LoxFunction> methods;

	LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods) {
		this.name = name;
		this.superclass = superclass;
		this.methods = methods;
	}

	/**
	 * Finds by name and returns a {@link LoxFunction} instance of a method in this
	 * class or its superclass.
	 *
	 * @param name The name of the method to find
	 * @return The instance of that method if it is found, otherwise it returns
	 *         null.
	 */
	LoxFunction findMethod(String name) {
		if (methods.containsKey(name)) {
			return methods.get(name);
		}

		if (superclass != null) {
			return superclass.findMethod(name);
		}

		return null;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int arity() {
		LoxFunction initializer = findMethod("init");
		if (initializer != null) {
			return initializer.arity();
		}

		return 0;
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		LoxInstance instance = new LoxInstance(this);

		LoxFunction initializer = findMethod("init");
		if (initializer != null) {
			initializer.bind(instance).call(interpreter, arguments);
		}

		return instance;
	}
}
