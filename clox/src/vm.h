#ifndef CLOX_VM_H
#define CLOX_VM_H

#include "chunk.h"
#include "hash_table.h"
#include "value.h"

#define STACK_MAX 256

typedef struct {
    Chunk *chunk;
    uint8_t *ip;
    Value stack[STACK_MAX];
    Value *stack_top;
    Object *objects;
    HashTable strings;
} VM;

typedef enum {
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_RUNTIME_ERROR
} InterpretResult;

void init_VM();
void free_VM();
void push(Value value);
Value pop();
Value peek(int distance);
InterpretResult interpret(const char *source);

extern VM vm;

#endif /* end of include guard: CLOX_VM_H */
