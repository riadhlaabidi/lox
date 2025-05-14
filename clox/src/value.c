#include <stdio.h>
#include <string.h>

#include "memory.h"
#include "object.h"

void init_value_array(ValueArray *arr)
{
    arr->count = 0;
    arr->capacity = 0;
    arr->values = NULL;
}

void write_value_array(ValueArray *arr, Value value)
{
    if (arr->capacity < arr->count + 1) {
        int old_capacity = arr->capacity;
        arr->capacity = GROW_CAPACITY(old_capacity);
        arr->values = GROW_ARRAY(Value, arr->values, old_capacity,
                                 arr->capacity);
    }

    arr->values[arr->count] = value;
    arr->count++;
}

void free_value_array(ValueArray *arr)
{
    FREE_ARRAY(Value, arr->values, arr->capacity);
    init_value_array(arr);
}

void print_value(Value value)
{
    switch (value.type) {
        case VAL_NIL:
            printf("nil");
            break;
        case VAL_BOOL:
            printf(AS_BOOL(value) ? "true" : "false");
            break;
        case VAL_NUMBER:
            printf("%g", AS_NUMBER(value));
            break;
        case VAL_OBJECT:
            print_object(value);
            break;
        default:
            assert(0 && "Unreachable code");
    }
}

int values_equal(Value a, Value b)
{
    if (a.type != b.type) {
        return 0;
    }

    switch (a.type) {
        case VAL_NUMBER:
            return AS_NUMBER(a) == AS_NUMBER(b);
        case VAL_BOOL:
            return AS_BOOL(a) == AS_BOOL(b);
        case VAL_NIL:
            return 1;
        case VAL_OBJECT: {
            StringObject *string_a = AS_STRING(a);
            StringObject *string_b = AS_STRING(b);
            return string_a->length == string_b->length &&
                   memcmp(string_a->chars, string_b->chars, string_a->length) ==
                       0;
        }
        default:
            assert(0 && "Unreachable code");
    }
}
