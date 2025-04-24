#include "compiler.h"
#include "scanner.h"
#include <stdio.h>

void compile(Scanner *scanner, const char *source)
{
    init_scanner(scanner, source);
    int line = -1;

    while (1) {
        Token token = scan_token(scanner);
        if (token.line != line) {
            printf("%4d ", token.line);
            line = token.line;
        } else {
            printf("   | ");
        }

        printf("%2d '%.*s'\n", token.type, token.length, token.start);

        if (token.type == TOKEN_EOF)
            break;
    }
}
