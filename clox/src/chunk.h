#ifndef CLOX_CHUNK_H
#define CLOX_CHUNK_H

#include "common.h"
#include "value.h"

typedef enum {
    OP_ADD,
    OP_CONSTANT,
    OP_CONSTANT_LONG,
    OP_DIVIDE,
    OP_EQUAL,
    OP_FALSE,
    OP_GREATER,
    OP_LESS,
    OP_MULTIPLY,
    OP_NEGATE,
    OP_NIL,
    OP_NOT,
    OP_PRINT,
    OP_POP,
    OP_RETURN,
    OP_SUBTRACT,
    OP_TRUE,
} OpCode;

typedef struct {
    int offset;
    int line;
} Line;

typedef struct {
    int count;
    int capacity;
    uint8_t *code;
    ValueArray constants;
    int lines_count;
    int lines_capacity;
    Line *lines;
} Chunk;

void init_chunk(Chunk *chunk);
void write_chunk(Chunk *chunk, uint8_t byte, int line);
int add_constant(Chunk *chunk, Value value);
void write_constant(Chunk *chunk, Value value, int line);
void free_chunk(Chunk *chunk);

/**
 * Get line number given a chunk's instruction index
 */
int get_line(Chunk *chunk, int instruction_index);

#endif // CLOX_CHUNK_H
