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
curl -s -H "Authorization: token $token" \
  "https://api.github.com/repos/$repo/actions/runs/$run_id/artifacts" \
  | jq -r '.artifacts[] | "- \(.name) (id=\(.id), size=\(.size_in_bytes))"' || true
