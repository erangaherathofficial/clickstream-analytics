#!/bin/bash
set -euo pipefail

BASE_URL="http://localhost:8090/api/events"
TOTAL_EVENTS=1000
BATCH_SIZE=50

echo "🚀 Performance test: $TOTAL_EVENTS events"
echo "⚙️  Batch size: $BATCH_SIZE parallel requests"
echo ""

start_time=$(date +%s)

for i in $(seq 1 $TOTAL_EVENTS); do
    curl -s -o /dev/null -X POST "$BASE_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"eventId\": \"perf-evt-$i\",
            \"sessionId\": \"perf-sess-$((i % 10 + 1))\",
            \"userId\": \"perf-user-$((i % 5 + 1))\",
            \"eventType\": \"PAGE_VIEW\",
            \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
            \"pageUrl\": \"/perf-test/page-$((i % 20))\",
            \"elementId\": null,
            \"ipAddress\": \"10.0.0.$((i % 255))\",
            \"device\": \"desktop\",
            \"browser\": \"Chrome\",
            \"location\": \"US\"
        }" &

    if (( i % BATCH_SIZE == 0 )); then
        wait
        echo "📊 Progress: $i/$TOTAL_EVENTS events sent"
    fi
done

wait

end_time=$(date +%s)
duration=$((end_time - start_time))

if [ "$duration" -eq 0 ]; then
    duration=1
fi

throughput=$((TOTAL_EVENTS / duration))

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Completed: $TOTAL_EVENTS events"
echo "⏱️  Duration: ${duration}s"
echo "🚀 Throughput: ~${throughput} events/sec"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📈 Check results: http://localhost:8090/api/metrics/summary"