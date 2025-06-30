#!/bin/bash
set -e

WEBHOOK_URL=$(aws secretsmanager get-secret-value \
  --secret-id codedeploy/discord/webhook \
  --query 'SecretString' --output text)

RAW_APP_NAME="${APPLICATION_NAME:-N/A}"
DEPLOY_GROUP="${DEPLOYMENT_GROUP_NAME:-N/A}"
APP_NAME=$(echo "$RAW_APP_NAME" | cut -d'-' -f1)
ENV_NAME=$(echo "$DEPLOY_GROUP" | cut -d'-' -f2)

DEPLOYMENT_ID="${DEPLOYMENT_ID:-N/A}"
REGION="ap-northeast-2"
DEPLOYMENT_URL="https://${REGION}.console.aws.amazon.com/codesuite/codedeploy/deployments/${DEPLOYMENT_ID}?region=${REGION}"

MESSAGE="✅ CodeDeploy 배포 성공
- 애플리케이션: $APP_NAME
- 환경: $ENV_NAME
- [배포 확인하기]($DEPLOYMENT_URL)"

# JSON 변환
PAYLOAD=$(jq -n --arg content "$MESSAGE" '{content: $content}')

curl -H "Content-Type: application/json" \
     -X POST \
     -d "$PAYLOAD" \
     "$WEBHOOK_URL"