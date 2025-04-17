#ifndef CLOX_MEMORY_H
#define CLOX_MEMORY_H

#include "common.h"

// Grows capacity by a factor of 2.
#define GROW_CAPACITY(capacity) ((capacity) < 8 ? 8 : (capacity) * 2)

// Grow array given the new capacity
#define GROW_ARRAY(type, ptr, new_capacity)                                    \
    (type *)reallocate(ptr, sizeof(type) * (new_capacity))

#define FREE_ARRAY(type, ptr, old_capacity) (type *)reallocate(ptr, 0)

// Reallocates a pointer, given the old size and the new size.
void *reallocate(void *pointer, size_t new_size);

#endif //  CLOX_MEMORY_H
