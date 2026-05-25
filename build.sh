#!/bin/bash

echo "Building Flossk TTS..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Build successful!"
    echo ""
    echo "JAR file location: target/flossk-tts.jar"
    echo ""
    ls -lh target/flossk-tts.jar
    echo ""
    echo "To run: java -jar target/flossk-tts.jar --spring.config.location=file:./application.properties"
else
    echo ""
    echo "✗ Build failed!"
    exit 1
fi
