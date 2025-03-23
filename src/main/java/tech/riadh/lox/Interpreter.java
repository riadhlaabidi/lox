package tech.riadh.lox;

import tech.riadh.lox.Expr.Binary;
import tech.riadh.lox.Expr.Grouping;
import tech.riadh.lox.Expr.Literal;
import tech.riadh.lox.Expr.Unary;

class Interpreter implements Expr.Visitor<Object> {

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
				if (left instanceof String || right instanceof String) {
					yield stringify(left) + stringify(right);
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

	void interpret(Expr expression) {
		try {
			Object value = evaluate(expression);
			System.out.println(stringify(value));
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}

	private Object evaluate(Expr expr) {
		return expr.accept(this);
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
			if ((double) right == 0d) {
				throw new RuntimeError(operator, "Division by zero.");
			}
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
