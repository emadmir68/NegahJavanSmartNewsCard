@ECHO OFF
SETLOCAL
SET DIR=%~dp0
SET JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
IF EXIST "%JAR%" (
  ECHO Gradle wrapper is already ready.
  PAUSE
  EXIT /B 0
)
ECHO Preparing Android project...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%JAR%'"
IF ERRORLEVEL 1 (
  ECHO Download failed. Check internet access and try again.
  PAUSE
  EXIT /B 1
)
ECHO Done. Now open this folder in Android Studio.
PAUSE
