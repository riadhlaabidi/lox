#include "chunk.h"
#include "debug.h"

int main(void)
{
    Chunk chunk;
    init_chunk(&chunk);
    int constant = add_constant(&chunk, 10.5);
    write_chunk(&chunk, OP_CONSTANT, 189);
    write_chunk(&chunk, constant, 189);
    write_chunk(&chunk, OP_RETURN, 189);

    disassemble_chunk(&chunk, "Test chunk");
    free_chunk(&chunk);
    return 0;
}
