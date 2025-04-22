#include "chunk.h"
#include "debug.h"
#include "vm.h"

int main(void)
{
    VM vm;
    init_VM(&vm);

    Chunk chunk;
    init_chunk(&chunk);

    // expression: -((5.8 + 22.2) * 2.5)
    write_constant(&chunk, 5.8, 123);
    write_constant(&chunk, 22.2, 123);
    write_chunk(&chunk, OP_ADD, 123);
    write_constant(&chunk, 2.5, 123);
    write_chunk(&chunk, OP_MULTIPLY, 123);
    write_chunk(&chunk, OP_NEGATE, 123);
    write_chunk(&chunk, OP_RETURN, 123); // -70

    disassemble_chunk(&chunk, "Test chunk");
    interpret(&vm, &chunk);
    free_VM(&vm);
    free_chunk(&chunk);

    return 0;
}
