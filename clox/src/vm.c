#include <stdio.h>

#include "chunk.h"
#include "common.h"
#include "debug.h"
#include "value.h"
#include "vm.h"

static InterpretResult run(VM *vm);
static void reset_stack(VM *vm);

void init_VM(VM *vm) { reset_stack(vm); }

void free_VM(VM *vm) {}

void push(VM *vm, Value value)
{
    *vm->stack_top = value;
    vm->stack_top++;
}

Value pop(VM *vm)
{
    vm->stack_top--;
    return *vm->stack_top;
}

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
#define BINARY_OP(operator)                                                    \
    do {                                                                       \
        double b = pop(vm);                                                    \
        double a = pop(vm);                                                    \
        push(vm, a operator b);                                                \
    } while (0)

    while (1) {
#ifdef DEBUG_TRACE_EXECUTION
        printf("    stack--");
        printf("[");
        // print stack
        for (Value *slot = vm->stack; slot < vm->stack_top; slot++) {
            printf(" ");
            print_value(*slot);
            printf(",");
        }
        printf(" ]");
        printf("\n");
        disassemble_instruction(vm->chunk, (int)(vm->ip - vm->chunk->code));
#endif /* ifdef DEBUG_TRACE_EXECUTION */

        uint8_t instruction;
        switch (instruction = READ_BYTE()) {
            case OP_CONSTANT: {
                Value constant = READ_CONSTANT();
                push(vm, constant);
                break;
            }
            case OP_ADD: {
                BINARY_OP(+);
                break;
            }
            case OP_SUBTRACT: {
                BINARY_OP(-);
                break;
            }
            case OP_MULTIPLY: {
                BINARY_OP(*);
                break;
            }
            case OP_DIVIDE: {
                BINARY_OP(/);
                break;
            }
            case OP_NEGATE: {
                push(vm, -pop(vm));
                break;
            }
            case OP_RETURN: {
                print_value(pop(vm));
                printf("\n");
                return INTERPRET_OK;
            }
        }
    }
#undef READ_BYTE
#undef READ_CONSTANT
#undef BINARY_OP
}

static void reset_stack(VM *vm) { vm->stack_top = vm->stack; }
