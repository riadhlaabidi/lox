#ifndef CLOX_VM_H
#define CLOX_VM_H

#include "chunk.h"
#include "scanner.h"

#define STACK_MAX 256

typedef struct {
    Chunk *chunk;
    uint8_t *ip;
    Value stack[STACK_MAX];
    Value *stack_top;
} VM;

typedef enum {
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_RUNTIME_ERROR
} InterpretResult;

void init_VM(VM *vm);
void free_VM(VM *vm);
void push(VM *vm, Value value);
Value pop(VM *vm);
InterpretResult interpret(Scanner *scanner, const char *source);

#endif /* end of include guard: CLOX_VM_H */
