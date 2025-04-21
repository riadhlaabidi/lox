#include "chunk.h"
#include "memory.h"
#include "value.h"

void init_chunk(Chunk *chunk)
{
    chunk->count = 0;
    chunk->capacity = 0;
    chunk->code = NULL;
    chunk->lines_count = 0;
    chunk->lines_capacity = 0;
    chunk->lines = NULL;
    init_value_array(&chunk->constants);
}

void write_chunk(Chunk *chunk, uint8_t byte, int line)
{
    if (chunk->capacity < chunk->count + 1) {
        int old_capacity = chunk->capacity;
        chunk->capacity = GROW_CAPACITY(old_capacity);
        chunk->code = GROW_ARRAY(uint8_t, chunk->code, old_capacity,
                                 chunk->capacity);
    }

    chunk->code[chunk->count] = byte;
    chunk->count++;

    if (chunk->lines_count > 0 &&
        chunk->lines[chunk->lines_count - 1].line == line) {
        return;
    }

    if (chunk->lines_capacity < chunk->lines_count + 1) {
        int old_capacity = chunk->lines_capacity;
        chunk->lines_capacity = GROW_CAPACITY(old_capacity);
        chunk->lines = GROW_ARRAY(Line, chunk->lines, old_capacity,
                                  chunk->lines_capacity);
    }

    chunk->lines[chunk->lines_count].offset = chunk->count - 1;
    chunk->lines[chunk->lines_count].line = line;
    chunk->lines_count++;
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
    FREE_ARRAY(Line, chunk->lines, chunk->lines_capacity);
    free_value_array(&chunk->constants);
    init_chunk(chunk);
}

int get_line(Chunk *chunk, int instruction_index)
{
    int start = 0;                // start is always inclusive
    int end = chunk->lines_count; // end is always exclusive

    // Since end is exclusive, if start reaches end-1, the line with index start
    // must be the answer, no further processing is needed.
    while (start < end - 1) {
        int middle = (start + end) / 2;
        Line *line = &chunk->lines[middle];

        if (instruction_index > line->offset) {
            start = middle;
        } else if (instruction_index < line->offset) {
            end = middle;
        } else {
            return line->line;
        }
    }

    return chunk->lines[start].line;
}
