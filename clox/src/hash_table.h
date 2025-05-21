#ifndef CLOX_HASH_TABLE_H
#define CLOX_HASH_TABLE_H

#include "value.h"

typedef struct {
    StringObject *key;
    Value value;
} HT_entry;

typedef struct {
    int count;
    int capacity;
    HT_entry *entries;
} HashTable;

void HT_init(HashTable *ht);
int HT_set(HashTable *ht, StringObject *key, Value value);
void HT_add_all(HashTable *src, HashTable *dest);
void HT_free(HashTable *ht);

#endif /* end of include guard: CLOX_HASH_TABLE_H */
