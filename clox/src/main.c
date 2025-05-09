#include <stdio.h>
#include <stdlib.h>

#include "vm.h"

static void repl(VM *vm);
static void run_file(VM *vm, const char *path);
static char *read_file(const char *path);

int main(int argc, char **argv)
{
    VM vm;

    init_VM(&vm);

    if (argc == 1) {
        repl(&vm);
    } else if (argc == 2) {
        run_file(&vm, argv[1]);
    } else {
        fprintf(stderr, "Usage: lox <file.lox>\n");
        exit(64);
    }

    free_VM(&vm);
    return EXIT_SUCCESS;
}

static void repl(VM *vm)
{
    char line[1024];
    while (1) {
        printf("> ");

        if (!fgets(line, sizeof(line), stdin)) {
            printf("\n");
            break;
        }

        interpret(vm, line);
    }
}
static void run_file(VM *vm, const char *path)
{
    char *source = read_file(path);
    InterpretResult result = interpret(vm, source);
    free(source);

    if (result == INTERPRET_COMPILE_ERROR)
        exit(65);
    if (result == INTERPRET_RUNTIME_ERROR)
        exit(70);
}

static char *read_file(const char *path)
{
    FILE *file = fopen(path, "rb");
    if (!file) {
        fprintf(stderr, "Could not open file \"%s\".\n", path);
        exit(74);
    }

    fseek(file, 0L, SEEK_END);
    size_t file_size = ftell(file);
    rewind(file);

    char *buffer = (char *)malloc(file_size + 1);
    if (!buffer) {
        fprintf(stderr, "Memory allocation failed.\n");
        exit(74);
    }

    size_t bytes_read = fread(buffer, sizeof(char), file_size, file);
    if (bytes_read < file_size) {
        fprintf(stderr, "Could not read file \"%s\".\n", path);
        exit(74);
    }

    buffer[bytes_read] = '\0';

    fclose(file);
    return buffer;
}
