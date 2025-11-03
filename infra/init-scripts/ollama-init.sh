#!/bin/bash
set -e

# === Configurable Model Name ===
MODEL_NAME=${MODEL_NAME:-llama3}

# Start Ollama server in the background
ollama serve &
SERVER_PID=$!

# Wait until the Ollama server is ready
echo "Waiting for Ollama server to start..."
until ollama list > /dev/null 2>&1; do
  sleep 2
done
echo "Ollama server is up."

# Pull model if not already downloaded
if ! ollama list | grep -q "$MODEL_NAME"; then
  echo "Pulling model: $MODEL_NAME"
  ollama pull "$MODEL_NAME"
else
  echo "Model '$MODEL_NAME' already exists"
fi

# Run the model
echo "Starting model: $MODEL_NAME"
ollama run "$MODEL_NAME" &

# Keep the server running
wait $SERVER_PID

