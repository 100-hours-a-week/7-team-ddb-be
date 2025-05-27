pipeline {
    agent any

    environment {
        SERVICE_NAME    = 'backend'
        PROJECT_ID      = 'velvety-calling-458402-c1'
        REGION          = 'asia-northeast3'
        GAR_HOST        = 'asia-northeast3-docker.pkg.dev'
        CONTAINER_NAME  = 'backend'
        PORT            = '8080'
        SSH_KEY_PATH    = '/var/jenkins_home/.ssh/id_rsa'
        SSH_USER        = 'peter'
    }

    stages {
        stage('Setup Environment by Branch') {
            steps {
                script {
                    def branchName = env.GIT_BRANCH.replaceFirst(/^origin\//, '')
                    env.BRANCH = branchName

                    // 브랜치에 따라 환경 분기 설정
                    if (branchName == 'main') {
                        env.BE_PRIVATE_IP = '10.10.30.2'
                        env.ENV_LABEL = 'prod'
                        env.REPO_NAME = 'dolpin-docker-image-prod'
                    } else if (branchName == 'dev') {
                        env.BE_PRIVATE_IP = '10.20.30.2'
                        env.ENV_LABEL = 'dev'
                        env.REPO_NAME = 'dolpin-docker-image-dev'
                    } else {
                        error "⚠️ 지원되지 않는 브랜치입니다: ${branchName}"
                    }

                    env.TAG = "${env.SERVICE_NAME}:${env.BUILD_NUMBER}"
                    env.GAR_IMAGE = "${env.GAR_HOST}/${env.PROJECT_ID}/${env.REPO_NAME}/${env.TAG}"
                }
            }
        }

        stage('Notify Before Start') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'Discord-Webhook', variable: 'DISCORD')]) {
                        discordSend(
                            description: "🚀 배포가 곧 시작됩니다: ${env.SERVICE_NAME} - ${env.BRANCH} 브랜치",
                            link: env.BUILD_URL,
                            title: "배포 시작",
                            webhookURL: "$DISCORD"
                        )
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build JAR') {
            steps {
                sh './gradlew clean build'
            }
        }

        stage('GAR 인증') {
            steps {
                sh "gcloud auth configure-docker ${env.GAR_HOST} --quiet"
            }
        }

        stage('Docker Build & Push to GAR') {
            steps {
                sh """
                    docker build -t ${env.GAR_IMAGE} .
                    docker push ${env.GAR_IMAGE}
                """
            }
        }

        stage('Create .env from Secret Manager') {
            steps {
                // GCP Secret Manager에서 DB Password, DATASOURCE_URL 다운로드
                sh """
                    DB_PASSWORD=\$(gcloud secrets versions access latest --secret="cloudsql-dolpinuser-password-${env.ENV_LABEL}")
                    DB_HOST=\$(gcloud secrets versions access latest --secret="cloudsql-public-ip-${env.ENV_LABEL}")

                    printf 'DB_PASSWORD=%s\n' "\$DB_PASSWORD" > .env.db
                    printf 'DATASOURCE_URL=%s:5432\n' "\$DB_HOST" >> .env.db
                """
            }
        }

        stage('Deploy to BE via SSH') {
            steps {
                script {
                    def saCredId = ''
                    def envFileId = ''

                    if (env.BRANCH == 'main') {
                        saCredId = 'be-sa-key-prod'
                        envFileId = 'be-prod-file'
                    } else if (env.BRANCH == 'dev') {
                        saCredId = 'be-sa-key-dev'
                        envFileId = 'be-dev-file'
                    }

                    // GCP Secret Manager에서 서비스 계정 키 다운로드
                    sh """
                        gcloud secrets versions access latest --secret="${saCredId}" --project="${env.PROJECT_ID}" > gcp-key.json
                    """
                    withCredentials([
                        file(credentialsId: envFileId, variable: 'ENV_FILE')
                    ]) {
                        def deployScript = """
#!/bin/bash
set -e

mv /tmp/.env.final /home/${env.SSH_USER}/.env
mv /tmp/gcp-key.json /home/${env.SSH_USER}/gcp-key.json
chown ${env.SSH_USER}:${env.SSH_USER} /home/${env.SSH_USER}/.env /home/${env.SSH_USER}/gcp-key.json
chmod 600 /home/${env.SSH_USER}/.env /home/${env.SSH_USER}/gcp-key.json

# 서비스 계정 인증 및 docker 인증
export HOME=/home/${env.SSH_USER}
gcloud auth activate-service-account --key-file="/home/${env.SSH_USER}/gcp-key.json"
gcloud config set project ${env.PROJECT_ID} --quiet
gcloud auth configure-docker ${env.GAR_HOST} --quiet
gcloud auth print-access-token | docker login -u oauth2accesstoken --password-stdin https://${env.GAR_HOST}

sudo docker stop ${env.CONTAINER_NAME} || true
sudo docker rm ${env.CONTAINER_NAME} || true

docker pull ${env.GAR_IMAGE}

sudo docker run -d --name ${env.CONTAINER_NAME} \
  --env-file /home/${env.SSH_USER}/.env \
  -v /home/${env.SSH_USER}/gcp-key.json:/home/${env.SSH_USER}/gcp-key.json \
  -v /home/${env.SSH_USER}/logs:/logs \
  -p ${env.PORT}:${env.PORT} -p 8081:8081 \
  ${env.GAR_IMAGE}

"""
                        // Jenkins 워크스페이스에 배포 스크립트 파일 저장
                        writeFile file: 'deploy.sh', text: deployScript

                        // 키와 스크립트 전송 후 실행
                        sh """
chmod 600 ${env.SSH_KEY_PATH}

sed -i -e '\$a\\' \$ENV_FILE
cat \$ENV_FILE .env.db > .env.final

scp -i ${env.SSH_KEY_PATH} -o StrictHostKeyChecking=no .env.final ${env.SSH_USER}@${env.BE_PRIVATE_IP}:/tmp/.env.final
scp -i ${env.SSH_KEY_PATH} -o StrictHostKeyChecking=no gcp-key.json ${env.SSH_USER}@${env.BE_PRIVATE_IP}:/tmp/gcp-key.json
scp -i ${env.SSH_KEY_PATH} -o StrictHostKeyChecking=no deploy.sh ${env.SSH_USER}@${env.BE_PRIVATE_IP}:/tmp/deploy.sh

ssh -tt -i ${env.SSH_KEY_PATH} -o StrictHostKeyChecking=no ${env.SSH_USER}@${env.BE_PRIVATE_IP} "bash /tmp/deploy.sh"
                        """
                    }
                }
            }
        }
    }
    
    post {
        success {
            withCredentials([string(credentialsId: 'Discord-Webhook', variable: 'DISCORD')]) {
                discordSend description: """
                제목 : ${currentBuild.displayName}
                결과 : ${currentBuild.result}
                실행 시간 : ${currentBuild.duration / 1000}s
                """,
                link: env.BUILD_URL, result: currentBuild.currentResult,
                title: "${env.JOB_NAME} : ${currentBuild.displayName} 성공",
                webhookURL: "$DISCORD"
            }
        }
        failure {
            withCredentials([string(credentialsId: 'Discord-Webhook', variable: 'DISCORD')]) {
                discordSend description: """
                제목 : ${currentBuild.displayName}
                결과 : ${currentBuild.result}
                실행 시간 : ${currentBuild.duration / 1000}s
                """,
                link: env.BUILD_URL, result: currentBuild.currentResult,
                title: "${env.JOB_NAME} : ${currentBuild.displayName} 실패",
                webhookURL: "$DISCORD"
            }
        }
    }
}
