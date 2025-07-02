#!/bin/bash
set -e

IMAGE=$(cat /home/ubuntu/app/.image_ref)
PORT=8080
CONTAINER_NAME=backend
ENV_NAME=$(echo "$DEPLOYMENT_GROUP_NAME" | cut -d'-' -f2)

SECRET_JSON=$(aws secretsmanager get-secret-value \
  --secret-id ${ENV_NAME}/backend/env \
  --query SecretString \
  --output text)

echo "$SECRET_JSON" | jq -r 'to_entries[] | "\(.key)=\(.value)"' > /home/ubuntu/.env

echo "▶ DB 비밀번호 시크릿 조회 및 병합"
cp /home/ubuntu/.env /home/ubuntu/.env.final

SECRET_JSON=$(aws secretsmanager get-secret-value \
  --secret-id ${ENV_NAME}/rds/credentials/secret \
  --query SecretString \
  --output text)

DB_PASSWORD=$(echo "$SECRET_JSON" | jq -r .password)
DB_HOST=$(echo "$SECRET_JSON" | jq -r .host)

echo "DB_PASSWORD=$DB_PASSWORD" >> /home/ubuntu/.env.final
echo "DATASOURCE_URL=$DB_HOST" >> /home/ubuntu/.env.final

docker run -d --name "$CONTAINER_NAME" \
  --restart=always \
  --env-file /home/ubuntu/.env.final \
  -v /home/ubuntu/logs:/logs \
  -p "$PORT:$PORT" -p 8081:8081 \
  "$IMAGE"