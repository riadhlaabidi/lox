#include "scanner.h"
#include <string.h>

static int is_at_end(Scanner *scanner);
static int is_digit(char c);
static Token make_token(Scanner *scanner, TokenType type);
static Token error_token(Scanner *scanner, const char *msg);
static char advance(Scanner *scanner);
static int match(Scanner *scanner, char expected);
static void skip_whitespaces(Scanner *scanner);
static char peek(Scanner *scanner);
static char peek_next(Scanner *scanner);
static Token string(Scanner *scanner);
static Token number(Scanner *scanner);

void init_scanner(Scanner *scanner, const char *source)
{
    scanner->start = source;
    scanner->current = source;
    scanner->line = 1;
}

Token scan_token(Scanner *scanner)
{
    skip_whitespaces(scanner);
    scanner->start = scanner->current;

    if (is_at_end(scanner)) {
        return make_token(scanner, TOKEN_EOF);
    }

    char c = advance(scanner);

    if (is_digit(c))
        return number(scanner);

    switch (c) {
        case '(':
            return make_token(scanner, TOKEN_LEFT_PAREN);
        case ')':
            return make_token(scanner, TOKEN_RIGHT_PAREN);
        case '{':
            return make_token(scanner, TOKEN_LEFT_BRACE);
        case '}':
            return make_token(scanner, TOKEN_RIGHT_BRACE);
        case ';':
            return make_token(scanner, TOKEN_SEMICOLON);
        case ',':
            return make_token(scanner, TOKEN_COMMA);
        case '.':
            return make_token(scanner, TOKEN_DOT);
        case '-':
            return make_token(scanner, TOKEN_MINUS);
        case '+':
            return make_token(scanner, TOKEN_PLUS);
        case '/':
            return make_token(scanner, TOKEN_SLASH);
        case '*':
            return make_token(scanner, TOKEN_STAR);
        case '!': {
            TokenType type = match(scanner, '=') ? TOKEN_BANG_EQUAL
                                                 : TOKEN_BANG;
            return make_token(scanner, type);
        }
        case '<': {
            TokenType type = match(scanner, '=') ? TOKEN_LESS_EQUAL
                                                 : TOKEN_LESS;
            return make_token(scanner, type);
        }
        case '>': {
            TokenType type = match(scanner, '=') ? TOKEN_GREATER_EQUAL
                                                 : TOKEN_GREATER;
            return make_token(scanner, type);
        }
        case '=': {
            TokenType type = match(scanner, '=') ? TOKEN_EQUAL_EQUAL
                                                 : TOKEN_EQUAL;
            return make_token(scanner, type);
        }
        case '"':
            return string(scanner);
    }

    return error_token(scanner, "Unexpected character.");
}

static int is_at_end(Scanner *scanner) { return *scanner->current == '\0'; }

static int is_digit(char c) { return c >= '0' && c <= '9'; }

static char advance(Scanner *scanner)
{
    scanner->current++;
    return scanner->current[-1];
}

static int match(Scanner *scanner, char expected)
{
    if (is_at_end(scanner) || *scanner->current != expected)
        return 0;

    scanner->current++;
    return 1;
}

static void skip_whitespaces(Scanner *scanner)
{
    while (1) {
        char c = peek(scanner);
        switch (c) {
            case ' ':
            case '\r':
            case '\t':
                advance(scanner);
                break;
            case '\n':
                scanner->line++;
                advance(scanner);
                break;
            case '/':
                if (peek_next(scanner) == '/') {
                    while (peek(scanner) != '\n' && !is_at_end(scanner)) {
                        advance(scanner);
                    }
                } else {
                    return;
                }
            default:
                return;
        }
    }
}

static Token string(Scanner *scanner)
{
    while (peek(scanner) != '"' && !is_at_end(scanner)) {
        if (peek(scanner) == '\n')
            scanner->line++;
        advance(scanner);
    }
    if (is_at_end(scanner))
        return error_token(scanner, "Unterminated string.");

    advance(scanner);
    return make_token(scanner, TOKEN_STRING);
}

static Token number(Scanner *scanner)
{
    while (is_digit(peek(scanner)))
        advance(scanner);

    if (peek(scanner) == '.' && is_digit(peek_next(scanner))) {
        advance(scanner);

        while (is_digit(peek(scanner)))
            advance(scanner);
    }

    return make_token(scanner, TOKEN_NUMBER);
}

static char peek(Scanner *scanner) { return *scanner->current; }

static char peek_next(Scanner *scanner)
{
    if (is_at_end(scanner))
        return '\0';

    return scanner->current[1];
}

static Token make_token(Scanner *scanner, TokenType type)
{
    Token token;
    token.start = scanner->start;
    token.type = type;
    token.length = (int)(scanner->current - scanner->start);
    token.line = scanner->line;
    return token;
}

static Token error_token(Scanner *scanner, const char *msg)
{
    Token token;
    token.start = msg;
    token.type = TOKEN_ERROR;
    token.length = (int)strlen(msg);
    token.line = scanner->line;
    return token;
}
