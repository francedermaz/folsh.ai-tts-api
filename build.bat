@echo off
echo Building Flossk TTS...
call mvn clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful!
    echo.
    echo JAR file location: target\flossk-tts.jar
    echo.
    dir target\flossk-tts.jar
    echo.
    echo To run: java -jar target\flossk-tts.jar --spring.config.location=file:./application.properties
) else (
    echo.
    echo Build failed!
    exit /b 1
)
