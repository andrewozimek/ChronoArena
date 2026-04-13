#!/bin/bash

# Directories
SRC_SERVER="src/server"
SRC_COMMON="src/common"
OUT_DIR="server_out"
JAR_NAME="server.jar"
MAIN_CLASS="server.ServerMain"

# Clean and create output dir
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# Compile
javac -d "$OUT_DIR" \
  $(find "$SRC_SERVER" "$SRC_COMMON" -name "*.java")

# Copy resources
cp -r "$SRC_SERVER/resources" "$OUT_DIR/"

# Create manifest
echo "Main-Class: $MAIN_CLASS" > manifest.txt

# Package jar
jar cfm "$JAR_NAME" manifest.txt -C "$OUT_DIR" .

# Cleanup
rm manifest.txt

echo "Built $JAR_NAME successfully"

# Launch
java -jar "$JAR_NAME"