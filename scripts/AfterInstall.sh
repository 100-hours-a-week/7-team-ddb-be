#!/bin/bash
set -e

echo "▶ ECR 로그인"
AWS_REGION="ap-northeast-2"
ACCOUNT_ID=$(curl -s http://169.254.169.254/latest/dynamic/instance-identity/document | grep accountId | awk -F\" '{print $4}')
ENV_NAME=$(echo "$DEPLOYMENT_GROUP_NAME" | cut -d'-' -f2)
REPO_NAME="dolpin-backend-${ENV_NAME}"
IMAGE_TAG=$(cat /home/ubuntu/app/.image_tag)
IMAGE="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}:${IMAGE_TAG}"

aws ecr get-login-password --region "$AWS_REGION" | \
  docker login --username AWS --password-stdin "${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

echo "▶ 이미지 풀"
docker pull "$IMAGE"

echo "$IMAGE" > /home/ubuntu/app/.image_ref
