#include <stdio.h>
#include <stdlib.h>

#include "compiler.h"
#include "object.h"
#include "scanner.h"

#ifdef DEBUG_PRINT_CODE
#include "debug.h"
#endif

typedef struct {
    Token current;
    Token previous;
    int had_error;
    int panic_mode;
} Parser;

typedef enum {
    PREC_NONE,
    PREC_ASSIGNMENT,
    PREC_OR,
    PREC_AND,
    PREC_EQUALITY,
    PREC_COMPARISON,
    PREC_TERM,
    PREC_FACTOR,
    PREC_UNARY,
    PREC_CALL,
    PREC_PRIMARY
} Precedence;

typedef void (*ParseFunc)();

typedef struct {
    ParseFunc prefix;
    ParseFunc infix;
    Precedence precedence;
} ParseRule;

Parser parser;
Chunk *compiling_chunk;

static void declaration();
static void expression();
static void literal();
static void number();
static void string();
static void grouping();
static void unary();
static void binary();
static void parse_precedence(Precedence precedence);
static void advance();
static int match(TokenType type);
static void error_at_current(const char *msg);
static void error_at(Token *token, const char *msg);
static void error(const char *msg);
static void consume(TokenType type, const char *msg);
static void emit_byte(uint8_t byte);
static void emit_bytes(uint8_t byte1, uint8_t byte2);
static void emit_return();
static void emit_constant(Value value);
static void end_compiler();
static Chunk *current_chunk();
static uint8_t make_constant(Value value);

int compile(const char *source, Chunk *chunk)
{
    init_scanner(source);
    compiling_chunk = chunk;
    parser.had_error = 0;
    parser.panic_mode = 0;
    advance();

    while (!match(TOKEN_EOF)) {
        declaration();
    }

    end_compiler();
    return !parser.had_error;
}

ParseRule rules[] = {
    [TOKEN_LEFT_PAREN] = {grouping, NULL, PREC_NONE},
    [TOKEN_RIGHT_PAREN] = {NULL, NULL, PREC_NONE},
    [TOKEN_LEFT_BRACE] = {NULL, NULL, PREC_NONE},
    [TOKEN_RIGHT_BRACE] = {NULL, NULL, PREC_NONE},
    [TOKEN_COMMA] = {NULL, NULL, PREC_NONE},
    [TOKEN_DOT] = {NULL, NULL, PREC_NONE},
    [TOKEN_MINUS] = {unary, binary, PREC_TERM},
    [TOKEN_PLUS] = {NULL, binary, PREC_TERM},
    [TOKEN_SEMICOLON] = {NULL, NULL, PREC_NONE},
    [TOKEN_SLASH] = {NULL, binary, PREC_FACTOR},
    [TOKEN_STAR] = {NULL, binary, PREC_FACTOR},
    [TOKEN_BANG] = {unary, NULL, PREC_NONE},
    [TOKEN_BANG_EQUAL] = {NULL, binary, PREC_COMPARISON},
    [TOKEN_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_EQUAL_EQUAL] = {NULL, binary, PREC_COMPARISON},
    [TOKEN_GREATER] = {NULL, binary, PREC_COMPARISON},
    [TOKEN_GREATER_EQUAL] = {NULL, binary, PREC_COMPARISON},
    [TOKEN_LESS] = {NULL, binary, PREC_COMPARISON},
    [TOKEN_LESS_EQUAL] = {NULL, binary, PREC_COMPARISON},
    [TOKEN_IDENTIFIER] = {NULL, NULL, PREC_NONE},
    [TOKEN_STRING] = {string, NULL, PREC_NONE},
    [TOKEN_NUMBER] = {number, NULL, PREC_NONE},
    [TOKEN_AND] = {NULL, NULL, PREC_NONE},
    [TOKEN_CLASS] = {NULL, NULL, PREC_NONE},
    [TOKEN_ELSE] = {NULL, NULL, PREC_NONE},
    [TOKEN_FALSE] = {literal, NULL, PREC_NONE},
    [TOKEN_FOR] = {NULL, NULL, PREC_NONE},
    [TOKEN_FUN] = {NULL, NULL, PREC_NONE},
    [TOKEN_IF] = {NULL, NULL, PREC_NONE},
    [TOKEN_NIL] = {literal, NULL, PREC_NONE},
    [TOKEN_OR] = {NULL, NULL, PREC_NONE},
    [TOKEN_PRINT] = {NULL, NULL, PREC_NONE},
    [TOKEN_RETURN] = {NULL, NULL, PREC_NONE},
    [TOKEN_SUPER] = {NULL, NULL, PREC_NONE},
    [TOKEN_THIS] = {NULL, NULL, PREC_NONE},
    [TOKEN_TRUE] = {literal, NULL, PREC_NONE},
    [TOKEN_VAR] = {NULL, NULL, PREC_NONE},
    [TOKEN_WHILE] = {NULL, NULL, PREC_NONE},
    [TOKEN_ERROR] = {NULL, NULL, PREC_NONE},
    [TOKEN_EOF] = {NULL, NULL, PREC_NONE},
};

static void print_statement()
{
    expression();
    consume(TOKEN_SEMICOLON, "Expected ';' after expression.");
    emit_byte(OP_PRINT);
}

static void statement()
{
    if (match(TOKEN_PRINT)) {
        print_statement();
    }
}
static void declaration() { statement(); }
static void expression() { parse_precedence(PREC_ASSIGNMENT); }

static void literal()
{
    switch (parser.previous.type) {
        case TOKEN_FALSE:
            emit_byte(OP_FALSE);
            break;
        case TOKEN_TRUE:
            emit_byte(OP_TRUE);
            break;
        case TOKEN_NIL:
            emit_byte(OP_NIL);
            break;
        default:
            assert(0 && "Unreachable code");
    }
}

static void number()
{
    double value = strtod(parser.previous.start, NULL);
    emit_constant(NUMBER_VALUE(value));
}

static void string()
{
    const char *start = parser.previous.start + 1;
    int length = parser.previous.length - 2;
    emit_constant(OBJECT_VALUE(copy_string(start, length)));
}

static void grouping()
{
    expression();
    consume(TOKEN_RIGHT_PAREN, "Expected ')' after expression.");
}

static void unary()
{
    TokenType operator= parser.previous.type;

    parse_precedence(PREC_UNARY);

    switch (operator) {
        case TOKEN_MINUS:
            emit_byte(OP_NEGATE);
            break;
        case TOKEN_BANG:
            emit_byte(OP_NOT);
            break;
        default:
            assert(0 && "Unreachable code");
    }
}

static void binary()
{
    TokenType operator_type = parser.previous.type;
    parse_precedence(rules[operator_type].precedence + 1);

    switch (operator_type) {
        case TOKEN_BANG_EQUAL:
            emit_bytes(OP_EQUAL, OP_NOT);
            break;
        case TOKEN_EQUAL_EQUAL:
            emit_byte(OP_EQUAL);
            break;
        case TOKEN_GREATER:
            emit_byte(OP_GREATER);
            break;
        case TOKEN_GREATER_EQUAL:
            emit_bytes(OP_LESS, OP_NOT);
            break;
        case TOKEN_LESS:
            emit_byte(OP_LESS);
            break;
        case TOKEN_LESS_EQUAL:
            emit_bytes(OP_GREATER, OP_NOT);
            break;
        case TOKEN_PLUS:
            emit_byte(OP_ADD);
            break;
        case TOKEN_MINUS:
            emit_byte(OP_SUBTRACT);
            break;
        case TOKEN_STAR:
            emit_byte(OP_MULTIPLY);
            break;
        case TOKEN_SLASH:
            emit_byte(OP_DIVIDE);
            break;
        default:
            assert(0 && "Unreachable code");
    }
}

static void parse_precedence(Precedence precedence)
{
    advance();
    ParseFunc prefix_rule = rules[parser.previous.type].prefix;
    if (prefix_rule == NULL) {
        error("Expected an expression.");
        return;
    }
    prefix_rule();

    while (precedence <= rules[parser.current.type].precedence) {
        advance();
        ParseFunc infix_rule = rules[parser.previous.type].infix;
        infix_rule();
    }
}

static void emit_constant(Value value)
{
    emit_bytes(OP_CONSTANT, make_constant(value));
}

static uint8_t make_constant(Value value)
{
    int constant = add_constant(current_chunk(), value);
    if (constant > UINT8_MAX) {
        error("Too many constants in one chunk");
        return 0;
    }

    return (uint8_t)constant;
}

static void advance()
{
    parser.previous = parser.current;
    while (1) {
        parser.current = scan_token();
        if (parser.current.type != TOKEN_ERROR)
            break;
        error_at_current(parser.current.start);
    }
}
static void error_at_current(const char *msg)
{
    error_at(&parser.current, msg);
}

static void error_at(Token *token, const char *msg)
{
    if (parser.panic_mode)
        return;

    parser.panic_mode = 1;
    fprintf(stderr, "[line %d] Error:", token->line);

    if (token->type == TOKEN_EOF) {
        fprintf(stderr, " at the end");
    } else if (token->type == TOKEN_ERROR) {

    } else {
        fprintf(stderr, " at '%.*s'", token->length, token->start);
    }

    fprintf(stderr, ": %s\n", msg);
    parser.had_error = 1;
}

static void error(const char *msg) { error_at(&parser.previous, msg); }

static void consume(TokenType type, const char *msg)
{
    if (parser.current.type == type) {
        advance();
        return;
    }
    error_at_current(msg);
}

static int check(TokenType type) { return parser.current.type == type; }

static int match(TokenType type)
{
    if (!check(type)) {
        return 0;
    }
    advance();
    return 1;
}

static void emit_byte(uint8_t byte)
{
    write_chunk(current_chunk(), byte, parser.previous.line);
}

static void emit_bytes(uint8_t byte1, uint8_t byte2)
{
    emit_byte(byte1);
    emit_byte(byte2);
}

static void emit_return() { emit_byte(OP_RETURN); }

static void end_compiler()
{
    emit_return();

#ifdef DEBUG_PRINT_CODE
    if (!parser.had_error) {
        disassemble_chunk(current_chunk(), "code");
    }
#endif
}

static Chunk *current_chunk() { return compiling_chunk; }
