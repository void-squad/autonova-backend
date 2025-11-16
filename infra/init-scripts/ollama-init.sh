#!/bin/sh
set -eu

# Ollama init script
# Accepts MODEL_NAMES as a comma-separated list of model identifiers to ensure are pulled.
# Default: llama3
MODEL_NAMES=${MODEL_NAMES:-llama3}

# Clean a mounted script into a stable path and exec it (helps with CRLF mounts).
SOURCE2="/scripts/ollama-init.sh"
TARGET="/usr/startup.sh"
if [ -f "$SOURCE2" ]; then
  if command -v tr >/dev/null 2>&1; then
    tr -d '\r' < "$SOURCE2" > "$TARGET" || sed 's/\r$//' "$SOURCE2" > "$TARGET"
  else
    sed 's/\r$//' "$SOURCE2" > "$TARGET"
  fi
  chmod +x "$TARGET" || true
  case "$0" in
    "$TARGET") ;;
    *) exec "$TARGET" "$@" ;;
  esac
fi

echo "Ollama init: checking for running server..."
if ollama list > /dev/null 2>&1; then
  echo "Ollama already running"
  # Try to find a running PID; if not found we leave SERVER_PID empty
  SERVER_PID=$(pgrep -f "ollama serve" || true)
else
  echo "Starting Ollama server in background..."
  ollama serve &
  SERVER_PID=$!
fi

echo "Waiting for Ollama server to be ready..."
MAX_WAIT=300
WAITED=0
until ollama list > /dev/null 2>&1; do
  sleep 2
  WAITED=$((WAITED+2))
  if [ "$WAITED" -ge "$MAX_WAIT" ]; then
    echo "Ollama did not become ready after $MAX_WAIT seconds" >&2
    exit 1
  fi
done
echo "Ollama server is up."

# Pull required models (if missing)
OLD_IFS=$IFS
IFS=','
for m in $MODEL_NAMES; do
  model=$(echo "$m" | sed 's/^ *//;s/ *$//')
  [ -z "$model" ] && continue
  echo "Checking model: $model"
  if ! ollama list | grep -q "^$model\b"; then
    echo "Pulling model: $model"
    ollama pull "$model"
  else
    echo "Model '$model' already exists"
  fi
done
IFS=$OLD_IFS

echo "Ollama init complete. Server PID: $SERVER_PID"

# Wait on the server process to keep the container running
if [ -n "${SERVER_PID:-}" ]; then
  wait "$SERVER_PID"
else
  # If we couldn't obtain a PID, fall back to keeping the container alive
  echo "No server PID found; sleeping to keep container alive"
  tail -f /dev/null
fi

