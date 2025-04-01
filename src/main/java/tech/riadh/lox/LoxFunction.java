package tech.riadh.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
	private final String name;
	private final Expr.Function declaration;
	private final Environment closure;

	LoxFunction(String name, Expr.Function declaration, Environment closure) {
		this.name = name;
		this.declaration = declaration;
		this.closure = closure;
	}

	@Override
	public int arity() {
		return declaration.parameters.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		Environment env = new Environment(closure);

		for (int i = 0; i < declaration.parameters.size(); i++) {
			env.define(declaration.parameters.get(i).lexeme, arguments.get(i));
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
		if (name == null) {
			return "<fn>";
		}
		return "<fn " + name + ">";
	}
}
