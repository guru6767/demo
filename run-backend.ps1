# Starto V2 - Backend Runner
# This script ensures Maven is available and runs the Spring Boot application.

$MAVEN_VERSION = "3.9.6"
$MAVEN_DIR = "$PSScriptRoot\.maven"
$MAVEN_ZIP = "$MAVEN_DIR\apache-maven-$MAVEN_VERSION-bin.zip"
$MAVEN_HOME = "$MAVEN_DIR\apache-maven-$MAVEN_VERSION"
$MVN_BIN = "$MAVEN_HOME\bin\mvn.cmd"

# Set JAVA_HOME (identified from previous research)
$jdkPath = Get-ChildItem -Path "C:\Program Files\Java" -Filter "jdk*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdkPath) {
    $env:JAVA_HOME = $jdkPath.FullName
} elseif (Test-Path "D:\Android\jbr\bin\java.exe") {
    $env:JAVA_HOME = "D:\Android\jbr"
} elseif (Test-Path "D:\bin\java.exe") {
    $env:JAVA_HOME = "D:\"
} elseif ($null -eq $env:JAVA_HOME -or $env:JAVA_HOME -eq "") {
    $javaPath = where.exe java | Select-Object -First 1
    if ($javaPath) {
        $env:JAVA_HOME = [System.IO.Path]::GetDirectoryName([System.IO.Path]::GetDirectoryName($javaPath))
    } else {
        $env:JAVA_HOME = "C:\Program Files\Java\jdk-23" # Fallback
    }
}
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

# Set Default Environment Variables for Local Development
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/starto"
$env:DATABASE_USERNAME = "postgres"
$env:DATABASE_PASSWORD = "admin"
$env:REDIS_URL = "redis://localhost:6379"
$env:FIREBASE_CONFIG_PATH = "$PSScriptRoot\firebase-service-account.json"
$env:OPENAI_API_KEY = "dummy_key"
$env:GEMINI_API_KEY = "dummy_key"
$env:RAZORPAY_KEY_ID = "dummy_key"
$env:RAZORPAY_KEY_SECRET = "dummy_key"
$env:RAZORPAY_WEBHOOK_SECRET = "dummy_key"
$env:STRIPE_API_KEY = "dummy_key"
$env:STRIPE_WEBHOOK_SECRET = "dummy_key"
$env:SENDGRID_API_KEY = "dummy_key"

if (-not (Test-Path $MAVEN_DIR)) {
    New-Item -ItemType Directory -Path $MAVEN_DIR | Out-Null
}

if (-not (Test-Path $MVN_BIN)) {
    Write-Host "Maven not found. Downloading Maven $MAVEN_VERSION..." -ForegroundColor Cyan
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"
    Invoke-WebRequest -Uri $url -OutFile $MAVEN_ZIP
    
    Write-Host "Extracting Maven..." -ForegroundColor Cyan
    Expand-Archive -Path $MAVEN_ZIP -DestinationPath $MAVEN_DIR
    Remove-Item $MAVEN_ZIP
}

Write-Host "Starting Starto V2 Backend..." -ForegroundColor Green
Set-Location -Path "$PSScriptRoot\starto-api"
& $MVN_BIN spring-boot:run