#include "scanner.h"
#include <string.h>

static int is_at_end(Scanner *scanner);
static Token make_token(Scanner *scanner, TokenType type);
static Token error_token(Scanner *scanner, const char *msg);

void init_scanner(Scanner *scanner, const char *source)
{
    scanner->start = source;
    scanner->current = source;
    scanner->line = 1;
}

Token scan_token(Scanner *scanner)
{
    scanner->start = scanner->current;
    if (is_at_end(scanner)) {
        return make_token(scanner, TOKEN_EOF);
    }
    return error_token(scanner, "Unexpected character.");
}

static int is_at_end(Scanner *scanner) { return *scanner->current == '\0'; }

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
