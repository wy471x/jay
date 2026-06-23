#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "==> Building TUI module..."
gradle :tui:jar -q

# Collect project jars
PROJECT_JARS=$(find "$PROJECT_ROOT" -name "*.jar" -path "*/build/libs/*" | grep -v "plain" | tr '\n' ':')

# Collect TamboUI 0.3.0 jars
TAMBOUI_JARS=$(find ~/.gradle/caches -name "tamboui-*.jar" -path "*/0.3.0/*" | tr '\n' ':')

# Collect other runtime dependencies
GRADLE_CACHE=~/.gradle/caches/modules-2/files-2.1
DEP_JARS=$(find $GRADLE_CACHE \
  -name "spring-boot-starter-3.4.7.jar" -o \
  -name "spring-boot-3.4.7.jar" -o \
  -name "spring-boot-autoconfigure-3.4.7.jar" -o \
  -name "spring-core-6.2.8.jar" -o \
  -name "spring-context-6.2.8.jar" -o \
  -name "spring-beans-6.2.8.jar" -o \
  -name "spring-aop-6.2.8.jar" -o \
  -name "spring-expression-6.2.8.jar" -o \
  -name "spring-jcl-6.2.8.jar" -o \
  -name "spring-jdbc-6.2.8.jar" -o \
  -name "spring-tx-6.2.8.jar" -o \
  -name "spring-data-jdbc-3.4.7.jar" -o \
  -name "spring-data-relational-3.4.7.jar" -o \
  -name "spring-data-commons-3.4.7.jar" -o \
  -name "jackson-core-2.18.3.jar" -o \
  -name "jackson-databind-2.18.3.jar" -o \
  -name "jackson-annotations-2.18.3.jar" -o \
  -name "jackson-dataformat-toml-2.18.3.jar" -o \
  -name "jackson-datatype-jsr310-2.18.3.jar" -o \
  -name "logback-classic-1.5.18.jar" -o \
  -name "logback-core-1.5.18.jar" -o \
  -name "slf4j-api-2.0.17.jar" -o \
  -name "log4j-to-slf4j-2.24.3.jar" -o \
  -name "log4j-api-2.24.3.jar" -o \
  -name "jul-to-slf4j-2.0.17.jar" -o \
  -name "micrometer-observation-1.14.8.jar" -o \
  -name "micrometer-commons-1.14.8.jar" -o \
  -name "HikariCP-5.1.0.jar" -o \
  -name "sqlite-jdbc-3.47.1.0.jar" -o \
  -name "flyway-core-10.22.0.jar" -o \
  -name "jakarta.annotation-api-2.1.1.jar" -o \
  -name "snakeyaml-2.3.jar" \
  2>/dev/null | tr '\n' ':')

CLASSPATH="${PROJECT_JARS}${TAMBOUI_JARS}${DEP_JARS}"

echo "==> Starting TUI..."
exec java --enable-native-access=ALL-UNNAMED \
  --class-path "$CLASSPATH" \
  com.jay.tui.TuiApplication "$@"
