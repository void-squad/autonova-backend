#!/bin/sh
set -e

# Allow overriding the jar location via env if needed
: ${JAR_PATH:=/app/progressmonitoring.jar}

echo "Starting progress-monitoring with JAVA_OPTS=${JAVA_OPTS}"

exec java ${JAVA_OPTS} -jar "${JAR_PATH}"
