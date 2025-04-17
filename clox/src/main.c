#include "chunk.h"
#include "debug.h"

int main(void)
{
    Chunk chunk;
    init_chunk(&chunk);
    int constant = add_constant(&chunk, 10.5);
    write_chunk(&chunk, OP_CONSTANT);
    write_chunk(&chunk, constant);
    write_chunk(&chunk, OP_RETURN);

    disassemble_chunk(&chunk, "Test chunk");
    free_chunk(&chunk);
    return 0;
}
