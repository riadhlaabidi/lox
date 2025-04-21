#include <stdlib.h>

#include "memory.h"

void *reallocate(void *ptr, size_t new_size)
{
    if (new_size == 0) {
        free(ptr);
        return NULL;
    }

    void *new_ptr = realloc(ptr, new_size);

    if (new_ptr == NULL)
        exit(1);

    return new_ptr;
}
