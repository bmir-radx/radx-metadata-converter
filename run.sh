#!/bin/bash

# RADx Metadata Converter - Command Line Application
# Usage: ./run.sh [arguments]

JAR_FILE="target/radx-metadata-converter-0.0.1-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found. Building the application..."
    ./mvnw clean package
fi

echo "Running RADx Metadata Converter..."
java -jar "$JAR_FILE" "$@"
