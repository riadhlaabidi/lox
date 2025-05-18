#include <stdio.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "vm.h"

static Object *allocate_object(size_t size, ObjectType type)
{
    Object *object = (Object *)reallocate(NULL, 0, size);
    object->type = type;
    object->next = vm.objects;
    vm.objects = object;
    return object;
}

StringObject *allocate_string_object(int length)
{
    StringObject *string = (StringObject *)allocate_object(
        sizeof(StringObject) + length + 1, STRING_OBJECT);
    string->length = length;
    return string;
}

StringObject *copy_string(const char *chars, int length)
{

    StringObject *string = allocate_string_object(length);
    memcpy(string->chars, chars, length);
    string->chars[length] = '\0';
    return string;
}

StringObject *concat_strings(StringObject *a, StringObject *b)
{
    int length = a->length + b->length;
    StringObject *result = allocate_string_object(length);

    memcpy(result->chars, a->chars, a->length);
    memcpy(result->chars + a->length, b->chars, b->length);
    result->chars[length] = '\0';

    return result;
}

void print_object(Value value)
{
    switch (OBJECT_TYPE(value)) {
        case STRING_OBJECT:
            printf("%s", AS_CSTRING(value));
            break;
    }
}
