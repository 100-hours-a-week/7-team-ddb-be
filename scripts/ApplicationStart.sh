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

echo "▶ DB, Redis 비밀번호 시크릿 조회 및 병합"
cp /home/ubuntu/.env /home/ubuntu/.env.final

SECRET_JSON=$(aws secretsmanager get-secret-value \
  --secret-id ${ENV_NAME}/rds/credentials/secret \
  --query SecretString \
  --output text)

DB_PASSWORD=$(echo "$SECRET_JSON" | jq -r .password)
DB_HOST=$(echo "$SECRET_JSON" | jq -r .host)

SECRET_JSON=$(aws secretsmanager get-secret-value \
  --secret-id ${ENV_NAME}/redis/credentials/secret \
  --query SecretString \
  --output text)

REDIS_HOST=$(echo "$SECRET_JSON" | jq -r .host)
REDIS_PORT=$(echo "$SECRET_JSON" | jq -r .port)
REDIS_PASSWORD=$(echo "$SECRET_JSON" | jq -r .password)
REDIS_USERNAME=$(echo "$SECRET_JSON" | jq -r .username)

{
  echo "DB_PASSWORD=$DB_PASSWORD"
  echo "DATASOURCE_URL=$DB_HOST"
  echo "REDIS_HOST=$REDIS_HOST"
  echo "REDIS_PORT=$REDIS_PORT"
  echo "REDIS_PASSWORD=$REDIS_PASSWORD"
  echo "REDIS_USERNAME=$REDIS_USERNAME"
} >> /home/ubuntu/.env.final

docker run -d --name "$CONTAINER_NAME" \
  --restart=unless-stopped \
  --env-file /home/ubuntu/.env.final \
  -v /home/ubuntu/logs:/logs \
  -p "$PORT:$PORT" -p 8081:8081 \
  "$IMAGE"