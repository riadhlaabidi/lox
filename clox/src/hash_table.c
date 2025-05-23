#include <string.h>

#include "hash_table.h"
#include "memory.h"
#include "value.h"

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
    HT_entry *tombstone = NULL;

    while (1) {
        HT_entry *entry = &entries[index];

        if (entry->key == NULL) {
            // might be empty or a tombstone
            if (IS_NIL(entry->value)) {
                // actual empty entry
                return tombstone != NULL ? tombstone : entry;
            } else {
                // a tombstone
                if (tombstone == NULL) {
                    tombstone = entry;
                }
            }
        } else if (entry->key == key) {
            // key found
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

    ht->count = 0;
    // Adjust the old entries to the new allocated capacity
    for (int i = 0; i < ht->capacity; i++) {
        HT_entry *old_entry = &ht->entries[i];

        if (old_entry->key == NULL) {
            // skip if tombstone or empty entry
            continue;
        }

        HT_entry *adjusted_entry = find_entry(entries, capacity,
                                              old_entry->key);
        adjusted_entry->key = old_entry->key;
        adjusted_entry->value = old_entry->value;
        ht->count++;
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
    if (is_new_key && IS_NIL(entry->value)) {
        ht->count++;
    }

    entry->key = key;
    entry->value = value;
    return is_new_key;
}

int HT_get(HashTable *ht, StringObject *key, Value *value)
{
    if (ht->count == 0) {
        return 0;
    }

    HT_entry *entry = find_entry(ht->entries, ht->capacity, key);
    if (entry->key == NULL) {
        return 0;
    }

    *value = entry->value;
    return 1;
}

int HT_delete(HashTable *ht, StringObject *key)
{
    if (ht->count == 0) {
        return 0;
    }

    HT_entry *entry = find_entry(ht->entries, ht->capacity, key);
    if (entry->key == NULL) {
        return 0;
    }

    entry->key = NULL;
    entry->value = BOOL_VALUE(1);
    return 1;
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

StringObject *HT_find_interned_string(HashTable *ht, const char *chars,
                                      int length, uint32_t hash)
{
    if (ht->count == 0) {
        return NULL;
    }

    uint32_t index = hash % ht->capacity;

    while (1) {
        HT_entry *entry = &ht->entries[index];

        if (entry->key == NULL) {
            // emtpy
            if (IS_NIL(entry->value)) {
                // not a tombstone -> not found
                return NULL;
            }
        } else if (entry->key->length == length && entry->key->hash == hash &&
                   memcmp(entry->key->chars, chars, length) == 0) {
            // found
            return entry->key;
        }

        index = (index + 1) % ht->capacity;
    }
}
