#!/bin/sh

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
DIRNAME=$(dirname "$0")
APP_HOME=$(cd "$DIRNAME" && pwd)

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Use Java 17 for Android build compatibility
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export JAVA_HOME

exec $JAVA_HOME/bin/java -Xmx64m -Xms64m -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
