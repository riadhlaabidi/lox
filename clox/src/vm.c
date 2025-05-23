#include <stdarg.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>

#include "chunk.h"
#include "compiler.h"
#include "debug.h"
#include "hash_table.h"
#include "memory.h"
#include "object.h"
#include "value.h"
#include "vm.h"

VM vm;

static void reset_stack() { vm.stack_top = vm.stack; }

void init_VM()
{
    reset_stack();
    vm.objects = NULL;
    HT_init(&vm.strings);
}

void free_VM()
{
    HT_free(&vm.strings);
    free_objects();
}

void push(Value value)
{
    *vm.stack_top = value;
    vm.stack_top++;
}

Value pop()
{
    vm.stack_top--;
    return *vm.stack_top;
}

Value peek(int distance) { return vm.stack_top[-1 - distance]; }

static int is_falsey(Value value)
{
    return IS_NIL(value) || (IS_BOOL(value) && !AS_BOOL(value));
}

static void runtime_error(const char *format, ...)
{
    va_list args;
    va_start(args, format);
    vfprintf(stderr, format, args);
    va_end(args);
    fputs("\n", stderr);

    size_t instruction = vm.ip - vm.chunk->code - 1;
    int line = get_line(vm.chunk, instruction);
    fprintf(stderr, "[line %d] in script\n", line);
    reset_stack();
}

static InterpretResult run()
{
#define READ_BYTE() (*vm.ip++)
#define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])

#define BINARY_OP(value_type, operator)                                        \
    do {                                                                       \
        if (!IS_NUMBER(peek(0)) || !IS_NUMBER(peek(1))) {                      \
            runtime_error("Operands must be numbers.");                        \
            return INTERPRET_RUNTIME_ERROR;                                    \
        }                                                                      \
        double b = AS_NUMBER(pop());                                           \
        double a = AS_NUMBER(pop());                                           \
        push(value_type(a operator b));                                        \
    } while (0)

    while (1) {
#ifdef DEBUG_TRACE_EXECUTION
        printf("    stack--");
        printf("[");
        // print stack
        for (Value *slot = vm.stack; slot < vm.stack_top; slot++) {
            printf(" ");
            print_value(*slot);
            printf(",");
        }
        printf(" ]");
        printf("\n");
        disassemble_instruction(vm.chunk, (int)(vm.ip - vm.chunk->code));
#endif /* ifdef DEBUG_TRACE_EXECUTION */

        uint8_t instruction;
        switch (instruction = READ_BYTE()) {
            case OP_CONSTANT: {
                Value constant = READ_CONSTANT();
                push(constant);
                break;
            }
            case OP_NIL:
                push(NIL_VALUE);
                break;
            case OP_TRUE:
                push(BOOL_VALUE(true));
                break;
            case OP_FALSE:
                push(BOOL_VALUE(false));
                break;
            case OP_EQUAL: {
                Value b = pop();
                Value a = pop();
                push(BOOL_VALUE(values_equal(a, b)));
                break;
            }
            case OP_GREATER:
                BINARY_OP(BOOL_VALUE, >);
                break;
            case OP_LESS:
                BINARY_OP(BOOL_VALUE, <);
                break;
            case OP_ADD: {
                if (IS_STRING(peek(0)) && IS_STRING(peek(1))) {
                    StringObject *s = concat_strings(AS_STRING(pop()),
                                                     AS_STRING(pop()));
                    push(OBJECT_VALUE(s));
                } else if (IS_NUMBER(peek(0)) && IS_NUMBER(peek(1))) {
                    double a = AS_NUMBER(pop());
                    double b = AS_NUMBER(pop());
                    push(NUMBER_VALUE(a + b));
                } else {
                    runtime_error("Operands must be two numbers or strings.");
                    return INTERPRET_RUNTIME_ERROR;
                }
                break;
            }
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
                push(BOOL_VALUE(is_falsey(pop())));
                break;
            case OP_NEGATE: {
                if (!IS_NUMBER(peek(0))) {
                    runtime_error("Operand must be a number.");
                    return INTERPRET_RUNTIME_ERROR;
                }
                push(NUMBER_VALUE(-AS_NUMBER(pop())));
                break;
            }
            case OP_RETURN: {
                print_value(pop());
                printf("\n");
                return INTERPRET_OK;
            }
        }
    }
#undef READ_BYTE
#undef READ_CONSTANT
#undef BINARY_OP
}

InterpretResult interpret(const char *source)
{
    Chunk chunk;
    init_chunk(&chunk);

    if (!compile(source, &chunk)) {
        free_chunk(&chunk);
        return INTERPRET_COMPILE_ERROR;
    }

    vm.chunk = &chunk;
    vm.ip = vm.chunk->code;

    InterpretResult res = run();

    free_chunk(&chunk);
    return res;
}
