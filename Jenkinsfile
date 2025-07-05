pipeline {
    agent any

    environment {
        SERVICE_NAME = 'backend'
        AWS_REGION   = 'ap-northeast-2'
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Set Branch') {
            steps {
                script {
                    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH?.replaceFirst(/^origin\//, '').trim()
                    def isMain = (branch == 'main')
                    def envLabel = isMain ? 'prod' : 'dev'

                    env.BRANCH = branch
                    env.ENV_LABEL = envLabel
                    env.ECR_REPO = "794038223418.dkr.ecr.${env.AWS_REGION}.amazonaws.com/dolpin-${env.SERVICE_NAME}-${envLabel}"
                    env.S3_BUCKET = "${envLabel}-dolpin-codedeploy-artifacts"
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}"
                    env.ZIP_NAME = "${env.SERVICE_NAME}-${env.BUILD_NUMBER}.zip"
                    env.APP_NAME = "${env.SERVICE_NAME}-${envLabel}-codedeploy-app"
                    env.DEPLOYMENT_GROUP = "${env.SERVICE_NAME}-${envLabel}-deployment-group"
                }
            }
        }

        stage('Notify Before Start') {
            when {
                expression { env.BRANCH in ['main', 'dev'] }
            }
            steps {
                script {
                    withCredentials([string(credentialsId: 'Discord-Webhook', variable: 'DISCORD')]) {
                        discordSend(
                            description: "🚀 빌드가 시작됩니다: ${env.SERVICE_NAME} - ${env.BRANCH} 브랜치",
                            link: env.BUILD_URL,
                            title: "빌드 시작",
                            webhookURL: "$DISCORD"
                        )
                    }
                }
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
        
        stage('Build JAR') {
            steps {
                sh './gradlew clean build -x test'
            }
        }

        stage('Docker Build & Push to ECR') {
            steps {
                withAWS(credentials: 'aws-access-key', region: "${env.AWS_REGION}") {
                    sh """
                    aws ecr get-login-password | docker login --username AWS --password-stdin ${env.ECR_REPO}
                    docker build -t ${env.ECR_REPO}:${env.IMAGE_TAG} .
                    docker push ${env.ECR_REPO}:${env.IMAGE_TAG}
                    """
                }
            }
        }

        stage('Package for CodeDeploy') {
            steps {
                sh """
                #!/bin/bash
                set -e

                mkdir -p deploy/scripts
                mkdir -p deploy/promtail/
                cp -r appspec.yml deploy/
                cp -r scripts/* deploy/scripts/
                cp -r promtail deploy/promtail/
                echo ${env.IMAGE_TAG} > deploy/.image_tag
                cd deploy && zip -r ../${env.ZIP_NAME} .
                """
            }
        }

        stage('Upload to S3') {
            steps {
                withAWS(credentials: 'aws-access-key', region: "${env.AWS_REGION}") {
                    sh "aws s3 cp ${env.ZIP_NAME} s3://${env.S3_BUCKET}/${env.ZIP_NAME}"
                }
            }
        }

        stage('Trigger CodeDeploy') {
            steps {
                withAWS(credentials: 'aws-access-key', region: "${env.AWS_REGION}") {
                    sh """
                    aws deploy create-deployment \
                      --application-name ${env.APP_NAME} \
                      --deployment-group-name ${env.DEPLOYMENT_GROUP} \
                      --s3-location bucket=${env.S3_BUCKET},bundleType=zip,key=${env.ZIP_NAME} \
                      --file-exists-behavior OVERWRITE
                    """
                }
            }
        }
    }

    post {
        success {
            script {
                if (env.BRANCH in ['main', 'dev']) {
                    withCredentials([string(credentialsId: 'Discord-Webhook', variable: 'DISCORD')]) {
                        discordSend description: """
                        제목 : ${env.SERVICE_NAME}-${currentBuild.displayName} 빌드
                        실행 시간 : ${currentBuild.duration / 1000}s
                        """,
                        link: env.BUILD_URL, result: currentBuild.currentResult,
                        title: "${env.JOB_NAME} : ${currentBuild.displayName} 성공",
                        webhookURL: "$DISCORD"
                    }
                }
            }
        }
        failure {
            script {
                if (env.BRANCH in ['main', 'dev']) {
                    withCredentials([string(credentialsId: 'Discord-Webhook', variable: 'DISCORD')]) {
                        discordSend description: """
                        제목 : ${env.SERVICE_NAME}-${currentBuild.displayName} 빌드
                        실행 시간 : ${currentBuild.duration / 1000}s
                        """,
                        link: env.BUILD_URL, result: currentBuild.currentResult,
                        title: "${env.JOB_NAME} : ${currentBuild.displayName} 실패",
                        webhookURL: "$DISCORD"
                    }
                }
            }
        }
    }
}
