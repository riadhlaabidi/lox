#ifndef CLOX_DEBUG_H
#define CLOX_DEBUG_H

#include "chunk.h"

/**
 * Disassembles all instructions in a chunk of bytecode and prints respective
 * operation code bytes.
 *
 * @param chunk The chunk of code to disassemble
 * @param name A name for the chunk to distinguish it while printing
 */
void disassemble_chunk(Chunk *chunk, const char *name);

/**
 * Disassembles a single instruction and prints its operation code byte.
 *
 * @param chunk The chunk of code where the instruction exists.
 * @param offset The byte offset of the instruction in the chunk.
 * @return Returns the next byte offset
 */
int disassemble_instruction(Chunk *chunk, int offset);

#endif /* end of include guard: CLOX_DEBUG_H */
