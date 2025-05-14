#ifndef CLOX_MEMORY_H
#define CLOX_MEMORY_H

#include "common.h"
#include "object.h"
#include "vm.h"

#define ALLOCATE(type, count)                                                  \
    (type *)reallocate(NULL, 0, sizeof(type) * (count))

// Grows capacity by a factor of 2.
#define GROW_CAPACITY(capacity) ((capacity) < 8 ? 8 : (capacity) * 2)

// Grow array given the new capacity
#define GROW_ARRAY(type, ptr, old_capacity, new_capacity)                      \
    (type *)reallocate(ptr, sizeof(type) * (old_capacity),                     \
                       sizeof(type) * (new_capacity))

#define FREE_ARRAY(type, ptr, old_capacity)                                    \
    reallocate(ptr, sizeof(type) * (old_capacity), 0)

#define FREE(type, ptr) reallocate(ptr, sizeof(type), 0)

// Reallocates a pointer, given the old size and the new size.
void *reallocate(void *pointer, size_t old_size, size_t new_size);

void free_objects();

#endif //  CLOX_MEMORY_H
