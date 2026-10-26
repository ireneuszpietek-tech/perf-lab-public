#!/usr/bin/env bash
set -e

echo "Running Postman API tests..."

# install newman locally if not present
if ! command -v newman &> /dev/null; then
  echo "Newman not found – installing..."
  npm install -g newman > /dev/null
fi
  npm update -g newman > /dev/null


TOKEN="$1"
BASEURL="$2"

echo "TOKEN: ${TOKEN}"
echo "BASEURL: ${BASEURL}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

newman run "$SCRIPT_DIR/postman/shipping-collection.json" \
  --env-var "authToken=$TOKEN"  --env-var "baseUrl=$BASEURL"
