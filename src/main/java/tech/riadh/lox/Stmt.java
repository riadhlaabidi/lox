package tech.riadh.lox;

import java.util.List;

abstract class Stmt {

	interface Visitor<R> {
		R visitVarStatement(Var stmt);

		R visitExpressionStatement(Expression stmt);

		R visitPrintStatement(Print stmt);

		R visitBlockStatement(Block stmt);

		R visitIfStatement(If stmt);

		R visitWhileStatement(While stmt);

		R visitFunctionStatement(Function stmt);

		R visitReturnStatement(Return stmt);

		R visitClassStatement(Class stmt);
	}

	abstract <R> R accept(Visitor<R> visitor);

	static class Expression extends Stmt {
		final Expr expression;

		Expression(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStatement(this);
		}
	}

	static class Print extends Stmt {
		final Expr expression;

		Print(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStatement(this);
		}

	}

	static class Var extends Stmt {
		final Token name;
		final Expr initializer;

		Var(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarStatement(this);
		}
	}

	static class Block extends Stmt {
		final List<Stmt> statements;

		Block(List<Stmt> statements) {
			this.statements = statements;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockStatement(this);
		}
	}

	static class If extends Stmt {
		final Expr condition;
		final Stmt thenBranch;
		final Stmt elseBranch;

		If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIfStatement(this);
		}
	}

	static class While extends Stmt {
		final Expr condition;
		final Stmt body;

		While(Expr condition, Stmt body) {
			this.condition = condition;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitWhileStatement(this);
		}
	}

	static class Function extends Stmt {
		final Token name;
		final List<Token> params;
		final List<Stmt> body;

		Function(Token name, List<Token> params, List<Stmt> body) {
			this.name = name;
			this.params = params;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFunctionStatement(this);
		}
	}

	static class Return extends Stmt {
		final Token keyword;
		final Expr value;

		Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitReturnStatement(this);
		}
	}

	static class Class extends Stmt {
		final Token name;
		final List<Stmt.Function> methods;
		final List<Stmt.Function> classMethods;

		Class(Token name, List<Stmt.Function> methods, List<Stmt.Function> classMethods) {
			this.name = name;
			this.methods = methods;
			this.classMethods = classMethods;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitClassStatement(this);
		}
	}
}
