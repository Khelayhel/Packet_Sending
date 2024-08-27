# Makefile for a File Transfer using UDP project with two main classes
# Define the Java compiler
JAVAC = javac
# Define the Java compiler flags
JFLAGS = -g
# Define the source directory
SRC_DIR = src
# Define the build directory
BUILD_DIR = bin
# Define the main classes
SENDER_MAIN = Run_Client
RECEIVER_MAIN = Run_Server
# Define source files
SOURCES = $(wildcard $(SRC_DIR)/*.java)
# Define class files
CLASSES = $(SOURCES:$(SRC_DIR)/%.java=$(BUILD_DIR)/%.class)
# Default target
all: $(CLASSES)
$(BUILD_DIR)/%.class: $(SRC_DIR)/%.java
@mkdir -p $(@D)
$(JAVAC) $(JFLAGS) -d $(BUILD_DIR) $<
receiver: all
java -cp $(BUILD_DIR) $(RECEIVER_MAIN) arg1 arg2
sender: all
java -cp $(BUILD_DIR) $(SENDER_MAIN)arg1 arg2 arg3 arg4
clean:
rm -rf $(BUILD_DIR)
.PHONY: all sender receiver clean
