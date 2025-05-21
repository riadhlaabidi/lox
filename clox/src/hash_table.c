#include "hash_table.h"
#include "memory.h"

#define HT_MAX_LOAD 0.75

void HT_init(HashTable *ht)
{
    ht->count = 0;
    ht->capacity = 0;
    ht->entries = NULL;
}

static HT_entry *find_entry(HT_entry *entries, int capacity, StringObject *key)
{
    uint32_t index = key->hash % capacity;

    while (1) {
        HT_entry *entry = &entries[index];
        if (entry->key == key || entry->key == NULL) {
            return entry;
        }
        index = (index + 1) % capacity;
    }
}

static void adjust_capacity(HashTable *ht, int capacity)
{
    HT_entry *entries = ALLOCATE(HT_entry, capacity);

    for (int i = 0; i < capacity; i++) {
        entries[i].key = NULL;
        entries[i].value = NIL_VALUE;
    }

    // Adjust the old entries to the new allocated capacity
    for (int i = 0; i < ht->capacity; i++) {
        HT_entry *old_entry = &ht->entries[i];

        if (old_entry->key == NULL) {
            continue;
        }

        HT_entry *adjusted_entry = find_entry(entries, capacity,
                                              old_entry->key);
        adjusted_entry->key = old_entry->key;
        adjusted_entry->value = old_entry->value;
    }

    FREE_ARRAY(HT_entry, ht->entries, ht->capacity);

    ht->entries = entries;
    ht->capacity = capacity;
}

int HT_set(HashTable *ht, StringObject *key, Value value)
{
    if (ht->count + 1 > ht->capacity * HT_MAX_LOAD) {
        int capacity = GROW_CAPACITY(ht->capacity);
        adjust_capacity(ht, capacity);
    }
    HT_entry *entry = find_entry(ht->entries, ht->capacity, key);
    int is_new_key = entry->key == NULL;
    if (is_new_key) {
        ht->count++;
    }
    entry->key = key;
    entry->value = value;
    return is_new_key;
}

void HT_add_all(HashTable *src, HashTable *dest)
{
    for (int i = 0; i < src->capacity; i++) {
        HT_entry *entry = &src->entries[i];
        if (entry->key != NULL) {
            HT_set(dest, entry->key, entry->value);
        }
    }
}

void HT_free(HashTable *ht)
{
    FREE_ARRAY(HT_entry, ht->entries, ht->capacity);
    HT_init(ht);
}
