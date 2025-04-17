#include "chunk.h"
#include <stdio.h>

int main(int argc, char **argv)
{
    Chunk chunk;
    init_chunk(&chunk);
    write_chunk(&chunk, OP_RETURN);
    printf("Chunk %04d %d\n", chunk.count - 1, chunk.code[chunk.count - 1]);
    free_chunk(&chunk);
    return 0;
}
