#include <stdio.h>

#include "chunk.h"
#include "debug.h"

static int simple_instruction(const char *name, int offset);
static int constant_instruction(const char *name, Chunk *chunk, int offset);
static int constant_long_instruction(const char *name, Chunk *chunk,
                                     int offset);

void disassemble_chunk(Chunk *chunk, const char *name)
{
    printf("=== %s ===\n", name);

    for (int offset = 0; offset < chunk->count;) {
        offset = disassemble_instruction(chunk, offset);
    }

    printf("======\n");
}

int disassemble_instruction(Chunk *chunk, int offset)
{
    printf("%04d ", offset);

    int line = get_line(chunk, offset);
    if (offset > 0 && line == get_line(chunk, offset - 1)) {
        printf("   | "); // same line number as the instruction above it
    } else {
        printf("%4d ", line);
    }

    uint8_t instr = chunk->code[offset];
    switch (instr) {
        case OP_CONSTANT:
            return constant_instruction("OP_CONSTANT", chunk, offset);
        case OP_CONSTANT_LONG:
            return constant_long_instruction("OP_CONSTANT_LONG", chunk, offset);
        case OP_RETURN:
            return simple_instruction("OP_RETURN", offset);
        default:
            printf("Unknown opcode %d\n", instr);
            return offset + 1;
    }
}

/*
 * Prints a simple instruction in the form:
 * <offset> <name>
 * then, increments and returns the offset of the next instruction.
 */
static int simple_instruction(const char *name, int offset)
{
    printf("%s\n", name);
    return offset + 1;
}

/**
 * Prints a constant instruction in the form:
 * <offset> <name> <constant index> <constant value>
 * then, increments and returns the offset of the next instruction.
 */
static int constant_instruction(const char *name, Chunk *chunk, int offset)
{
    uint8_t constant = chunk->code[offset + 1];
    printf("%s %4d '", name, constant);
    print_value(chunk->constants.values[constant]);
    printf("'\n");
    return offset + 2;
}

static int constant_long_instruction(const char *name, Chunk *chunk, int offset)
{
    uint32_t constant = chunk->code[offset + 1] |
                        (chunk->code[offset + 2] << 8) |
                        (chunk->code[offset + 3] << 16);
    printf("%s %d '", name, constant);
    print_value(chunk->constants.values[constant]);
    printf("'\n");
    return offset + 4;
}
