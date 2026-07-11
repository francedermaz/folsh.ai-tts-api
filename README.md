# Flossk TTS - Text-to-Speech API Server

A Spring Boot-based Text-to-Speech (TTS) API server that converts text to speech audio using Piper TTS engine. Features API key management, token-based usage limits, iframe embedding, and a comprehensive admin dashboard.

## Features

- 🎙️ **Text-to-Speech Conversion**: Convert text to high-quality WAV audio files
- 🔐 **API Key Authentication**: Secure API access with API key management
- 📊 **Token Management**: Flexible token limit system (unlimited, total, monthly, yearly, one-time)
- 🎨 **Admin Dashboard**: Web-based interface for managing API keys and monitoring usage
- 🔗 **Iframe Embedding**: Generate embeddable iframe players for websites
- 📈 **Usage Tracking**: Detailed usage history and statistics per API key
- 💾 **Database Support**: MySQL (production) and H2 (development)
- 📝 **API Documentation**: Swagger/OpenAPI documentation included

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+** (for building)
- **MySQL 8+** (for production) or H2 (for development)

## Installation

### Step 1: Install Java SDK

#### Ubuntu/Debian

```bash
# Update package list
sudo apt update

# Install OpenJDK 17
sudo apt install openjdk-17-jdk

# Verify installation
java -version
javac -version
```

#### CentOS/RHEL/Fedora

```bash
# Install OpenJDK 17
sudo dnf install java-17-openjdk-devel

# Or for older versions
sudo yum install java-17-openjdk-devel

# Verify installation
java -version
javac -version
```

#### macOS

```bash
# Using Homebrew
brew install openjdk@17

# Link it
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Verify installation
java -version
```

#### Windows

1. Download OpenJDK 17 from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/#java17)
2. Run the installer
3. Set `JAVA_HOME` environment variable:
   - Open System Properties → Environment Variables
   - Add `JAVA_HOME` pointing to JDK installation (e.g., `C:\Program Files\Java\jdk-17`)
   - Add `%JAVA_HOME%\bin` to `PATH`
4. Verify installation:
   ```cmd
   java -version
   javac -version
   ```

### Step 2: Install Maven (if not already installed)

#### Ubuntu/Debian

```bash
sudo apt install maven
```

#### CentOS/RHEL/Fedora

```bash
sudo dnf install maven
# Or for older versions
sudo yum install maven
```

#### macOS

```bash
brew install maven
```

#### Windows

1. Download Maven from [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract to a directory (e.g., `C:\Program Files\Apache\maven`)
3. Add `MAVEN_HOME` environment variable pointing to Maven directory
4. Add `%MAVEN_HOME%\bin` to `PATH`

### Step 3: Create a Dedicated User (Recommended for Production)

Creating a dedicated user improves security and makes process management easier.

#### Linux/macOS

```bash
# Create a new user (without home directory)
sudo useradd -r -s /bin/false flossktts

# Or with home directory (if needed)
sudo useradd -m -s /bin/bash flossktts

# Create application directory
sudo mkdir -p /opt/flossk-tts
sudo chown flossktts:flossktts /opt/flossk-tts

# Create cache directory
sudo mkdir -p /var/cache/flossk-tts
sudo chown flossktts:flossktts /var/cache/flossk-tts

# Create log directory
sudo mkdir -p /var/log/flossk-tts
sudo chown flossktts:flossktts /var/log/flossk-tts
```

#### Windows

1. Open Computer Management → Local Users and Groups → Users
2. Right-click → New User
3. Create user `flossktts` (uncheck "User must change password at next logon")
4. Create directories:
   ```cmd
   mkdir C:\flossk-tts
   mkdir C:\flossk-tts\cache
   mkdir C:\flossk-tts\logs
   ```
5. Set permissions for the user on these directories

## Building the Application

### Standard Build

```bash
mvn clean package
```

### Build Without Tests (Faster)

```bash
mvn clean package -DskipTests
```

The JAR file will be created at: `target/flossk-tts.jar`

## Configuration

### Option 1: External Properties File (Recommended for Production)

1. Copy the example properties file:
```bash
cp application.properties.production-example application.properties
```

2. Edit `application.properties` and configure:
   - MySQL database connection
   - Cache directory
   - Server port (default: 8080)
   - Logging settings

Example configuration:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tts
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
tts.cache.directory=./cache
server.port=8080
```

### Option 2: Environment Variables

```bash
export MYSQL_URL="jdbc:mysql://localhost:3306/tts"
export MYSQL_USER="your_username"
export MYSQL_PASSWORD="your_password"
export MYSQL_DIALECT="org.hibernate.dialect.MySQLDialect"
```

### Option 3: Development Mode (H2 Database)

For local development without MySQL, use the `dev` profile:
```bash
java -jar flossk-tts.jar --spring.profiles.active=dev
```

This uses an H2 file-based database at `./data/ttsdb`.

## Running the Application

### Development Mode (Foreground)

#### With External Properties File

```bash
java -jar flossk-tts.jar --spring.config.location=file:./application.properties
```

#### With Environment Variables

```bash
java -jar flossk-tts.jar
```

#### Development Mode (H2 Database)

```bash
java -jar flossk-tts.jar --spring.profiles.active=dev
```

The application will start on `http://localhost:8080` (or your configured port).

### Production Mode (Background)

#### Option 1: Using nohup (Simple)

```bash
# Navigate to application directory
cd /opt/flossk-tts

# Run in background with nohup
nohup java -jar flossk-tts.jar --spring.config.location=file:./application.properties > /var/log/flossk-tts/app.log 2>&1 &

# Save the process ID
echo $! > /var/run/flossk-tts.pid

# Check if running
ps aux | grep flossk-tts

# View logs
tail -f /var/log/flossk-tts/app.log
```

**Stopping the application:**
```bash
# Find and kill the process
kill $(cat /var/run/flossk-tts.pid)

# Or find by name
pkill -f flossk-tts.jar
```

#### Option 2: Using systemd (Recommended for Linux)

Create a systemd service file:

```bash
sudo nano /etc/systemd/system/flossk-tts.service
```

Add the following content:

```ini
[Unit]
Description=Flossk TTS API Server
After=network.target mysql.service

[Service]
Type=simple
User=flossktts
Group=flossktts
WorkingDirectory=/opt/flossk-tts
ExecStart=/usr/bin/java -jar /opt/flossk-tts/flossk-tts.jar --spring.config.location=file:/opt/flossk-tts/application.properties
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=flossk-tts

# Security settings
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/cache/flossk-tts /var/log/flossk-tts

# Resource limits
LimitNOFILE=65536
LimitNPROC=4096

[Install]
WantedBy=multi-user.target
```

**Enable and start the service:**

```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable service to start on boot
sudo systemctl enable flossk-tts

# Start the service
sudo systemctl start flossk-tts

# Check status
sudo systemctl status flossk-tts

# View logs
sudo journalctl -u flossk-tts -f

# Stop the service
sudo systemctl stop flossk-tts

# Restart the service
sudo systemctl restart flossk-tts
```

#### Option 3: Using screen (Interactive Background)

```bash
# Start a new screen session
screen -S flossk-tts

# Run the application
java -jar flossk-tts.jar --spring.config.location=file:./application.properties

# Detach from screen: Press Ctrl+A, then D

# Reattach to screen session
screen -r flossk-tts

# List all screen sessions
screen -ls

# Kill a screen session
screen -X -S flossk-tts quit
```

#### Option 4: Using tmux (Alternative to screen)

```bash
# Start a new tmux session
tmux new -s flossk-tts

# Run the application
java -jar flossk-tts.jar --spring.config.location=file:./application.properties

# Detach from tmux: Press Ctrl+B, then D

# Reattach to tmux session
tmux attach -t flossk-tts

# List all tmux sessions
tmux ls

# Kill a tmux session
tmux kill-session -t flossk-tts
```

#### Option 5: Windows Service (Windows Server)

For Windows, use [NSSM (Non-Sucking Service Manager)](https://nssm.cc/):

```cmd
# Download and extract NSSM
# Run as Administrator

# Install service
nssm install FlosskTTS "C:\Program Files\Java\jdk-17\bin\java.exe"
nssm set FlosskTTS AppParameters "-jar C:\flossk-tts\flossk-tts.jar --spring.config.location=file:C:\flossk-tts\application.properties"
nssm set FlosskTTS AppDirectory "C:\flossk-tts"
nssm set FlosskTTS DisplayName "Flossk TTS API Server"
nssm set FlosskTTS Description "Flossk Text-to-Speech API Server"
nssm set FlosskTTS Start SERVICE_AUTO_START
nssm set FlosskTTS AppStdout "C:\flossk-tts\logs\output.log"
nssm set FlosskTTS AppStderr "C:\flossk-tts\logs\error.log"

# Start service
nssm start FlosskTTS

# Stop service
nssm stop FlosskTTS

# Remove service
nssm remove FlosskTTS confirm
```

### Running as Dedicated User

If you created a dedicated user (recommended), run the application as that user:

```bash
# Switch to the dedicated user
sudo su - flossktts

# Or run a command as that user
sudo -u flossktts java -jar /opt/flossk-tts/flossk-tts.jar --spring.config.location=file:/opt/flossk-tts/application.properties
```

**Note**: Ensure the dedicated user has:
- Read access to the JAR file and application.properties
- Write access to cache and log directories
- Network access (for database connections and API requests)

### JVM Memory Configuration

For production, configure JVM memory settings based on your server resources:

```bash
# Example: Allocate 2GB heap memory
java -Xms2g -Xmx2g -jar flossk-tts.jar --spring.config.location=file:./application.properties

# Example: With garbage collection options
java -Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -jar flossk-tts.jar --spring.config.location=file:./application.properties
```

**Recommended JVM Options for Production:**

```bash
java \
  -Xms2g \
  -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/flossk-tts/heap-dump.hprof \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -Xloggc:/var/log/flossk-tts/gc.log \
  -jar flossk-tts.jar \
  --spring.config.location=file:./application.properties
```

**Memory Guidelines:**
- **Minimum**: 1GB heap for small deployments (< 100 requests/hour)
- **Recommended**: 2-4GB heap for medium deployments (100-1000 requests/hour)
- **Large**: 4-8GB heap for high-traffic deployments (> 1000 requests/hour)

Update your systemd service file to include JVM options:

```ini
[Service]
ExecStart=/usr/bin/java \
  -Xms2g \
  -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar /opt/flossk-tts/flossk-tts.jar \
  --spring.config.location=file:/opt/flossk-tts/application.properties
```

### Monitoring and Health Checks

**Check Application Status:**

```bash
# Using systemd
sudo systemctl status flossk-tts

# Using curl (health endpoint)
curl http://localhost:8080/actuator/health

# Check if port is listening
netstat -tlnp | grep 8080
# Or
ss -tlnp | grep 8080

# Check process
ps aux | grep flossk-tts
```

**View Logs:**

```bash
# Systemd logs
sudo journalctl -u flossk-tts -f

# Application logs (if configured)
tail -f /var/log/flossk-tts/app.log

# Error logs only
sudo journalctl -u flossk-tts -p err
```

**Monitor Resource Usage:**

```bash
# CPU and memory usage
top -p $(pgrep -f flossk-tts.jar)

# Or use htop for better visualization
htop -p $(pgrep -f flossk-tts.jar)

# Disk usage (cache directory)
du -sh /var/cache/flossk-tts

# Database connections
mysql -u root -p -e "SHOW PROCESSLIST;"
```

## Usage

### Admin Dashboard

1. **Access the Admin Dashboard**: Navigate to `http://localhost:8080/admin`
2. **Login**: 
   - Default username: `floosk`
   - Default password: `flosskaadmin`
   - ⚠️ **Important**: Change the default password after first login!

3. **Create API Keys**:
   - Click "Create New API Key"
   - Enter owner name
   - Select token limit type:
     - **Unlimited**: No token restrictions
     - **Total**: One-time total token limit
     - **Monthly**: Tokens reset at the start of each month
     - **Yearly**: Tokens reset at the start of each year
     - **Once**: Single-use only (disabled after first use)
   - Set token limit value (if applicable)
   - Optionally set referer domain for iframe embeds

4. **Manage API Keys**:
   - View all API keys and their status
   - Edit token limits and referer domains
   - View usage history and statistics
   - Generate iframe embed codes

### API Usage

#### Generate Speech

**Endpoint**: `POST /api/tts/speak`

**Headers**:
```
X-API-KEY: your-api-key-here
Content-Type: application/json
```

**Request Body**:
```json
{
  "text": "Hello, this is a test message.",
  "voiceId": "edon"
}
```

**Available Voices**: `edon`, `arta`, `arben`, `dren`

**Response**: WAV audio file (binary)

**Example using cURL**:
```bash
curl -X POST http://localhost:8080/api/tts/speak \
  -H "X-API-KEY: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello world","voiceId":"edon"}' \
  --output speech.wav
```

**Example using JavaScript (Fetch API)**:
```javascript
const response = await fetch('http://localhost:8080/api/tts/speak', {
  method: 'POST',
  headers: {
    'X-API-KEY': 'your-api-key-here',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    text: 'Hello, this is a test message.',
    voiceId: 'edon'
  })
});

const audioBlob = await response.blob();
const audioUrl = URL.createObjectURL(audioBlob);
const audio = new Audio(audioUrl);
audio.play();
```

**Example with multiline text**:
```json
{
  "text": "This is line one.\nThis is line two.\nThis is line three.",
  "voiceId": "edon"
}
```

Note: Newlines in the text are automatically replaced with " ." (space and period) before processing.

### Iframe Embedding

1. **Generate Embed Code**:
   - Go to Admin Dashboard → Select API Key → Click "Iframe"
   - Copy the iframe code provided

2. **Use in HTML**:
```html
<!-- With pre-filled text -->
<iframe 
  src="http://your-server.com/embed/player?token=YOUR_TOKEN" 
  text_to_speech="Pershendetje, ky është zëri shqip." 
  width="500" 
  height="400" 
  frameborder="0">
</iframe>

<!-- Without text (user can enter text) -->
<iframe 
  src="http://your-server.com/embed/player?token=YOUR_TOKEN" 
  width="500" 
  height="400" 
  frameborder="0">
</iframe>
```

**Security Features**:
- API key is encoded in the token (not exposed in URL)
- Referer domain validation ensures iframe can only be embedded on authorized domains
- Token includes timestamp and signature for security

### API Documentation

Interactive API documentation is available at:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## Token System

### Token Counting

Tokens are counted as **words longer than 3 characters** in the input text. Punctuation is removed before counting.

Example:
- "Hello world" → 2 tokens ("Hello" and "world" both count)
- "This is a test" → 2 tokens ("This" and "test" both count)

### Token Limit Types

1. **Unlimited**: No restrictions on token usage
2. **Total**: One-time limit that decreases with each request
3. **Monthly**: Limit resets at the start of each month
4. **Yearly**: Limit resets at the start of each year
5. **Once**: Single-use only - API key is disabled after first successful request

### Usage Tracking

Each API key tracks:
- Total tokens used
- Total requests made
- Tokens used today
- Tokens used this month
- Remaining tokens (based on limit type)
- Detailed request history with timestamps, IP addresses, and user agents

## File Structure

```
flossk-tts/
├── src/
│   ├── main/
│   │   ├── java/com/flossk/tts/
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── service/         # Business logic
│   │   │   ├── entity/          # Database entities
│   │   │   ├── repository/      # Data access
│   │   │   ├── config/          # Configuration
│   │   │   └── filter/          # Security filters
│   │   └── resources/
│   │       ├── templates/       # Thymeleaf templates
│   │       └── voices/          # Voice model files (.onnx)
│   └── test/
├── application.properties.production-example
├── pom.xml
└── README.md
```

## Cache

Generated audio files are cached to improve performance. Cache directory is configurable via `tts.cache.directory` property (default: `./cache`).

Cache keys are based on:
- Text content (normalized)
- Voice ID
- Text hash (SHA-256)

## Security

- **API Authentication**: API keys required for `/api/**` endpoints
- **Admin Authentication**: Form-based login for admin dashboard
- **CSRF Protection**: Enabled for admin endpoints
- **Iframe Security**: Referer domain validation for embed tokens
- **Password Encryption**: BCrypt password hashing

## Troubleshooting

### Application won't start

1. **Database Connection Issues**:
   - Check MySQL is running and accessible
   - Verify database credentials in `application.properties`
   - For development, use `--spring.profiles.active=dev` to use H2

2. **Port Already in Use**:
   - Change `server.port` in `application.properties`
   - Or stop the process using the port

3. **Voice Models Not Found**:
   - Ensure voice model files (`.onnx` and `.onnx.json`) are in `src/main/resources/voices/`
   - Required voices: `edon`, `arta`, `arben`, `dren`

### API Returns 403 Forbidden

- Check API key has remaining tokens
- Verify token limit type and reset period (for monthly/yearly)
- Check API key is not disabled (for "once" type)

### Iframe Not Loading

- Verify referer domain is set correctly in API key settings
- Check iframe URL includes valid token
- Ensure `Content-Security-Policy` headers allow embedding

## Production Deployment

### Quick Production Setup Checklist

1. ✅ Install Java SDK 17+
2. ✅ Create dedicated user (`flossktts`)
3. ✅ Set up MySQL database
4. ✅ Build the JAR file: `mvn clean package -DskipTests`
5. ✅ Copy JAR to `/opt/flossk-tts/`
6. ✅ Create `application.properties` with production settings
7. ✅ Set up systemd service (or preferred process manager)
8. ✅ Configure reverse proxy (Nginx/Apache)
9. ✅ Set up SSL/TLS certificates
10. ✅ Configure firewall rules
11. ✅ Set up log rotation
12. ✅ Configure monitoring and alerts

### Detailed Production Setup

See `PRODUCTION_SETUP.md` for comprehensive production deployment instructions including:
- Complete systemd service configuration
- Docker containerization
- Nginx reverse proxy setup
- SSL/TLS configuration with Let's Encrypt
- Database migration and backup strategies
- Log rotation configuration
- Monitoring and health checks

## License

**License:** [GNU Affero General Public License v3.0 (AGPL-3.0)](https://opensource.org/license/agpl-3-0)

## Support

For issues and questions, please [create an issue](https://github.com/your-repo/issues) or contact support.
