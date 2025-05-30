package tech.riadh.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import tech.riadh.lox.Expr.Assign;
import tech.riadh.lox.Expr.Binary;
import tech.riadh.lox.Expr.Call;
import tech.riadh.lox.Expr.Get;
import tech.riadh.lox.Expr.Grouping;
import tech.riadh.lox.Expr.Literal;
import tech.riadh.lox.Expr.Logical;
import tech.riadh.lox.Expr.Set;
import tech.riadh.lox.Expr.Super;
import tech.riadh.lox.Expr.This;
import tech.riadh.lox.Expr.Unary;
import tech.riadh.lox.Expr.Variable;
import tech.riadh.lox.Stmt.Block;
import tech.riadh.lox.Stmt.Expression;
import tech.riadh.lox.Stmt.Function;
import tech.riadh.lox.Stmt.If;
import tech.riadh.lox.Stmt.Print;
import tech.riadh.lox.Stmt.Return;
import tech.riadh.lox.Stmt.Var;
import tech.riadh.lox.Stmt.While;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
	/**
	 * Stores scopes' mappings while resolving variables, each variable is
	 * represented by its name and a boolean flag indicating whether or not the
	 * resolution is finished.
	 */
	private final Stack<Map<String, Boolean>> scopes = new Stack<>();

	private final Interpreter interpreter;
	private FunctionType currentFunction = FunctionType.NONE;
	private ClassType currentClass = ClassType.NONE;

	Resolver(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	@Override
	public Void visitVarStatement(Var stmt) {
		declare(stmt.name);
		if (stmt.initializer != null) {
			resolve(stmt.initializer);
		}
		define(stmt.name);
		return null;
	}

	@Override
	public Void visitExpressionStatement(Expression stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitPrintStatement(Print stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitBlockStatement(Block stmt) {
		beginScope();
		resolve(stmt.statements);
		endScope();
		return null;
	}

	@Override
	public Void visitIfStatement(If stmt) {
		resolve(stmt.condition);
		resolve(stmt.thenBranch);
		if (stmt.elseBranch != null) {
			resolve(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitWhileStatement(While stmt) {
		resolve(stmt.condition);
		resolve(stmt.body);
		return null;
	}

	@Override
	public Void visitFunctionStatement(Function stmt) {
		declare(stmt.name);
		define(stmt.name);
		resolveFunction(stmt, FunctionType.FUNCTION);
		return null;
	}

	@Override
	public Void visitReturnStatement(Return stmt) {
		if (currentFunction == FunctionType.NONE) {
			Lox.error(stmt.keyword, "Can't return outside of a function.");
		}
		if (stmt.value != null) {
			if (currentFunction == FunctionType.INITIALIZER) {
				Lox.error(stmt.keyword, "Can't return a value from an initializer.");
			}
			resolve(stmt.value);
		}
		return null;
	}

	@Override
	public Void visitClassStatement(Stmt.Class stmt) {
		ClassType enclosingClass = currentClass;
		currentClass = ClassType.CLASS;

		declare(stmt.name);
		define(stmt.name);

		if (stmt.superclass != null) {
			if (stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
				Lox.error(stmt.superclass.name, "A class can't inherit from itself.");
			}
			currentClass = ClassType.SUBCLASS;
			resolve(stmt.superclass);
			beginScope();
			scopes.peek().put("super", true);
		}

		beginScope();
		scopes.peek().put("this", true);

		for (Stmt.Function method : stmt.methods) {
			FunctionType declaration = FunctionType.METHOD;
			if (method.name.lexeme.equals("init")) {
				declaration = FunctionType.INITIALIZER;
			}
			resolveFunction(method, declaration);
		}

		endScope();

		if (stmt.superclass != null) {
			endScope();
		}

		currentClass = enclosingClass;
		return null;
	}

	@Override
	public Void visitBinaryExpr(Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitGroupingExpr(Grouping expr) {
		resolve(expr.expression);
		return null;
	}

	@Override
	public Void visitLiteralExpr(Literal expr) {
		return null;
	}

	@Override
	public Void visitLogicalExpr(Logical expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitUnaryExpr(Unary expr) {
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitVariableExpr(Variable expr) {
		if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
			Lox.error(expr.name, "Can't read local variable in its own initializer");
		}
		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitAssignExpr(Assign expr) {
		resolve(expr.value);
		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitCallExpr(Call expr) {
		resolve(expr.callee);
		for (Expr arg : expr.arguments) {
			resolve(arg);
		}
		return null;
	}

	@Override
	public Void visitGetExpr(Get expr) {
		resolve(expr.object);
		return null;
	}

	@Override
	public Void visitSetExpr(Set expr) {
		resolve(expr.value);
		resolve(expr.object);
		return null;
	}

	@Override
	public Void visitThisExpr(This expr) {
		if (currentClass == ClassType.NONE) {
			Lox.error(expr.keyword, "Can't use 'this' outside of a class");
			return null;
		}
		resolveLocal(expr, expr.keyword);
		return null;
	}

	@Override
	public Void visitSuperExpr(Super expr) {
		if (currentClass == ClassType.NONE) {
			Lox.error(expr.keyword, "Can't use 'super' outside of a class.");
		} else if (currentClass != ClassType.SUBCLASS) {
			Lox.error(expr.keyword, "'super' can only be used in a subclass.");
		}

		resolveLocal(expr, expr.keyword);
		return null;
	}

	/**
	 * Resolves a list of statements.
	 * 
	 * @param statements A list of statements to resolve
	 */
	void resolve(List<Stmt> statements) {
		for (Stmt s : statements) {
			resolve(s);
		}
	}

	/**
	 * Resolves a single statement.
	 */
	private void resolve(Stmt statement) {
		statement.accept(this);
	}

	/**
	 * Resolves an expression.
	 */
	private void resolve(Expr expr) {
		expr.accept(this);
	}

	/**
	 * Begins a new scope by pushing a new HashMap to the scopes stack.
	 */
	private void beginScope() {
		scopes.push(new HashMap<String, Boolean>());
	}

	/**
	 * Ends the current scope by popping it out from the stack.
	 */
	private void endScope() {
		scopes.pop();
	}

	/**
	 * Declares a variable in the innermost scope so that it shadows any outer one.
	 * The declared variable is marked as not ready yet while it still being
	 * resolved by binding its name to false in the scope map. If there is no scope
	 * in the scopes stack, this method does nothing.
	 */
	private void declare(Token name) {
		if (scopes.isEmpty()) {
			return;
		}
		Map<String, Boolean> scope = scopes.peek();
		if (scope.containsKey(name.lexeme)) {
			Lox.error(name, "Already a variable with this name in this scope.");
		}
		scope.put(name.lexeme, false);
	}

	/**
	 * Defines a variable after resolving its initializer expression in the same
	 * scope where it existed but has not been fully initialized yet. This method
	 * sets its resolution flag to true indicating that it's now fully initialized
	 * and is available for use. If scopes stack is empty, this method
	 * does nothing.
	 */
	private void define(Token name) {
		if (scopes.isEmpty()) {
			return;
		}
		scopes.peek().put(name.lexeme, true);
	}

	private void resolveLocal(Expr expr, Token name) {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			if (scopes.get(i).containsKey(name.lexeme)) {
				interpreter.resolve(expr, scopes.size() - 1 - i);
				return;
			}
		}
	}

	/**
	 * Resolves a function body by creating a new scope and binding variables for
	 * each of the function's parameters.
	 *
	 * @param function The function statement to resolve
	 * @param type     The function type
	 */
	private void resolveFunction(Stmt.Function function, FunctionType type) {
		FunctionType enclosingFunction = currentFunction;
		currentFunction = type;
		beginScope();
		for (Token param : function.params) {
			declare(param);
			define(param);
		}
		resolve(function.body);
		endScope();
		currentFunction = enclosingFunction;
	}
}
