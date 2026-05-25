# Building the JAR File

## Prerequisites

- Java 17 or higher
- Maven 3.6+ (or use Maven Wrapper)

## Build Commands

### Standard Build (Development)

```bash
mvn clean package
```

This creates a JAR file in `target/tts-0.0.1-SNAPSHOT.jar`

### Build Without Tests (Faster)

```bash
mvn clean package -DskipTests
```

### Build for Production (Optimized)

```bash
mvn clean package -DskipTests -Pproduction
```

Or with explicit optimization:

```bash
mvn clean package -DskipTests -Dspring-boot.repackage.excludeDevtools=true
```

## Output Location

After building, the JAR file will be located at:
```
target/tts-0.0.1-SNAPSHOT.jar
```

## Rename for Distribution

You can rename it to something more user-friendly:

```bash
cp target/tts-0.0.1-SNAPSHOT.jar flossk-tts.jar
```

## Verify the JAR

Check that the JAR was built correctly:

```bash
java -jar target/tts-0.0.1-SNAPSHOT.jar --version
```

Or test run it:

```bash
java -jar target/tts-0.0.1-SNAPSHOT.jar
```

## Build Script Example

Create a `build.sh` script:

```bash
#!/bin/bash
echo "Building Flossk TTS..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "Build successful!"
    cp target/tts-0.0.1-SNAPSHOT.jar flossk-tts.jar
    echo "JAR file created: flossk-tts.jar"
    ls -lh flossk-tts.jar
else
    echo "Build failed!"
    exit 1
fi
```

Make it executable:
```bash
chmod +x build.sh
./build.sh
```

## Windows Build Script

Create `build.bat`:

```batch
@echo off
echo Building Flossk TTS...
call mvn clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo Build successful!
    copy target\tts-0.0.1-SNAPSHOT.jar flossk-tts.jar
    echo JAR file created: flossk-tts.jar
) else (
    echo Build failed!
    exit /b 1
)
```

## Distribution Package

Create a distribution package with the JAR and configuration:

```bash
mkdir -p dist
cp target/tts-0.0.1-SNAPSHOT.jar dist/flossk-tts.jar
cp application.properties dist/application.properties.example
cp PRODUCTION_SETUP.md dist/README.md
tar -czf flossk-tts-dist.tar.gz dist/
```

## Troubleshooting

### Out of Memory During Build

```bash
export MAVEN_OPTS="-Xmx1024m"
mvn clean package
```

### Skip Tests

```bash
mvn clean package -DskipTests
```

### Clean Build (Remove Old Builds)

```bash
mvn clean
mvn package
```
