#include <stdio.h>

#include "chunk.h"
#include "common.h"
#include "debug.h"
#include "vm.h"

static InterpretResult run(VM *vm);

void init_VM(VM *vm) {}

void free_VM(VM *vm) {}

InterpretResult interpret(VM *vm, Chunk *chunk)
{
    vm->chunk = chunk;
    vm->ip = vm->chunk->code;
    return run(vm);
}

static InterpretResult run(VM *vm)
{
#define READ_BYTE() (*vm->ip++)
#define READ_CONSTANT() (vm->chunk->constants.values[READ_BYTE()])

    while (1) {
#ifdef DEBUG_TRACE_EXECUTION
        disassemble_instruction(vm->chunk, (int)(vm->ip - vm->chunk->code));
#endif /* ifdef DEBUG_TRACE_EXECUTION */

        uint8_t instruction;
        switch (instruction = READ_BYTE()) {
            case OP_CONSTANT: {
                Value constant = READ_CONSTANT();
                print_value(constant);
                printf("\n");
                break;
            }
            case OP_RETURN: {
                return INTERPRET_OK;
            }
        }
    }
#undef READ_BYTE
#undef READ_CONSTANT
}
