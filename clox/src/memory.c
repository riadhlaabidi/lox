#include <stdio.h>
#include <stdlib.h>

#include "memory.h"

void *reallocate(void *ptr, size_t old_size, size_t new_size)
{
    (void)old_size; // NOTE: supress unused parameter

    if (new_size == 0) {
        free(ptr);
        return NULL;
    }

    void *new_ptr = realloc(ptr, new_size);

    if (new_ptr == NULL) {
        fprintf(stderr, "Failed to allocate memory\n");
        exit(1);
    }

    return new_ptr;
}

static void free_object(Object *object)
{
    switch (object->type) {
        case STRING_OBJECT: {
            StringObject *s = (StringObject *)object;
            FREE_ARRAY(char, s->chars, s->length + 1);
            FREE(StringObject, object);
            break;
        }
    }
}

void free_objects()
{
    Object *object = vm.objects;
    while (object != NULL) {
        Object *next = object->next;
        free_object(object);
        object = next;
    }
}
