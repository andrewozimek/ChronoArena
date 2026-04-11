#!/bin/bash

# Directories
SRC_CLIENT="src/client"
SRC_COMMON="src/common"
OUT_DIR="out"
JAR_NAME="client.jar"
MAIN_CLASS="client.ClientMain"

# Clean and create output dir
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# Compile
javac -d "$OUT_DIR" \
  $(find "$SRC_CLIENT" "$SRC_COMMON" -name "*.java")

# Copy resources
cp -r "$SRC_CLIENT/resources" "$OUT_DIR/"

# Create manifest
echo "Main-Class: $MAIN_CLASS" > manifest.txt

# Package jar
jar cfm "$JAR_NAME" manifest.txt -C "$OUT_DIR" .

# Cleanup
rm manifest.txt

echo "Built $JAR_NAME successfully"

# Launch
java -jar "$JAR_NAME"