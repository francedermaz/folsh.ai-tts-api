# Quick Start Guide

## Running the JAR with External Configuration

### Step 1: Copy the example properties file

```bash
# Copy the example to application.properties (must end with .properties)
cp application.properties.production-example application.properties
```

### Step 2: Edit application.properties

Open `application.properties` and update MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/flossk_tts
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

### Step 3: Run the JAR

```bash
java -jar flossk-tts.jar --spring.config.location=file:./application.properties
```

**Note:** The file must be named `application.properties` (not `application.properties.production-example`). Spring Boot only recognizes `.properties` extension.

## Alternative: Use Environment Variables

Instead of a properties file, you can use environment variables:

```bash
export MYSQL_URL="jdbc:mysql://localhost:3306/flossk_tts"
export MYSQL_USER="your_username"
export MYSQL_PASSWORD="your_password"
export MYSQL_DIALECT="org.hibernate.dialect.MySQLDialect"

java -jar flossk-tts.jar
```

## Development Mode (H2 File-based)

For local development without MySQL:

```bash
java -jar flossk-tts.jar --spring.profiles.active=dev
```

This uses H2 file-based database at `./data/ttsdb`.
