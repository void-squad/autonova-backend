#!/usr/bin/env bash
set -eu
run_id=${1:-}
if [ -z "$run_id" ]; then
  echo "run id required as first arg"
  exit 1
fi
repo="${GITHUB_REPOSITORY:-}"
token="${GITHUB_TOKEN:-}"
if [ -z "$repo" ] || [ -z "$token" ]; then
  echo "GITHUB_REPOSITORY or GITHUB_TOKEN not set"
  exit 1
fi

# Download and inspect artifacts that match test-results-*
for name in $(curl -s -H "Authorization: token $token" \
              "https://api.github.com/repos/$repo/actions/runs/$run_id/artifacts" \
              | jq -r '.artifacts[].name' || true); do
  if [[ "$name" == test-results-* ]]; then
    echo "Found artifact: $name"
    mkdir -p debug-artifacts
    id=$(curl -s -H "Authorization: token $token" \
           "https://api.github.com/repos/$repo/actions/runs/$run_id/artifacts" \
           | jq -r ".artifacts[] | select(.name==\"$name\") | .id")
    echo "Downloading artifact id=$id"
    curl -s -L -H "Authorization: token $token" \
      "https://api.github.com/repos/$repo/actions/artifacts/${id}/zip" \
      -o "debug-artifacts/${name}.zip"
    unzip -l "debug-artifacts/${name}.zip" || true
    unzip -p "debug-artifacts/${name}.zip" "**/*.trx" 2>/dev/null | head -c 200 || true
    unzip -p "debug-artifacts/${name}.zip" "**/*.xml" 2>/dev/null | head -c 200 || true
  fi
done
