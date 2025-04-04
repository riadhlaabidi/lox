package tech.riadh.lox;

import java.util.List;

import tech.riadh.lox.Stmt.Function;

class LoxFunction implements LoxCallable {
	private final Stmt.Function declaration;
	private final Environment closure;

	LoxFunction(Function declaration, Environment closure) {
		this.declaration = declaration;
		this.closure = closure;
	}

	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		Environment env = new Environment(closure);

		for (int i = 0; i < declaration.params.size(); i++) {
			env.define(arguments.get(i));
		}

		try {
			interpreter.executeBlock(declaration.body, env);
		} catch (Return e) {
			return e.value;
		}

		return null;
	}

	@Override
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}
}
