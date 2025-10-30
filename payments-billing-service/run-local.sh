#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

PROFILE=""
ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    -p|--profile)
      PROFILE="$2"; shift 2;;
    --) shift; break;;
    *) ARGS+=("$1"); shift;;
  esac
done
ARGS+=("$@")

ENV_FILE=""
for candidate in "$ENV_FILE" .env ../.env; do
  [[ -z "$candidate" ]] && continue
  if [[ -f "$candidate" ]]; then
    echo "Loading env from $candidate..."
    # Normalize CRLF to LF into a temp copy to avoid bash CR issues
    TMP_ENV=$(mktemp)
    sed 's/\r$//' "$candidate" > "$TMP_ENV"
    # Prefer simple and robust loader when file is shell-compatible
    if ! grep -qE '^\s*export\s' "$TMP_ENV"; then
      # shellcheck disable=SC1090
      set -a; source "$TMP_ENV"; set +a
    else
      echo "Stripping 'export' keywords for compatibility" >&2
      ENV_CONTENT=$(sed -E 's/^\s*export\s+//g' "$TMP_ENV" | grep -Ev '^\s*#' | sed '/^\s*$/d')
      while IFS= read -r line; do
        KEY=${line%%=*}
        VAL=${line#*=}
        KEY=$(echo "$KEY" | xargs)
        if [[ "$VAL" =~ ^".*"$ || "$VAL" =~ ^'.*'$ ]]; then
          VAL=${VAL:1:-1}
        fi
        export "$KEY"="$VAL"
      done <<< "$ENV_CONTENT"
    fi
    rm -f "$TMP_ENV"
    break
  fi
done

# Minimal validation for commonly required vars
REQ_VARS=(
  AUTH_JWT_ISSUER
  AUTH_JWT_AUDIENCE
  AUTH_JWT_HS256_SECRET
)

MISSING=()
for v in "${REQ_VARS[@]}"; do
  if [[ -z "${!v-}" ]]; then
    MISSING+=("$v")
  fi
done

if [[ ${#MISSING[@]} -gt 0 ]]; then
  echo "Missing required environment variables: ${MISSING[*]}" >&2
  echo "Ensure they are present in .env or exported in your shell." >&2
  exit 1
fi

# Show masked values for sanity
echo "AUTH_JWT_ISSUER=${AUTH_JWT_ISSUER}"
echo "AUTH_JWT_AUDIENCE=${AUTH_JWT_AUDIENCE}"
echo "AUTH_JWT_HS256_SECRET=$(printf '%s' "${AUTH_JWT_HS256_SECRET}" | sed 's/./*/g')"

RUN_ARGS=()
if [[ -n "$PROFILE" ]]; then
  RUN_ARGS+=("-Dspring-boot.run.profiles=$PROFILE")
fi

if [[ ${#ARGS[@]} -gt 0 ]]; then
  RUN_ARGS+=("-Dspring-boot.run.arguments=${ARGS[*]}")
fi

echo "Starting payments-billing-service..."
exec ./mvnw spring-boot:run "${RUN_ARGS[@]}"
