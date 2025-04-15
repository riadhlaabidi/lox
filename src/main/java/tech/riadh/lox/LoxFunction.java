package tech.riadh.lox;

import java.util.List;

import tech.riadh.lox.Stmt.Function;

class LoxFunction implements LoxCallable {
	private final Stmt.Function declaration;
	private final Environment closure;
	private final boolean isInitializer;

	LoxFunction(Function declaration, Environment closure, boolean isInitializer) {
		this.declaration = declaration;
		this.closure = closure;
		this.isInitializer = isInitializer;
	}

	LoxFunction bind(LoxInstance instance) {
		Environment environment = new Environment(closure);
		environment.define("this", instance);
		return new LoxFunction(declaration, environment, isInitializer);
	}

	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		Environment env = new Environment(closure);

		for (int i = 0; i < declaration.params.size(); i++) {
			env.define(declaration.params.get(i).lexeme, arguments.get(i));
		}

		try {
			interpreter.executeBlock(declaration.body, env);
		} catch (Return e) {
			if (isInitializer) {
				return closure.getAt(0, "this");
			}
			return e.value;
		}

		if (isInitializer) {
			return closure.getAt(0, "this");
		}

		return null;
	}

	@Override
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}
}
