#!/bin/sh
APP_HOME=$(cd "${0%/*}" && pwd -P)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  URL="https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar"
  if command -v curl >/dev/null 2>&1; then curl -fL "$URL" -o "$JAR" || exit 1
  elif command -v wget >/dev/null 2>&1; then wget -O "$JAR" "$URL" || exit 1
  else echo "Gradle wrapper JAR missing. Install curl/wget or download $URL"; exit 1
  fi
fi
exec java -Xmx64m -Xms64m -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
