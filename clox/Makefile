SHELL = /bin/sh
CC = gcc
CFLAGS = -Wall -Wextra -std=c99 -pedantic -ggdb

SRC_DIR := src
BUILD_DIR = build
TARGET = $(BUILD_DIR)/lox

SOURCES := $(wildcard $(SRC_DIR)/*.c)
OBJECTS := $(patsubst $(SRC_DIR)/%.c, $(BUILD_DIR)/%.o, $(SOURCES))

all: $(TARGET)

$(TARGET): $(OBJECTS)
	$(CC) $(OBJECTS) -o $@

$(BUILD_DIR)/%.o: $(SRC_DIR)/%.c | $(BUILD_DIR)
	$(CC) $(CFLAGS) -c $< -o $@

$(BUILD_DIR):
	mkdir -p $(BUILD_DIR)

clean:
	rm -rf $(BUILD_DIR)

run:
	@echo "*******************"
	@echo "* Lox Interpreter *"
	@echo "*******************"
	@echo
	@./$(TARGET)

.PHONY: all clean run




