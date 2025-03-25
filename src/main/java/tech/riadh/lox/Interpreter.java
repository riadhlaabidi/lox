package tech.riadh.lox;

import java.util.List;

import tech.riadh.lox.Expr.Assign;
import tech.riadh.lox.Expr.Binary;
import tech.riadh.lox.Expr.Grouping;
import tech.riadh.lox.Expr.Literal;
import tech.riadh.lox.Expr.Unary;
import tech.riadh.lox.Expr.Variable;
import tech.riadh.lox.Stmt.Block;
import tech.riadh.lox.Stmt.Var;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

	private Environment environment = new Environment();
	private static Object unitialized = new Object();

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
		Object value = environment.get(expr.name);

		if (value == unitialized) {
			throw new RuntimeError(expr.name, "Variable '" + expr.name.lexeme + "' has not been initialized.");
		}

		return value;
	}

	@Override
	public Object visitAssignExpr(Assign expr) {
		Object value = evaluate(expr.value);
		environment.assign(expr.name, value);
		return value;
	}

	@Override
	public Void visitVarStatement(Var stmt) {
		Object value = unitialized;
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
	public Void visitPrintStatement(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitBlockStatement(Block stmt) {
		executeBlock(stmt, new Environment(environment));
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
	 * @param block            The block statement to execute
	 * @param blockEnvironment The block environment
	 */
	private void executeBlock(Block block, Environment blockEnvironment) {
		Environment previous = this.environment;
		try {
			this.environment = blockEnvironment;
			for (Stmt s : block.statements) {
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
	 * Interprets an expression and return its value.
	 *
	 * @param expression The expression to be interpreted
	 * @return The string representation of the interpreted value of the expression,
	 *         or null in case of an interpretation error.
	 */
	String interpret(Expr expression) {
		try {
			Object value = evaluate(expression);
			return stringify(value);
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
			return null;
		}
	}

	/**
	 * Interprets an expression.
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
