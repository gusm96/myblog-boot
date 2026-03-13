pipeline {
    environment {
        repository = "seongmogu/myblog-boot"
        dockerImage = ''
    }
    agent any
    stages {
        stage('Frontend Build') {
            steps {
                dir('frontend') {
                    bat 'npm ci --prefer-offline'
                    bat 'npm run build'
                }
            }
        }
        stage('Backend Build') {
            steps {
                dir('backend') {
                    bat './gradlew build'
                }
            }
        }
        stage('Docker Hub Login and build image') {
            steps {
                script {
                    withDockerRegistry([credentialsId: "docker-hub", url: "https://index.docker.io/v1/"]) {
                        // 백엔드 이미지 빌드
                        dockerImage = docker.build(repository + ":$BUILD_NUMBER", "./backend")
                    }
                }
            }
        }
        stage('Docker hub Push') {
            steps {
                script {
                    bat "docker push $repository:$BUILD_NUMBER"
                }
            }
        }
    }
}
