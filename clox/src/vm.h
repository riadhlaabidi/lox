#ifndef CLOX_VM_H
#define CLOX_VM_H

#include "chunk.h"

typedef struct {
    Chunk *chunk;
    uint8_t *ip;
} VM;

typedef enum {
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_RUNTIME_ERROR
} InterpretResult;

void init_VM(VM *vm);
void free_VM(VM *vm);

InterpretResult interpret(VM *vm, Chunk *chunk);

#endif /* end of include guard: CLOX_VM_H */
