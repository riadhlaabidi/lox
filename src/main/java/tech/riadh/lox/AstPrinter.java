package tech.riadh.lox;

import tech.riadh.lox.Expr.Binary;
import tech.riadh.lox.Expr.Conditional;
import tech.riadh.lox.Expr.Grouping;
import tech.riadh.lox.Expr.Literal;
import tech.riadh.lox.Expr.Unary;

class AstPrinter implements Expr.Visitor<String> {

	String print(Expr expr) {
		return expr.accept(this);
	}

	@Override
	public String visitBinaryExpr(Binary expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitGroupingExpr(Grouping expr) {
		return parenthesize("group", expr.expression);
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
		return parenthesize(expr.operator.lexeme, expr.right);
	}

	private String parenthesize(String name, Expr... exprs) {
		StringBuilder sb = new StringBuilder();

		sb.append("(");
		sb.append(name);
		for (Expr expr : exprs) {
			sb.append(" ");
			sb.append(expr.accept(this));
		}
		sb.append(")");

		return sb.toString();
	}

	@Override
	public String visitConditionalExpr(Conditional expr) {
		StringBuilder sb = new StringBuilder();
		sb.append("If");
		sb.append(parenthesize("condition", expr.condition));
		sb.append(parenthesize("then", expr.thenBranch));
		sb.append(parenthesize("else", expr.elseBranch));

		return sb.toString();
	}

	public static void main(String[] args) {
		Expr expr = new Expr.Binary(
				new Expr.Unary(new Token(TokenType.MINUS, "-", null, 1), new Expr.Literal(123)),
				new Token(TokenType.STAR, "*", null, 1),
				new Expr.Grouping(new Expr.Literal(45.67)));

		System.out.println(new AstPrinter().print(expr));
	}
}
