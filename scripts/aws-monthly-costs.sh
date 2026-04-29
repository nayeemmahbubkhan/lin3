#!/usr/bin/env bash
set -euo pipefail

start_date="$(date +%Y-%m-01)"
end_date="$(date -d '+1 day' +%Y-%m-%d)"

services=(
  "Amazon Elastic Compute Cloud - Compute"
  "Amazon Elastic Block Store"
  "AmazonCloudWatch"
  "Amazon Route 53"
  "Amazon Simple Notification Service"
)

service_values="$(printf '"%s",' "${services[@]}")"
service_values="${service_values%,}"

echo "Monthly AWS spend snapshot"
echo "Range: ${start_date} -> ${end_date}"
echo

auth_account="$(aws sts get-caller-identity --query Account --output text)"
echo "Account: ${auth_account}"
echo

aws ce get-cost-and-usage \
  --time-period "Start=${start_date},End=${end_date}" \
  --granularity MONTHLY \
  --metrics UnblendedCost \
  --filter "{\"Dimensions\":{\"Key\":\"SERVICE\",\"Values\":[${service_values}]}}" \
  --group-by Type=DIMENSION,Key=SERVICE \
  --query 'ResultsByTime[0].Groups[].{Service:Keys[0],Amount:Metrics.UnblendedCost.Amount,Unit:Metrics.UnblendedCost.Unit}' \
  --output table

