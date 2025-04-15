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
	 * parsing a statement if it doesn't match a function or a variable declaration.
	 *
	 * declaration -> funDecl | classDecl | varDecl | statement;
	 *
	 * @return A function, variable, or an actual declaration statement. In case of
	 *         a parse error, the parser goes into panic mode and synchronizes
	 *         tokens until the next valid state and returns null.
	 */
	private Stmt declaration() {
		try {
			if (match(TokenType.FUN)) {
				return function("function");
			}
			if (match(TokenType.CLASS)) {
				return classDeclaration();
			}
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
	 * Parses and returns a function declaration statement.
	 *
	 * funDecl -> "fun" function;
	 * function -> IDENTIFIER "(" parameters? ")" block;
	 * parameters -> IDENTIFIER ("," IDENTIFIER)*;
	 * 
	 * @return A function declaration statement
	 */
	private Stmt.Function function(String kind) {
		Token name = consume(TokenType.IDENTIFIER, "Expected " + kind + " name.");
		consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
		List<Token> parameters = new ArrayList<>();

		if (!check(TokenType.RIGHT_PAREN)) {
			do {
				if (parameters.size() >= 255) {
					error(peek(), "Can't have more than 255 parameters.");
				}

				parameters.add(consume(TokenType.IDENTIFIER, "Expected parameter name."));
			} while (match(TokenType.COMMA));
		}
		consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");

		consume(TokenType.LEFT_BRACE, "Expected '{' before " + kind + " body.");
		List<Stmt> body = block();
		return new Stmt.Function(name, parameters, body);
	}

	/**
	 * Parses and returns a class declaration statement.
	 *
	 * classDecl -> "class" IDENTIFIER ("<" IDENTIFIER)? "{" function* "}";
	 * 
	 * @return A class declaration statement
	 */
	private Stmt classDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expected class name.");
		Expr.Variable superclass = null;

		if (match(TokenType.LESS)) {
			Token scn = consume(TokenType.IDENTIFIER, "Expected superclass name after '<'.");
			superclass = new Expr.Variable(scn);
		}

		consume(TokenType.LEFT_BRACE, "Expected '{' before class body.");

		List<Stmt.Function> methods = new ArrayList<>();
		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			methods.add(function("method"));
		}

		consume(TokenType.RIGHT_BRACE, "Expected '}' after class body.");

		return new Stmt.Class(name, superclass, methods);
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
	 * statement -> exprStmt | ifStmt | whileStmt | forStmt | printStmt | blockStmt
	 * | returnStmt;
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
			return new Stmt.Block(block());
		}
		if (match(TokenType.RETURN)) {
			return returnStatement();
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
	 * Parses and returns a block statement. This method assumes that the block's
	 * opening brace is already consumed.
	 *
	 * blockStmt -> "{" declarations* "}";
	 *
	 * @return A block statement
	 */
	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();

		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}

		consume(TokenType.RIGHT_BRACE, "Expected '}' after block.");
		return statements;
	}

	/**
	 * Parses and returns a return statement.
	 *
	 * returnStmt -> "return" expression? ";";
	 * 
	 * @return A return statement
	 */
	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;
		if (!check(TokenType.SEMICOLON)) {
			value = expression();
		}
		consume(TokenType.SEMICOLON, "Expected ';' after return value.");
		return new Stmt.Return(keyword, value);
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
	 * assignment -> (call ".")? IDENTIFIER "=" assignment | logic_or;
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
			} else if (expr instanceof Expr.Get) {
				Expr.Get get = (Expr.Get) expr;
				return new Expr.Set(get.object, get.name, value);
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
	 * unary -> ( "!" | "-" ) unary | call;
	 * 
	 * @return A unary expression
	 */
	private Expr unary() {
		if (match(TokenType.BANG, TokenType.MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return call();
	}

	/**
	 * Parses and returns a call expression.
	 *
	 * call -> primary ( "(" arguments? ")" | "." IDENTIFIER)*;
	 * arguments -> expression ("," expression)*;
	 * 
	 * @return A call expression
	 */
	private Expr call() {
		Expr expr = primary();

		while (true) {
			if (match(TokenType.LEFT_PAREN)) {
				List<Expr> args = new ArrayList<>();
				if (!check(TokenType.RIGHT_PAREN)) {
					// parse arguments
					do {
						if (args.size() >= 255) {
							// same limit as java for later compatibility
							error(peek(), "Can't have more than 255 arguments");
						}
						args.add(expression());
					} while (match(TokenType.COMMA));
				}
				Token paren = consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments.");
				expr = new Expr.Call(expr, paren, args);
			} else if (match(TokenType.DOT)) {
				Token name = consume(TokenType.IDENTIFIER, "Expected property name after '.'");
				expr = new Expr.Get(expr, name);
			} else {
				break;
			}
		}

		return expr;
	}

	/**
	 * Parses and returns a primary expression.
	 *
	 * primary -> "false" | "nil" | "this" | "true" | IDENTIFIER | NUMBER | STRING |
	 * "(" expression ")" | "super" "." IDENTIFIER;
	 * 
	 * @return A primary expression
	 * @throws ParseError If it does not match a valid primary token
	 */
	private Expr primary() {
		if (match(TokenType.FALSE)) {
			return new Expr.Literal(false);
		}

		if (match(TokenType.NIL)) {
			return new Expr.Literal(null);
		}

		if (match(TokenType.THIS)) {
			return new Expr.This(previous());
		}

		if (match(TokenType.TRUE)) {
			return new Expr.Literal(true);
		}

		if (match(TokenType.IDENTIFIER)) {
			return new Expr.Variable(previous());
		}

		if (match(TokenType.NUMBER, TokenType.STRING)) {
			return new Expr.Literal(previous().literal);
		}

		if (match(TokenType.LEFT_PAREN)) {
			Expr expr = expression();
			consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
			return new Expr.Grouping(expr);
		}

		if (match(TokenType.SUPER)) {
			Token keyword = previous();
			consume(TokenType.DOT, "Expected '.' after 'super'.");
			Token method = consume(TokenType.IDENTIFIER, "Expected superclass method name.");
			return new Expr.Super(keyword, method);
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
