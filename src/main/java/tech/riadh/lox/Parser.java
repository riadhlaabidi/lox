package tech.riadh.lox;

import java.util.List;

/**
 * A Recursive Descent Parser
 *
 * expression -> conditional;
 * conditional -> comma ("?" expression ":" conditional)?;
 * comma -> equality ( "," equality)*;
 * equality -> comparison ( ( "!=" | "==" ) comparison )*;
 * comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )*;
 * term -> factor ( ( "-" | "+" ) factor )*;
 * factor -> unary ( ( "/" | "*" ) unary )*;
 * unary -> (( "!" | "-" ) unary) | primary;
 * primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
 * // error productions
 * | ("!=" | "==") equality
 * | (">"| ">="| "<"| "<=") comparison
 * | ("+") term
 * | ("*" | "/") factor;
 *
 * @see <a href=
 *      "https://craftinginterpreters.com/parsing-expressions.html#challenges">Challenges</a>
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

	Expr parse() {
		try {
			return expression();
		} catch (ParseError error) {
			return null;
		}
	}

	private Expr expression() {
		return conditional();
	}

	private Expr conditional() {
		Expr expr = comma();

		if (match(TokenType.QUESTION)) {
			Expr thenBranch = expression();
			consume(TokenType.COLON, "Expected ':' after then branch of the conditional expression.");
			Expr elseBranch = conditional();
			expr = new Expr.Conditional(expr, thenBranch, elseBranch);
		}

		return expr;
	}

	private Expr comma() {
		Expr expr = equality();

		while (match(TokenType.COMMA)) {
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
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

		if (match(TokenType.LEFT_PAREN)) {
			Expr expr = expression();
			consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
			return new Expr.Grouping(expr);
		}

		// error productions
		if (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
			error(previous(), "Missing left-hand operand.");
			equality();
			return null;
		}

		if (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
			error(previous(), "Missing left-hand operand.");
			comparison();
			return null;
		}

		if (match(TokenType.PLUS)) {
			error(previous(), "Missing left-hand operand.");
			term();
			return null;
		}

		if (match(TokenType.SLASH, TokenType.STAR)) {
			error(previous(), "Missing left-hand operand.");
			factor();
			return null;
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

	private Token consume(TokenType type, String message) {
		if (check(type)) {
			return advance();
		}
		throw error(peek(), message);
	}

	private ParseError error(Token token, String message) {
		Lox.error(token, message);
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
