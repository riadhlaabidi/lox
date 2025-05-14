#ifndef CLOX_VALUE_H
#define CLOX_VALUE_H

typedef struct Object Object;
typedef struct StringObject StringObject;

typedef enum {
    VAL_BOOL,
    VAL_NIL,
    VAL_NUMBER,
    VAL_OBJECT,
} ValueType;

typedef struct {
    ValueType type;
    union {
        int boolean;
        double number;
        Object *object;
    } as;
} Value;

#define IS_BOOL(value) ((value).type == VAL_BOOL)
#define IS_NIL(value) ((value).type == VAL_NIL)
#define IS_NUMBER(value) ((value).type == VAL_NUMBER)
#define IS_OBJECT(value) ((value).type == VAL_OBJECT)

#define AS_BOOL(value) ((value).as.boolean)
#define AS_NUMBER(value) ((value).as.number)
#define AS_OBJECT(value) ((value).as.object)

#define BOOL_VALUE(value) ((Value){VAL_BOOL, {.boolean = value}})
#define NIL_VALUE ((Value){VAL_NIL, {.number = 0}})
#define NUMBER_VALUE(value) ((Value){VAL_NUMBER, {.number = value}})
#define OBJECT_VALUE(value) ((Value){VAL_OBJECT, {.object = (Object *)value}})

typedef struct {
    int capacity;
    int count;
    Value *values;
} ValueArray;

void init_value_array(ValueArray *arr);
void write_value_array(ValueArray *arr, Value value);
void free_value_array(ValueArray *arr);
void print_value(Value value);
int values_equal(Value a, Value b);

#endif /* end of include guard: CLOX_VALUE_H */
