@ECHO OFF
SETLOCAL
SET DIR=%~dp0
SET JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
IF NOT EXIST "%JAR%" (
  ECHO Downloading Gradle wrapper...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%JAR%'"
  IF ERRORLEVEL 1 EXIT /B 1
)
java -Xmx64m -Xms64m -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
ENDLOCAL
