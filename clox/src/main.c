#include "chunk.h"
#include "debug.h"
#include "vm.h"

int main(void)
{
    VM vm;
    init_VM(&vm);

    Chunk chunk;
    init_chunk(&chunk);

    write_constant(&chunk, 300.5, 123);
    write_chunk(&chunk, OP_RETURN, 123);

    disassemble_chunk(&chunk, "Test chunk");
    interpret(&vm, &chunk);
    free_VM(&vm);
    free_chunk(&chunk);

    return 0;
}
