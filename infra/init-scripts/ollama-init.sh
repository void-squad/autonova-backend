#!/bin/bash
set -e

# Start Ollama server
ollama serve &
SERVER_PID=$!

# Wait until server is ready using ollama CLI
echo "Waiting for Ollama server..."
until ollama list > /dev/null 2>&1; do
  sleep 2
done

# Pull llama3 if not already downloaded
if ! ollama list | grep -q "llama3"; then
  echo "Pulling model: llama3"
  ollama pull llama3
else
  echo "Model llama3 already exists"
fi

# Keep server running
wait $SERVER_PID
