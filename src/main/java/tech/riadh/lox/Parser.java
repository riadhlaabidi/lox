package tech.riadh.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lox's Recursive Descent Parser
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
	 *
	 * program -> declaration* EOF;
	 *
	 * @return A list of {@link Stmt Statements}
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
	 *
	 * declaration -> varDecl | statement;
	 *
	 * @return A variable declaration statement, or an actual statement. In case of
	 *         a parse error, the parser goes into panic mode and synchronizes
	 *         tokens until the next valid state and returns null.
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
	 *
	 * varDecl -> "var" IDENTIFIER ("=" expression)? ";";
	 *
	 * @return A variable declaration statement
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
	 * Parses and returns a statement.
	 *
	 * statement -> exprStmt | ifStmt | whileStmt | forStmt | printStmt | blockStmt;
	 * 
	 * @return A statement, this could be a print statement or an expression
	 *         statement.
	 */
	private Stmt statement() {
		if (match(TokenType.IF)) {
			return ifStatement();
		}
		if (match(TokenType.WHILE)) {
			return whileStatement();
		}
		if (match(TokenType.FOR)) {
			return forStatement();
		}
		if (match(TokenType.PRINT)) {
			return printStatement();
		}
		if (match(TokenType.LEFT_BRACE)) {
			return block();
		}
		return expressionStatement();
	}

	/**
	 * Parses and returns an expression statement consuming the following semicolon.
	 *
	 * exprStmt -> expression ";";
	 *
	 * @return An expression statement
	 */
	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(TokenType.SEMICOLON, "Expected ';' after expression");
		return new Stmt.Expression(expr);
	}

	/**
	 * Parses and returns an if statement. This method eagerly looks for an else
	 * before returning, so the innermost call to a nested series will claim the
	 * else clause for itself before returning to the outer if statements.
	 *
	 * ifStmt -> "if" "(" expression ")" statement ("else" statement)?:
	 *
	 * @return An If statement
	 */
	private Stmt ifStatement() {
		consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expected ')' after if statement's condition.");

		Stmt thenBranch = statement();
		Stmt elseBranch = null;
		if (match(TokenType.ELSE)) {
			elseBranch = statement();
		}

		return new Stmt.If(condition, thenBranch, elseBranch);
	}

	/**
	 * Parses and returns a while statement
	 *
	 * whileStmt -> "while" "(" expression ")" statement;
	 * 
	 * @return A while statement
	 */
	private Stmt whileStatement() {
		consume(TokenType.LEFT_PAREN, "Expected '(' after while.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expected ')' after while condition.");
		Stmt body = statement();
		return new Stmt.While(condition, body);
	}

	/**
	 * Parses and returns a for statement. This method desugares the syntactic sugar
	 * of the for loop into a primitive form that the interpreter already knows how
	 * to execute, using statements and while loop.
	 *
	 * For instance:
	 * 
	 * <pre>
	 * <code>
	 *for (var i = 0; i < 5; i = i + 1) {
	 *    print i;
	 *}
	 * </code>
	 * </pre>
	 * 
	 * could be rewritten as:
	 * 
	 * <pre>
	 * <code>
	 *{
	 *    var i = 0;
	 *    while (i < 5) {
	 *        print i;
	 *        i = i + 1;
	 *    }
	 *}
	 * </code>
	 * </pre>
	 *
	 * forStmt -> "for" "(" (varDecl | exprStmt | ";" ) expression? ";"
	 * expression? ")" statement;
	 * 
	 * @return A for statement
	 */
	private Stmt forStatement() {
		consume(TokenType.LEFT_PAREN, " Expected '(' after 'for'.");

		Stmt initializer;
		if (match(TokenType.SEMICOLON)) {
			initializer = null;
		} else if (match(TokenType.VAR)) {
			initializer = varDeclaration();
		} else {
			initializer = expressionStatement();
		}
		// The semicolon after initializer is consumed in each case

		Expr condition = null;
		if (!check(TokenType.SEMICOLON)) {
			condition = expression();
		}
		consume(TokenType.SEMICOLON, "Expected ';' after loop condition.");

		Expr increment = null;
		if (!check(TokenType.RIGHT_PAREN)) {
			increment = expression();
		}
		consume(TokenType.RIGHT_PAREN, "Expected ')' after for clauses.");

		Stmt body = statement();

		// Desugaring for loop into already defined nodes
		if (increment != null) {
			body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
		}

		if (condition == null) {
			condition = new Expr.Literal(true);
		}
		body = new Stmt.While(condition, body);

		if (initializer != null) {
			body = new Stmt.Block(Arrays.asList(initializer, body));
		}

		return body;
	}

	/**
	 * Parses and returns a print statement cosuming the following semicolon.
	 * 
	 * printStmt -> "print" expression ";";
	 *
	 * @return A print statement
	 */
	private Stmt printStatement() {
		Expr value = expression();
		consume(TokenType.SEMICOLON, "Expected ';' after value.");
		return new Stmt.Print(value);
	}

	/**
	 * Parses and returns a block statement.
	 *
	 * blockStmt -> "{" declarations* "}";
	 *
	 * @return A block statement
	 */
	private Stmt block() {
		List<Stmt> statements = new ArrayList<>();

		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}

		consume(TokenType.RIGHT_BRACE, "Expected '}' after block.");
		return new Stmt.Block(statements);
	}

	/**
	 * Parses and returns an expression.
	 *
	 * expression -> assignment;
	 *
	 * @return An expression
	 */
	private Expr expression() {
		return assignment();
	}

	/**
	 * Parses and returns an assignment expression.
	 *
	 * assignment -> IDENTIFIER "=" assignment | logic_or;
	 *
	 * @return An assignment expression
	 */
	private Expr assignment() {
		Expr expr = or();

		if (match(TokenType.EQUAL)) {
			Token equal = previous();
			Expr value = assignment();

			if (expr instanceof Expr.Variable) {
				Expr.Variable v = (Expr.Variable) expr;
				return new Expr.Assign(v.name, value);
			}

			// report an error without throwing, the parser is in a valid state
			error(equal, "Invalid assignment target.");
		}

		return expr;
	}

	/**
	 * Parses and returns a logical or expression
	 *
	 * logic_or -> logic_and ("or" logic_and)*;
	 *
	 * @return A logical or expression
	 */
	private Expr or() {
		Expr expr = and();

		while (match(TokenType.OR)) {
			Token operator = previous();
			Expr right = and();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

	/**
	 * Parses and returns a logical and expression.
	 *
	 * logic_and -> equality ("and" equality)*;
	 * 
	 * @return A logical and expression
	 */
	private Expr and() {
		Expr expr = equality();

		while (match(TokenType.AND)) {
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

	/**
	 * Parses and returns an equality expression.
	 *
	 * equality -> comparison ( ( "!=" | "==" ) comparison )*;
	 * 
	 * @return An equality expression
	 */
	private Expr equality() {
		Expr expr = comparison();

		while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	/**
	 * Parses and returns a comparison expression.
	 *
	 * comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )*;
	 * 
	 * @return A comparison expression
	 */
	private Expr comparison() {
		Expr expr = term();

		while (match(TokenType.LESS_EQUAL, TokenType.LESS, TokenType.GREATER_EQUAL, TokenType.GREATER)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	/**
	 * Parses and returns a term expression.
	 *
	 * term -> factor ( ( "-" | "+" ) factor )*;
	 * 
	 * @return A term expression
	 */
	private Expr term() {
		Expr expr = factor();

		while (match(TokenType.PLUS, TokenType.MINUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	/**
	 * Parses and returns a factor expression.
	 *
	 * factor -> unary ( ( "/" | "*" ) unary )*;
	 * 
	 * @return A factor expression
	 */
	private Expr factor() {
		Expr expr = unary();

		while (match(TokenType.SLASH, TokenType.STAR)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	/**
	 * Parses and returns a unary expression.
	 *
	 * unary -> ( "!" | "-" ) unary | primary;
	 * 
	 * @return A unary expression
	 */
	private Expr unary() {
		if (match(TokenType.BANG, TokenType.MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return primary();
	}

	/**
	 * Parses and returns a primary expression.
	 *
	 * primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" |
	 * IDENTIFIER;
	 * 
	 * @return A primary expression
	 * @throws ParseError If it does not match a valid primary token
	 */
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
