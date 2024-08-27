Java Compiler
JAVAC = javac

Java Virtual Machine
JAVA = java

Compiler Flags
JAVACFLAGS = -d bin

Source and Output Directories
SRC_DIR = src
BIN_DIR = bin

Main Class
MAIN_CLASS = network_project.Run_Client

Source Files
SOURCES = $(wildcard $(SRC_DIR)/*.java)

Class Files
CLASSES = $(SOURCES:$(SRC_DIR)/%.java=$(BIN_DIR)/%.class)

.PHONY: all clean run

all: $(MAIN_CLASS)

$(MAIN_CLASS): $(CLASSES)
    @echo "Creating Main Executable..."
    @$(JAVA) -classpath $(BIN_DIR) $(MAIN_CLASS)

$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
    @mkdir -p $(@D)
    @echo "Compiling $<..."
    @$(JAVAC) $(JAVACFLAGS) $<

clean:
    @echo "Cleaning..."
    @rm -rf $(BIN_DIR)

run: all

.PHONY: run_Client run_Server

run_Client:
    $(JAVA) -classpath $(BIN_DIR) network_project.Run_Client

run_Server:
    $(JAVA) -classpath $(BIN_DIR) network_project.Run_Server