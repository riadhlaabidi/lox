package tech.riadh.lox;

enum TokenType {
	// Single character tokens
	COLON,
	COMMA,
	DOT,
	LEFT_BRACE,
	LEFT_PAREN,
	MINUS,
	PLUS,
	QUESTION,
	RIGHT_BRACE,
	RIGHT_PAREN,
	SEMICOLON,
	SLASH,
	STAR,

	// One or two character tokens
	BANG,
	BANG_EQUAL,
	EQUAL,
	EQUAL_EQUAL,
	GREATER,
	GREATER_EQUAL,
	LESS,
	LESS_EQUAL,

	// Literals
	IDENTIFIER,
	STRING,
	NUMBER,

	// Keywords
	AND,
	CLASS,
	ELSE,
	FALSE,
	FUN,
	FOR,
	IF,
	NIL,
	OR,
	PRINT,
	RETURN,
	SUPER,
	THIS,
	TRUE,
	VAR,
	WHILE,

	EOF
}
