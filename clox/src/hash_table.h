#ifndef CLOX_HASH_TABLE_H
#define CLOX_HASH_TABLE_H

#include "common.h"
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
int HT_get(HashTable *ht, StringObject *key, Value *value);
int HT_delete(HashTable *ht, StringObject *key);
void HT_add_all(HashTable *src, HashTable *dest);
void HT_free(HashTable *ht);

StringObject *HT_find_interned_string(HashTable *ht, const char *chars,
                                      int length, uint32_t hash);

#endif /* end of include guard: CLOX_HASH_TABLE_H */
