#include "chunk.h"
#include "debug.h"

int main(void)
{
    Chunk chunk;
    init_chunk(&chunk);
    for (int i = 0; i <= 10; i++) {
        write_constant(&chunk, 20.0, i + 1);
    }
    write_chunk(&chunk, OP_RETURN, 8);

    disassemble_chunk(&chunk, "Test chunk");
    free_chunk(&chunk);
    return 0;
}
