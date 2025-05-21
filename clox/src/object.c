#include <stdio.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "vm.h"

#define ALLOCATE_OBJECT(type, object_type)                                     \
    (type *)allocate_object(sizeof(type), object_type)

static Object *allocate_object(size_t size, ObjectType type)
{
    Object *object = (Object *)reallocate(NULL, 0, size);
    object->type = type;
    object->next = vm.objects;
    vm.objects = object;
    return object;
}

static uint32_t hash_string(const char *key, int length)
{
    uint32_t hash = 2166136261u;
    for (int i = 0; i < length; i++) {
        hash ^= (uint8_t)key[i];
        hash *= 16777619;
    }
    return hash;
}

StringObject *allocate_string_object(char *chars, int length, uint32_t hash)
{
    StringObject *string = ALLOCATE_OBJECT(StringObject, STRING_OBJECT);
    string->chars = chars;
    string->length = length;
    string->hash = hash;
    return string;
}

StringObject *copy_string(const char *chars, int length)
{
    uint32_t hash = hash_string(chars, length);
    char *heap_chars = ALLOCATE(char, length + 1);
    memcpy(heap_chars, chars, length);
    heap_chars[length] = '\0';

    return allocate_string_object(heap_chars, length, hash);
}

StringObject *concat_strings(StringObject *a, StringObject *b)
{
    int length = a->length + b->length;
    char *concatenated = ALLOCATE(char, length + 1);
    memcpy(concatenated, a->chars, a->length);
    memcpy(concatenated + a->length, b->chars, b->length);
    concatenated[length] = '\0';

    uint32_t hash = hash_string(concatenated, length);
    return allocate_string_object(concatenated, length, hash);
}

void print_object(Value value)
{
    switch (OBJECT_TYPE(value)) {
        case STRING_OBJECT:
            printf("%s", AS_CSTRING(value));
            break;
    }
}
