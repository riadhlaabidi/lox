#include <stdarg.h>
#include <stddef.h>
#include <stdio.h>

#include "chunk.h"
#include "compiler.h"
#include "debug.h"
#include "value.h"
#include "vm.h"

static InterpretResult run(VM *vm);
static void reset_stack(VM *vm);
static void runtime_error(VM *vm, const char *format, ...);

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

Value peek(VM *vm, int distance) { return vm->stack_top[-1 - distance]; }

static int is_falsey(Value value)
{
    return IS_NIL(value) || (IS_BOOL(value) && !AS_BOOL(value));
}

InterpretResult interpret(VM *vm, const char *source)
{
    Chunk chunk;
    init_chunk(&chunk);

    if (!compile(source, &chunk)) {
        free_chunk(&chunk);
        return INTERPRET_COMPILE_ERROR;
    }

    vm->chunk = &chunk;
    vm->ip = vm->chunk->code;

    InterpretResult res = run(vm);

    free_chunk(&chunk);
    return res;
}

static InterpretResult run(VM *vm)
{
#define READ_BYTE() (*vm->ip++)
#define READ_CONSTANT() (vm->chunk->constants.values[READ_BYTE()])

#define BINARY_OP(value_type, operator)                                        \
    do {                                                                       \
        if (!IS_NUMBER(peek(vm, 0)) && !IS_NUMBER(peek(vm, 1))) {              \
            runtime_error(vm, "Operands must be numbers.");                    \
            return INTERPRET_RUNTIME_ERROR;                                    \
        }                                                                      \
        double b = AS_NUMBER(pop(vm));                                         \
        double a = AS_NUMBER(pop(vm));                                         \
        push(vm, value_type(a operator b));                                    \
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
            case OP_NIL:
                push(vm, NIL_VAL);
                break;
            case OP_TRUE:
                push(vm, BOOL_VALUE(true));
                break;
            case OP_FALSE:
                push(vm, BOOL_VALUE(false));
                break;
            case OP_EQUAL: {
                Value b = pop(vm);
                Value a = pop(vm);
                push(vm, BOOL_VALUE(values_equal(a, b)));
                break;
            }
            case OP_GREATER:
                BINARY_OP(BOOL_VALUE, >);
                break;
            case OP_LESS:
                BINARY_OP(BOOL_VALUE, <);
                break;
            case OP_ADD:
                BINARY_OP(NUMBER_VALUE, +);
                break;
            case OP_SUBTRACT:
                BINARY_OP(NUMBER_VALUE, -);
                break;
            case OP_MULTIPLY:
                BINARY_OP(NUMBER_VALUE, *);
                break;
            case OP_DIVIDE:
                BINARY_OP(NUMBER_VALUE, /);
                break;
            case OP_NOT:
                push(vm, BOOL_VALUE(is_falsey(pop(vm))));
                break;
            case OP_NEGATE: {
                if (!IS_NUMBER(peek(vm, 0))) {
                    runtime_error(vm, "Operand must be a number.");
                    return INTERPRET_RUNTIME_ERROR;
                }
                push(vm, NUMBER_VALUE(-AS_NUMBER(pop(vm))));
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

static void runtime_error(VM *vm, const char *format, ...)
{
    va_list args;
    va_start(args, format);
    vfprintf(stderr, format, args);
    va_end(args);
    fputs("\n", stderr);

    size_t instruction = vm->ip - vm->chunk->code - 1;
    int line = get_line(vm->chunk, instruction);
    fprintf(stderr, "[line %d] in script\n", line);
    reset_stack(vm);
}
