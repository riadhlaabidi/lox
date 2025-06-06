package tech.riadh.lox;

class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return "Token [type=" + type + ", lexeme=\033[0;32m" + lexeme + "\033[0m, literal=" + literal + "]";
    }
}
