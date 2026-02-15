#!/bin/bash
set -euo pipefail

BASE_URL="http://localhost:8090/api/metrics"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Clickstream Analytics Report"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "📈 Summary:"
curl -s "$BASE_URL/summary" | jq .
echo ""

echo "📋 Latest Metrics (top 10):"
curl -s "$BASE_URL" | jq '.[0:10]'
echo ""

echo "🗄️  PostgreSQL Aggregated View:"
docker exec -it postgres psql -U clickstream -d clickstream -c "
SELECT page_url, event_type, SUM(event_count) as total_events, COUNT(*) as time_windows
FROM page_view_metrics
GROUP BY page_url, event_type
ORDER BY total_events DESC
LIMIT 10;"