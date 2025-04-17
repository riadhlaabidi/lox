#ifndef CLOX_VALUE_H
#define CLOX_VALUE_H

#include "common.h"

typedef double Value;

typedef struct {
    int capacity;
    int count;
    Value *values;
} ValueArray;

void init_value_array(ValueArray *arr);
void write_value_array(ValueArray *arr, Value value);
void free_value_array(ValueArray *arr);
void print_value(Value value);

#endif /* end of include guard: CLOX_VALUE_H */
