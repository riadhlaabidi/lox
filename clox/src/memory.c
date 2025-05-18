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
            StringObject *string = (StringObject *)object;
            // free string object along with its contiguous characters array
            reallocate(object, sizeof(StringObject) + string->length + 1, 0);
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
