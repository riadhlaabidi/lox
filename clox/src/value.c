#include <stdio.h>

#include "memory.h"
#include "value.h"

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

void print_value(Value value) { printf("%g", value); }
