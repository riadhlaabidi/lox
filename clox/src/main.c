#include "chunk.h"
#include "debug.h"

int main(void)
{
    Chunk chunk;
    init_chunk(&chunk);

    write_constant(&chunk, 300.5, 1);
    write_constant(&chunk, 20.0, 2);
    write_chunk(&chunk, OP_RETURN, 2);

    disassemble_chunk(&chunk, "Test chunk");
    free_chunk(&chunk);
    return 0;
}
