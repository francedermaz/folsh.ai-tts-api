# Production Setup Guide

## Database Options

**MySQL is strongly recommended for production** for better reliability, performance, and scalability.

**H2 file-based database is allowed** but NOT recommended for production due to:
- Limited concurrency support
- Potential data corruption under high load
- Limited scalability
- More complex backup/recovery

For **local development** with file-based H2 database, use the `dev` profile:
```bash
java -jar flossk-tts.jar --spring.profiles.active=dev
```

## Quick Start - External Properties File

When you have only the JAR file, you can provide configuration via an external properties file:

1. **Create `application.properties` file** (in the same directory as the JAR):
   ```bash
   # Copy the example file (must be named application.properties, not .production-example)
   cp application.properties.production-example application.properties
   # Or if you have application.properties.example:
   # cp application.properties.example application.properties
   ```
   
   **Important:** The file must be named `application.properties` (Spring Boot only recognizes `.properties` extension)

2. **Update MySQL database credentials** in `application.properties` (Recommended):
   ```properties
   spring.datasource.url=jdbc:mysql://your-host:3306/flossk_tts
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. **Create MySQL database:**
   ```sql
   CREATE DATABASE flossk_tts CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

4. **Run with external properties file:**
   ```bash
   java -jar flossk-tts.jar --spring.config.location=file:./application.properties
   ```
   
   Or use absolute path:
   ```bash
   java -jar flossk-tts.jar --spring.config.location=file:/absolute/path/to/application.properties
   ```
   
   **Important:** The file must be named `application.properties` (not `application.properties.production-example`). Copy the example file first:
   ```bash
   cp application.properties.production-example application.properties
   # Then edit application.properties with your MySQL credentials
   java -jar flossk-tts.jar --spring.config.location=file:./application.properties
   ```

5. **Alternative: Use additional location** (external file + defaults):
   ```bash
   java -jar flossk-tts.jar --spring.config.additional-location=file:./application.properties
   ```

## Development Mode (File-based H2)

For local development, you can use file-based H2 database:

```bash
java -jar flossk-tts.jar --spring.profiles.active=dev
```

This will:
- Use H2 file-based database (`./data/ttsdb`)
- Enable H2 console at `/h2-console`
- Show SQL queries in logs
- Auto-create/update database schema

## File Structure Example

```
/opt/flossk-tts/
├── flossk-tts.jar
├── application.properties    # Your configuration file
├── cache/                     # Cache directory (created automatically)
└── logs/                      # Logs directory (if configured)
```

## Using Environment Variables

You can also use environment variables instead of or in combination with the properties file:

```bash
export MYSQL_URL="jdbc:mysql://localhost:3306/flossk_tts"
export MYSQL_USER="your_username"
export MYSQL_PASSWORD="your_password"
export MYSQL_DRIVER="com.mysql.cj.jdbc.Driver"
export MYSQL_DIALECT="org.hibernate.dialect.MySQLDialect"

java -jar flossk-tts.jar --spring.config.location=file:./application.properties
```

Environment variables will override values in the properties file.

## Systemd Service Example

Create `/etc/systemd/system/flossk-tts.service`:

```ini
[Unit]
Description=Flossk TTS Service
After=network.target mysql.service

[Service]
Type=simple
User=flossk
WorkingDirectory=/opt/flossk-tts
ExecStart=/usr/bin/java -jar /opt/flossk-tts/flossk-tts.jar --spring.config.location=file:/opt/flossk-tts/application.properties
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

# Environment variables (optional, will override properties file)
Environment="MYSQL_URL=jdbc:mysql://localhost:3306/flossk_tts"
Environment="MYSQL_USER=flossk_user"
Environment="MYSQL_PASSWORD=your_secure_password"

[Install]
WantedBy=multi-user.target
```

Then:
```bash
sudo systemctl daemon-reload
sudo systemctl enable flossk-tts
sudo systemctl start flossk-tts
sudo systemctl status flossk-tts
```

## Docker Example

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: flossk_tts
      MYSQL_USER: flossk_user
      MYSQL_PASSWORD: flossk_password
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"

  flossk-tts:
    build: .
    ports:
      - "8080:8080"
    environment:
      MYSQL_URL: jdbc:mysql://mysql:3306/flossk_tts
      MYSQL_USER: flossk_user
      MYSQL_PASSWORD: flossk_password
      MYSQL_DRIVER: com.mysql.cj.jdbc.Driver
      MYSQL_DIALECT: org.hibernate.dialect.MySQLDialect
    volumes:
      - ./cache:/app/cache
    depends_on:
      - mysql

volumes:
  mysql_data:
```

## Important Production Considerations

1. **Database Backup:** Set up regular backups of your MySQL database
2. **Cache Cleanup:** The cache cleanup job runs daily, but monitor disk space
3. **Log Rotation:** Configure log rotation if using file logging
4. **SSL/TLS:** Use a reverse proxy (nginx/Apache) with SSL certificates
5. **Firewall:** Only expose necessary ports (typically 8080 or 443)
6. **Monitoring:** Set up monitoring for disk space, memory, and database connections
7. **Admin Password:** Change the default admin password immediately after first login

## Reverse Proxy Example (Nginx)

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
