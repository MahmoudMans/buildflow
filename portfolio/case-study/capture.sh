#!/usr/bin/env bash
# Takes the case-study screenshots from a running BuildFlow demo.
# It only reads: it signs in with the demo accounts and looks at pages, it never sends a form.
#
#   bash portfolio/case-study/capture.sh
#
# Needs: the API running with the demo data (docker compose up, BUILDFLOW_DEMO_ENABLED=true).
# If nothing is listening on http://localhost:3000 it builds and starts the frontend for you,
# and stops it again at the end. Playwright is installed in a temporary folder outside the project.
set -u

ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "Run this inside the buildflow repo."; exit 1; }
cd "$ROOT" || exit 1

KIT="portfolio/case-study"
[ -f "$KIT/tools/capture.mjs" ] || { echo "$KIT/tools/capture.mjs not found."; exit 1; }
for tool in node npm curl; do
  command -v "$tool" >/dev/null 2>&1 || { echo "$tool not found."; exit 1; }
done

BASE_URL="${BASE_URL:-http://localhost:3000}"
API_URL="${API_URL:-http://localhost:8080}"
OUT_DIR="$ROOT/$KIT/screenshots"

TMP="$(mktemp -d)"
REPORT="$TMP/report.txt"
SERVER_PID=""
STARTED=0

cleanup() {
  if [ "$STARTED" -eq 1 ]; then
    [ -n "$SERVER_PID" ] && pkill -P "$SERVER_PID" 2>/dev/null
    [ -n "$SERVER_PID" ] && kill "$SERVER_PID" 2>/dev/null
    lsof -ti tcp:3000 2>/dev/null | xargs kill 2>/dev/null
  fi
  return 0
}
trap cleanup EXIT

http_code() { curl -s -o /dev/null -m 5 -w '%{http_code}' "$1" 2>/dev/null; }
# Anything that answers counts as running (a 404 still means a server is listening).
is_up() { [ "$(http_code "$1")" != "000" ]; }

[ "$(http_code "$API_URL/actuator/health")" = "200" ] || {
  echo "The API does not answer on $API_URL."
  echo "Start the project first (docker compose up, with BUILDFLOW_DEMO_ENABLED=true), then run this again."
  exit 1
}

# The browser asks the API first whether the frontend's address is allowed (CORS).
# Another project's API on the same port answers "Invalid CORS request", and every sign-in then fails.
REFUSED="$(curl -s -m 5 -H "Origin: $BASE_URL" "$API_URL/api/v1/auth/me" 2>/dev/null | grep -c 'Invalid CORS request')"
if [ "$REFUSED" != "0" ]; then
  echo "The API on $API_URL refuses requests from $BASE_URL (CORS), so signing in from the browser cannot work."
  echo "That usually means a different project's server is using this port, or BuildFlow is set up for another frontend address."
  OWNER_PID="$(lsof -nP -iTCP:8080 -sTCP:LISTEN -t 2>/dev/null | head -1)"
  [ -n "$OWNER_PID" ] && echo "Port 8080 is held by pid $OWNER_PID: $(ps -p "$OWNER_PID" -o command= 2>/dev/null | cut -c1-160)"
  echo "Stop that server, start BuildFlow (docker compose up -d --build backend frontend), then run this again."
  exit 1
fi

if is_up "$BASE_URL/"; then
  echo "Using the frontend that is already running at $BASE_URL"
  echo "(A development server shows a small Next.js badge in the corner. A production build does not.)"
else
  [ "$BASE_URL" = "http://localhost:3000" ] || { echo "Nothing answers on $BASE_URL."; exit 1; }
  command -v pnpm >/dev/null 2>&1 || { echo "pnpm not found: needed to build the frontend."; exit 1; }

  echo ">> The frontend is not running: building it and starting it for the screenshots (a few minutes)"
  ( cd frontend && pnpm build ) >"$TMP/build.log" 2>&1 </dev/null || { echo "The frontend build failed:"; tail -30 "$TMP/build.log"; exit 1; }

  ( cd frontend && exec pnpm exec next start -p 3000 ) >"$TMP/server.log" 2>&1 </dev/null &
  SERVER_PID=$!
  STARTED=1

  i=0
  until is_up "$BASE_URL/"; do
    i=$((i + 1))
    [ "$i" -gt 60 ] && { echo "The frontend did not start:"; tail -20 "$TMP/server.log"; exit 1; }
    sleep 2
  done
  echo "Frontend started."
fi

WORK="$TMP/shots"
mkdir -p "$WORK" "$OUT_DIR"
cp "$ROOT/$KIT/tools/capture.mjs" "$WORK/"

echo ">> Installing Playwright in a temporary folder (first run takes a minute)"
( cd "$WORK" && npm init -y >/dev/null 2>&1 && npm install playwright >"$TMP/npm.log" 2>&1 ) </dev/null || {
  echo "npm install playwright failed:"; tail -20 "$TMP/npm.log"; exit 1
}

if ! ( cd "$WORK" && node -e "import('playwright').then(async (m) => { try { const b = await m.chromium.launch({ channel: 'chrome' }); await b.close(); } catch (e) { process.exit(1); } })" ) >/dev/null 2>&1 </dev/null; then
  echo ">> Google Chrome was not found: downloading Chromium for Playwright (about 150 MB, first run only)"
  ( cd "$WORK" && npx playwright install chromium ) >"$TMP/install.log" 2>&1 </dev/null || {
    echo "Downloading Chromium failed:"; tail -20 "$TMP/install.log"; exit 1
  }
fi

echo ">> Taking the screenshots (about a minute)"
( cd "$WORK" && OUT_DIR="$OUT_DIR" BASE_URL="$BASE_URL" node capture.mjs ) 2>&1 </dev/null | tee "$TMP/capture.log"
CAPTURE_CODE=${PIPESTATUS[0]}

{
  echo "=== CAPTURE (exit $CAPTURE_CODE) ==="
  echo "frontend: $BASE_URL    api: $API_URL"
  sed -n '/=== RESULT/,$p' "$TMP/capture.log"
  echo
  echo "=== WHAT THE TOOL SAW ==="
  grep -E 'FAILED|roles on /demo|signed in as|projects:' "$TMP/capture.log" | cut -c1-260
  echo
  echo "=== FILES IN $KIT/screenshots ==="
  ls -l "$OUT_DIR" | awk '{ print $5, $9 }' | tail -n +2
  command -v file >/dev/null 2>&1 && ( cd "$OUT_DIR" && file 0*.png 1*.png 2*.png 2>/dev/null | cut -c1-110 )
  echo "total size: $(du -sh "$OUT_DIR" | cut -f1)"
  echo
  git status --short | head -10
} > "$REPORT" 2>&1

if command -v pbcopy >/dev/null 2>&1; then
  pbcopy < "$REPORT"
  echo
  echo "Report copied to the clipboard ($(wc -c < "$REPORT" | tr -d ' ') bytes)."
fi

echo
echo "Screenshots are in $KIT/screenshots"
echo "Open _contact-sheet.jpg there to look at all of them at once."
exit "$CAPTURE_CODE"
