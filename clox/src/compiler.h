#ifndef CLOX_COMPILER_H
#define CLOX_COMPILER_H

#include "vm.h"

int compile(const char *source, Chunk *chunk);

#endif /* end of include guard: CLOX_COMPILER_H */
