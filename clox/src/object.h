#ifndef CLOX_OBJECT_H
#define CLOX_OBJECT_H

#include "value.h"
#include "vm.h"

#define OBJECT_TYPE(value) (AS_OBJECT(value)->type)
#define IS_STRING(value) is_object_of_type(value, STRING_OBJECT)

#define AS_STRING(value) ((StringObject *)AS_OBJECT(value))
#define AS_CSTRING(value) (((StringObject *)AS_OBJECT(value))->chars)

typedef enum {
    STRING_OBJECT,
} ObjectType;

struct Object {
    ObjectType type;
    struct Object *next;
};

struct StringObject {
    Object object;
    int length;
    char *chars;
    uint32_t hash;
};

StringObject *copy_string(const char *chars, int length);
StringObject *concat_strings(StringObject *a, StringObject *b);
void print_object(Value value);

static inline int is_object_of_type(Value value, ObjectType type) {
    return IS_OBJECT(value) && AS_OBJECT(value)->type == type;
}

#endif /* end of include guard: CLOX_OBJECT_H */
