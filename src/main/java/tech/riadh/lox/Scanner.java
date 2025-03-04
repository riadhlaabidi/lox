package tech.riadh.lox;

import java.util.ArrayList;
import java.util.List;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            case '(' -> addToken(TokenType.LEFT_PAREN);
            case ')' -> addToken(TokenType.RIGHT_PAREN);
            case '{' -> addToken(TokenType.LEFT_BRACE);
            case '}' -> addToken(TokenType.RIGHT_BRACE);
            case ',' -> addToken(TokenType.COMMA);
            case '.' -> addToken(TokenType.DOT);
            case '-' -> addToken(TokenType.MINUS);
            case '+' -> addToken(TokenType.PLUS);
            case ';' -> addToken(TokenType.SEMICOLON);
            case '*' -> addToken(TokenType.STAR);

            // Tokens that might or not be two characters long
            case '!' -> addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
            case '>' -> addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case '<' -> addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case '=' -> addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);

            case '/' -> {
                if (match('/')) {
                    // we're scanning a single line comment
                    while (peek() != '\n' && !isAtEnd()) {
                        advance(); // consume characters until the end of the line
                    }
                } else {
                    addToken(TokenType.SLASH);
                }
            }

            case ' ', '\r', '\t' -> {
                // Ignore whitespaces
            }

            case '\n' -> line++;

            case '"' -> string();

            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> number();

            default -> Lox.error(line, "Unexpected character.");
        }
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        Token t = new Token(type, text, literal, line);
        tokens.add(t);
    }

    /**
     * Consumes a string token enclosed in ""
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // valid string, consume the closing " character
        advance();

        // Trim the enclosing quotes
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);

        // NOTE: unescaping escape sequences would be here if supported.
    }

    private void number() {
        while (isDigit(peek())) {
            advance();
        }

        if (peek() == '.' && isDigit(peekAhead())) {
            advance(); // consume the '.'
            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Consumes the current character, and advances the current pointer.
     * 
     * @return the current character in the source code
     */
    private char advance() {
        return source.charAt(current++);
    }

    /**
     * Returns the current uncomsumed character.
     * 
     * @return the current unconsumed character
     */
    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    /**
     * Returns the next to current uncomsumed character.
     * 
     * @return the next to current unconsumed character
     */
    private char peekAhead() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    /**
     * Consumes the current character if it matches the expected param.
     * 
     * @param expected character to match
     * @return true If there is a match
     * @return false If there is no match, or reached end of source code
     */
    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }

        if (source.charAt(current) != expected) {
            return false;
        }

        current++;
        return true;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }
}
