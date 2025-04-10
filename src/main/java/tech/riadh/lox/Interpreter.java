package tech.riadh.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.riadh.lox.Expr.Assign;
import tech.riadh.lox.Expr.Binary;
import tech.riadh.lox.Expr.Call;
import tech.riadh.lox.Expr.Get;
import tech.riadh.lox.Expr.Grouping;
import tech.riadh.lox.Expr.Literal;
import tech.riadh.lox.Expr.Logical;
import tech.riadh.lox.Expr.Set;
import tech.riadh.lox.Expr.This;
import tech.riadh.lox.Expr.Unary;
import tech.riadh.lox.Expr.Variable;
import tech.riadh.lox.Stmt.Block;
import tech.riadh.lox.Stmt.Class;
import tech.riadh.lox.Stmt.Function;
import tech.riadh.lox.Stmt.If;
import tech.riadh.lox.Stmt.Var;
import tech.riadh.lox.Stmt.While;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	private final Map<Expr, Integer> locals = new HashMap<>();
	final Environment globals = new Environment();
	private Environment environment = globals;

	Interpreter() {
		globals.define("clock", new LoxCallable() {
			@Override
			public int arity() {
				return 0;
			}

			@Override
			public Object call(Interpreter interpreter, List<Object> arguments) {
				return (double) System.currentTimeMillis() / 1000.0;
			}

			@Override
			public String toString() {
				return "<native function>";
			}
		});
	}

	@Override
	public Object visitBinaryExpr(Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		return switch (expr.operator.type) {
			case GREATER -> {
				checkNumberOperands(expr.operator, left, right);
				yield (double) left > (double) right;
			}
			case GREATER_EQUAL -> {
				checkNumberOperands(expr.operator, left, right);
				yield (double) left >= (double) right;
			}
			case LESS -> {
				checkNumberOperands(expr.operator, left, right);
				yield (double) left < (double) right;
			}
			case LESS_EQUAL -> {
				checkNumberOperands(expr.operator, left, right);
				yield (double) left <= (double) right;
			}
			case EQUAL_EQUAL -> isEqual(left, right);
			case BANG_EQUAL -> !isEqual(left, right);
			case PLUS -> {
				if (left instanceof String && right instanceof String) {
					yield (String) left + (String) right;
				}
				if (left instanceof Double && right instanceof Double) {
					yield (double) left + (double) right;
				}
				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			}
			case MINUS -> {
				checkNumberOperands(expr.operator, left, right);
				yield (double) left - (double) right;
			}
			case SLASH -> {
				checkNumberOperands(expr.operator, left, right);
				yield (double) left / (double) right;
			}
			case STAR -> {
				checkNumberOperands(expr.operator, left, right);
				yield (double) left * (double) right;
			}

			default -> null;
		};
	}

	@Override
	public Object visitGroupingExpr(Grouping expr) {
		return evaluate(expr.expression);
	}

	@Override
	public Object visitLiteralExpr(Literal expr) {
		return expr.value;
	}

	@Override
	public Object visitLogicalExpr(Logical expr) {
		Object left = evaluate(expr.left);
		if (expr.operator.type == TokenType.AND) {
			if (!isTruthy(left)) {
				return left;
			}
		} else {
			if (isTruthy(left)) {
				return left;
			}
		}

		return evaluate(expr.right);
	}

	@Override
	public Object visitUnaryExpr(Unary expr) {
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case BANG -> {
				return !isTruthy(right);
			}
			case MINUS -> {
				checkNumberOperand(expr.operator, right);
				return -(double) right;
			}
			default -> {
				// unreachable
				return null;
			}
		}
	}

	@Override
	public Object visitVariableExpr(Variable expr) {
		return lookUpVariable(expr.name, expr);
	}

	private Object lookUpVariable(Token name, Expr expr) {
		Integer distance = locals.get(expr);
		if (distance != null) {
			return environment.getAt(distance, name.lexeme);
		}
		return globals.get(name);
	}

	@Override
	public Object visitAssignExpr(Assign expr) {
		Object value = evaluate(expr.value);

		Integer distance = locals.get(expr);
		if (distance != null) {
			environment.assignAt(distance, expr.name, value);
		} else {
			globals.assign(expr.name, value);
		}
		return value;
	}

	@Override
	public Object visitCallExpr(Call expr) {
		Object callee = evaluate(expr.callee);

		List<Object> args = new ArrayList<>();
		for (Expr arg : expr.arguments) {
			args.add(evaluate(arg));
		}

		if (!(callee instanceof LoxCallable)) {
			throw new RuntimeError(expr.paren, "Can only call functions or classes.");
		}

		LoxCallable function = (LoxCallable) callee;
		if (args.size() != function.arity()) {
			throw new RuntimeError(expr.paren,
					"Exptected " + function.arity() + " arguments but got " + args.size() + ".");
		}
		return function.call(this, args);
	}

	@Override
	public Object visitGetExpr(Get expr) {
		Object object = evaluate(expr.object);

		if (object instanceof LoxInstance) {
			return ((LoxInstance) object).get(expr.name);
		}

		throw new RuntimeError(expr.name, "Only instances have properties.");
	}

	@Override
	public Object visitSetExpr(Set expr) {
		Object object = evaluate(expr.object);

		if (object instanceof LoxInstance) {
			Object value = evaluate(expr.value);
			((LoxInstance) object).set(expr.name, value);
			return value;
		}

		throw new RuntimeError(expr.name, "Only instances have fields.");
	}

	@Override
	public Object visitThisExpr(This expr) {
		return lookUpVariable(expr.keyword, expr);
	}

	@Override
	public Void visitVarStatement(Var stmt) {
		Object value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}
		environment.define(stmt.name.lexeme, value);
		return null;
	}

	@Override
	public Void visitExpressionStatement(Stmt.Expression stmt) {
		evaluate(stmt.expression);
		return null;
	}

	@Override
	public Void visitIfStatement(If stmt) {
		if (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.thenBranch);
		} else if (stmt.elseBranch != null) {
			execute(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitWhileStatement(While stmt) {
		while (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.body);
		}
		return null;
	}

	@Override
	public Void visitPrintStatement(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitBlockStatement(Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}

	@Override
	public Void visitFunctionStatement(Function stmt) {
		LoxFunction function = new LoxFunction(stmt, environment, false);
		environment.define(stmt.name.lexeme, function);
		return null;
	}

	@Override
	public Void visitReturnStatement(Stmt.Return stmt) {
		if (stmt.value != null) {
			throw new Return(evaluate(stmt.value));
		}
		throw new Return(null);
	}

	@Override
	public Void visitClassStatement(Class stmt) {
		environment.define(stmt.name.lexeme, null);

		Map<String, LoxFunction> methods = new HashMap<>();
		for (Stmt.Function method : stmt.methods) {
			LoxFunction m = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
			methods.put(method.name.lexeme, m);
		}

		LoxClass loxClass = new LoxClass(stmt.name.lexeme, methods);
		environment.assign(stmt.name, loxClass);
		return null;
	}

	/**
	 * Executes a block statement in the context of a given environment.
	 * This method changes the {@link #environment environment} field to the
	 * given blockEnvironment which corresponds to the innermost scope
	 * containing the code to be executed, and then restores the previous
	 * environment back regardless of whether it ecountered an exception or not
	 * while being executed.
	 * 
	 * @param statements       A list of statements to execute
	 * @param blockEnvironment The block environment
	 */
	void executeBlock(List<Stmt> statements, Environment blockEnvironment) {
		Environment previous = this.environment;
		try {
			this.environment = blockEnvironment;
			for (Stmt s : statements) {
				execute(s);
			}
		} finally {
			this.environment = previous;
		}
	}

	void interpret(List<Stmt> statements) {
		try {
			for (Stmt stmt : statements) {
				execute(stmt);
			}
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}

	/**
	 * Interprets an expression.
	 *
	 * @return The evaluation of an expression
	 */
	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	/**
	 * Interprets a statement.
	 */
	private void execute(Stmt stmt) {
		stmt.accept(this);
	}

	void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
	}

	private boolean isTruthy(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean) {
			return (boolean) o;
		}
		return true;
	}

	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}

		if (a == null) {
			return false;
		}

		return a.equals(b);
	}

	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double) {
			return;
		}
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double) {
			return;
		}
		throw new RuntimeError(operator, "Operands must be numbers.");
	}

	private String stringify(Object o) {
		if (o == null) {
			return "nil";
		}

		if (o instanceof Double) {
			String s = o.toString();
			if (s.endsWith(".0")) {
				s = s.substring(0, s.length() - 2);
			}
			return s;
		}

		return o.toString();
	}

}
