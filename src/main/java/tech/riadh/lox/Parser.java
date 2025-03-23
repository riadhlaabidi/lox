package tech.riadh.lox;

import java.util.ArrayList;
import java.util.List;

/**
 * A Recursive Descent Parser
 *
 * expression -> equality;
 * equality -> comparison ( ( "!=" | "==" ) comparison )*;
 * comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )*;
 * term -> factor ( ( "-" | "+" ) factor )*;
 * factor -> unary ( ( "/" | "*" ) unary )*;
 * unary -> ( "!" | "-" ) unary | primary;
 * primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" |
 * IDENTIFIER;
 *
 */
class Parser {

	private static class ParseError extends RuntimeException {
	}

	private List<Token> tokens;
	private int current = 0;

	Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	/**
	 * Parses a Lox program.
	 * program -> declaration* EOF;
	 */
	List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();

		while (!isAtEnd()) {
			statements.add(declaration());
		}

		return statements;
	}

	/**
	 * Parses a declaration statement. The declaration rule falls through to
	 * parsing a statement if it doesn't match a variable declaration.
	 * declaration -> varDecl | statement;
	 */
	private Stmt declaration() {
		try {
			if (match(TokenType.VAR)) {
				return varDeclaration();
			}
			return statement();
		} catch (ParseError error) {
			synchronize();
			return null;
		}
	}

	/**
	 * Parses and returns a variable declaration statement.
	 * varDecl -> "var" IDENTIFIER ("=" expression)? ";";
	 */
	private Stmt varDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expected variable name.");
		Expr initializer = null;

		if (match(TokenType.EQUAL)) {
			initializer = expression();
		}

		consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}

	/**
	 * Parses a statement.
	 * statement -> exprStmt | printStmt;
	 */
	private Stmt statement() {
		if (match(TokenType.PRINT)) {
			return printStatement();
		}
		return expressionStatement();
	}

	/**
	 * Parses and returns a print statement cosuming the following semicolon.
	 * printStmt -> "print" expression ";";
	 */
	private Stmt printStatement() {
		Expr value = expression();
		consume(TokenType.SEMICOLON, "Expected ';' after value.");
		return new Stmt.Print(value);
	}

	/**
	 * Parses and returns an expression statement consuming the following semicolon.
	 * exprStmt -> expression ";";
	 */
	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(TokenType.SEMICOLON, "Expected ';' after expression");
		return new Stmt.Expression(expr);
	}

	private Expr expression() {
		return equality();
	}

	private Expr equality() {
		Expr expr = comparison();

		while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr comparison() {
		Expr expr = term();

		while (match(TokenType.LESS_EQUAL, TokenType.LESS, TokenType.GREATER_EQUAL, TokenType.GREATER)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr term() {
		Expr expr = factor();

		while (match(TokenType.PLUS, TokenType.MINUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr factor() {
		Expr expr = unary();

		while (match(TokenType.SLASH, TokenType.STAR)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr unary() {
		if (match(TokenType.BANG, TokenType.MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return primary();
	}

	private Expr primary() {
		if (match(TokenType.FALSE)) {
			return new Expr.Literal(false);
		}

		if (match(TokenType.TRUE)) {
			return new Expr.Literal(true);
		}

		if (match(TokenType.NIL)) {
			return new Expr.Literal(null);
		}

		if (match(TokenType.NUMBER, TokenType.STRING)) {
			return new Expr.Literal(previous().literal);
		}

		if (match(TokenType.IDENTIFIER)) {
			return new Expr.Variable(previous());
		}

		if (match(TokenType.LEFT_PAREN)) {
			Expr expr = expression();
			consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
			return new Expr.Grouping(expr);
		}

		throw error(peek(), "Expected an expression.");
	}

	private void synchronize() {
		advance();

		while (!isAtEnd()) {
			if (previous().type == TokenType.SEMICOLON) {
				return;
			}

			switch (peek().type) {
				case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> {
					return;
				}

				default -> {
					// nothing
				}
			}

			advance();
		}
	}

	private Token consume(TokenType type, String msg) {
		if (check(type)) {
			return advance();
		}
		throw error(peek(), msg);
	}

	private ParseError error(Token t, String msg) {
		Lox.error(t, msg);
		return new ParseError();
	}

	private boolean match(TokenType... types) {
		for (TokenType t : types) {
			if (check(t)) {
				advance();
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if the current token is of the given type.
	 * 
	 * @param type Token type to check the current one on
	 * @return true if the current token is of the given type, false otherwise
	 */
	private boolean check(TokenType type) {
		if (isAtEnd()) {
			return false;
		}
		return peek().type == type;
	}

	/*
	 * Consumes the current token and returns it.
	 * 
	 * @return current token
	 */
	private Token advance() {
		if (!isAtEnd()) {
			current++;
		}
		return previous();
	}

	/*
	 * Peeks the current token in the list, without consuming it.
	 *
	 * @return the current token
	 */
	private Token peek() {
		return tokens.get(current);
	}

	/*
	 * Checks whether or not you reached the end of the tokens list.
	 * 
	 * @return true if the end is reached, false otherwise
	 */
	private boolean isAtEnd() {
		return peek().type == TokenType.EOF;
	}

	/*
	 * Returns the previous token, the consumed one before current.
	 *
	 * @return the previous consumed token
	 */
	private Token previous() {
		return tokens.get(current - 1);
	}

}
