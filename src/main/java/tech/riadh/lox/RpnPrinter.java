package tech.riadh.lox;

import tech.riadh.lox.Expr.Binary;
import tech.riadh.lox.Expr.Grouping;
import tech.riadh.lox.Expr.Literal;
import tech.riadh.lox.Expr.Unary;
import tech.riadh.lox.Expr.Variable;

/**
 * A visitor class that converts an expression to
 * Reverse Polish Notation (both operands placed before the operator)
 * 
 * @see <a href=
 *      "https://craftinginterpreters.com/representing-code.html#challenges">Challenges</a>
 */
class RpnPrinter implements Expr.Visitor<String> {

	@Override
	public String visitBinaryExpr(Binary expr) {
		return expr.left.accept(this) + " "
				+ expr.right.accept(this) + " "
				+ expr.operator.lexeme;
	}

	@Override
	public String visitGroupingExpr(Grouping expr) {
		return expr.expression.accept(this);
	}

	@Override
	public String visitLiteralExpr(Literal expr) {
		if (expr.value == null) {
			return "nil";
		}
		return expr.value.toString();
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		return expr.right.accept(this) + " " + expr.operator.lexeme;
	}

	@Override
	public String visitVariableExpr(Variable expr) {
		return expr.name.lexeme;
	}

	private String print(Expr expr) {
		return expr.accept(this);
	}

	public static void main(String[] args) {
		// (1 + 2) * (4 - 3)
		Expr exp = new Expr.Binary(
				// (1 + 2)
				new Expr.Grouping(
						new Expr.Binary(
								new Expr.Literal(1),
								new Token(TokenType.PLUS, "+", null, 1),
								new Expr.Variable(new Token(TokenType.IDENTIFIER, "a", null, 1)))),
				// *
				new Token(TokenType.STAR, "*", null, 1),
				// (4 - 3)
				new Expr.Grouping(
						new Expr.Binary(
								new Expr.Literal(4),
								new Token(TokenType.MINUS, "-", null, 1),
								new Expr.Literal(3))));

		System.out.println(new RpnPrinter().print(exp));
	}
}
