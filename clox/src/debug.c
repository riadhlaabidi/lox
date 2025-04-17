#include <stdio.h>

#include "chunk.h"
#include "debug.h"

static int simple_instruction(const char *name, int offset);

void disassemble_chunk(Chunk *chunk, const char *name)
{
    for (int offset = 0; offset < chunk->count;) {
        offset = disassemble_instruction(chunk, offset);
    }
}

int disassemble_instruction(Chunk *chunk, int offset)
{
    printf("%04d ", offset);

    uint8_t instr = chunk->code[offset];
    switch (instr) {
        case OP_RETURN:
            return simple_instruction("OP_RETURN", offset);
        default:
            printf("Unknown opcode %d\n", instr);
            return offset + 1;
    }
}

/* Prints a simple instruction name, increments and returns the offset */
static int simple_instruction(const char *name, int offset)
{
    printf("%s\n", name);
    return offset + 1;
}
