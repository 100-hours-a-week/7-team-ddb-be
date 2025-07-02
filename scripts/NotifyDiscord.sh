#!/bin/bash
set -e

echo "▶ 서비스 헬스체크 수행 시작"

PORT=8080
HEALTH_ENDPOINT="http://localhost:${PORT}/api/v1/health"
RETRY_COUNT=15
SLEEP_DURATION=5
IS_HEALTHY=false

for ((i=1; i<=RETRY_COUNT; i++)); do
  echo "▶ [시도 $i/${RETRY_COUNT}] ${HEALTH_ENDPOINT} 요청 중..."

  if curl -sf "$HEALTH_ENDPOINT" > /dev/null; then
    echo "✅ 헬스체크 성공"
    IS_HEALTHY=true
    break
  fi

  echo "❌ 아직 응답 없음, ${SLEEP_DURATION}초 후 재시도"
  sleep "$SLEEP_DURATION"
done

echo "❌ 헬스체크 실패 - CodeDeploy 배포 실패로 간주"

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

if [ "$IS_HEALTHY" = true ]; then
  MESSAGE="✅ CodeDeploy 배포 성공
- 애플리케이션: $APP_NAME
- 환경: $ENV_NAME
- [배포 확인하기]($DEPLOYMENT_URL)"
  STATUS=0
else
  MESSAGE="❌ CodeDeploy 배포 실패 (헬스체크 실패)
- 애플리케이션: $APP_NAME
- 환경: $ENV_NAME
- [배포 확인하기]($DEPLOYMENT_URL)"
  STATUS=1
fi

# JSON 변환
PAYLOAD=$(jq -n --arg content "$MESSAGE" '{content: $content}')

curl -H "Content-Type: application/json" \
     -X POST \
     -d "$PAYLOAD" \
     "$WEBHOOK_URL"

exit $STATUS
