#!/usr/bin/env sh
set -eu

PROJECT_ID="${1:-}"
if [ -z "$PROJECT_ID" ]; then
  echo "Usage: ./backend/scripts/configure-ttl.sh FIREBASE_PROJECT_ID" >&2
  exit 2
fi

if ! command -v gcloud >/dev/null 2>&1; then
  echo "gcloud is required. Install the Google Cloud CLI and authenticate first." >&2
  exit 1
fi

gcloud firestore fields ttls update expiresAt \
  --collection-group=typing \
  --enable-ttl \
  --project="$PROJECT_ID" \
  --async

gcloud firestore fields ttls update expireAt \
  --collection-group=functionEvents \
  --enable-ttl \
  --project="$PROJECT_ID" \
  --async

gcloud firestore fields ttls list --project="$PROJECT_ID"
