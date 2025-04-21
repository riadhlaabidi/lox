#include "chunk.h"
#include "memory.h"
#include "value.h"
#include <stdint.h>

void init_chunk(Chunk *chunk)
{
    chunk->count = 0;
    chunk->capacity = 0;
    chunk->code = NULL;
    chunk->lines = NULL;
    init_value_array(&chunk->constants);
}

void write_chunk(Chunk *chunk, uint8_t byte, int line)
{
    if (chunk->capacity < chunk->count + 1) {
        int old_capacity = chunk->capacity;
        chunk->capacity = GROW_CAPACITY(old_capacity);
        chunk->code = GROW_ARRAY(uint8_t, chunk->code, chunk->capacity);
        chunk->lines = GROW_ARRAY(int, chunk->lines, chunk->capacity);
    }

    chunk->code[chunk->count] = byte;
    chunk->lines[chunk->count] = line;
    chunk->count++;
}

void write_constant(Chunk *chunk, Value value, int line)
{
    int index = add_constant(chunk, value);
    if (index < 256) {
        write_chunk(chunk, OP_CONSTANT, line);
        write_chunk(chunk, index, line);
    } else {
        write_chunk(chunk, OP_CONSTANT_LONG, line);
        for (int i = 0; i < 3; i++) {
            uint8_t b = (index >> (8 * i)) & 0xFF;
            write_chunk(chunk, b, line);
        }
    }
}

int add_constant(Chunk *chunk, Value value)
{
    write_value_array(&chunk->constants, value);
    return chunk->constants.count - 1;
}

void free_chunk(Chunk *chunk)
{
    FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
    FREE_ARRAY(int, chunk->lines, chunk->capacity);
    free_value_array(&chunk->constants);
    init_chunk(chunk);
}
