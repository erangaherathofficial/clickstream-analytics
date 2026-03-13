#!/bin/bash
set -euo pipefail

BASE_URL="http://localhost:8090/api/events"
TOTAL_EVENTS=100

PAGES=("/home" "/products" "/cart" "/checkout" "/about")
EVENT_TYPES=("PAGE_VIEW" "BUTTON_CLICK" "ADD_TO_CART" "SEARCH")
DEVICES=("mobile" "desktop" "tablet")
BROWSERS=("Chrome" "Firefox" "Safari" "Edge")
LOCATIONS=("US" "UK" "LK" "DE" "JP")

echo "Generating ${TOTAL_EVENTS} test events..."

for i in $(seq 1 "${TOTAL_EVENTS}"); do
    user_id="user-$((RANDOM % 5 + 1))"
    session_id="sess-${user_id}-$((RANDOM % 3 + 1))"
    page="${PAGES[$((RANDOM % ${#PAGES[@]}))]}"
    event_type="${EVENT_TYPES[$((RANDOM % ${#EVENT_TYPES[@]}))]}"
    device="${DEVICES[$((RANDOM % ${#DEVICES[@]}))]}"
    browser="${BROWSERS[$((RANDOM % ${#BROWSERS[@]}))]}"
    location="${LOCATIONS[$((RANDOM % ${#LOCATIONS[@]}))]}"
    timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)

    response=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"eventId\": \"evt-$(uuidgen | tr '[:upper:]' '[:lower:]')\",
            \"sessionId\": \"$session_id\",
            \"userId\": \"$user_id\",
            \"eventType\": \"$event_type\",
            \"timestamp\": \"$timestamp\",
            \"pageUrl\": \"$page\",
            \"elementId\": null,
            \"ipAddress\": \"192.168.1.$((RANDOM % 255))\",
            \"device\": \"$device\",
            \"browser\": \"$browser\",
            \"location\": \"$location\"
        }")

    if [ "$response" -eq 202 ]; then
        echo "[${i}/${TOTAL_EVENTS}] ${user_id} | ${event_type} | ${page}"
    else
        echo "[${i}/${TOTAL_EVENTS}] FAILED (HTTP ${response})"
    fi

    sleep 0.1
done

echo "Done. ${TOTAL_EVENTS} events sent."