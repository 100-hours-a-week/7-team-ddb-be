#!/bin/bash
set -e

echo "▶ Promtail Docker 컨테이너 실행"

ENV_NAME=$(echo "$DEPLOYMENT_GROUP_NAME" | cut -d'-' -f2)
LOG_DIR="/home/ubuntu/logs"
CONFIG_PATH="/home/ubuntu/app/promtail/promtail.yaml"
POSITIONS_DIR="/home/ubuntu/app/promtail/positions"

mkdir -p "$POSITIONS_DIR"
chown -R 10001:10001 "$POSITIONS_DIR"

docker stop promtail || true
docker rm promtail || true

docker run -d \
  --name promtail \
  --restart=always \
  -e env="$ENV_NAME" \
  -v "$LOG_DIR":/var/log/spring \
  -v "$CONFIG_PATH":/etc/promtail/config.yaml \
  -v "$POSITIONS_DIR":/var/log/loki \
  grafana/promtail:2.8.0 \
  -config.file=/etc/promtail/config.yaml
