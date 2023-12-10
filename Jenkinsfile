pipeline {
    environment {
        repository = "seongmogu/myblog-boot"
        dockerImage = ''
    }
    agent any
    stages {
        stage('Set Execute Permission') {
            steps {
                script {
                    // 실행 권한 설정
                    sh 'chmod +x /var/jenkins_home/workspace/myblog-boot/gradlew'
                }
            }
        }
        stage('Build') {
            steps {
                // Gradle build
                sh './gradlew build -x test'
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    // Docker 빌드
                    dockerImage = docker.build(repository + ":$BUILD_NUMBER")
                }
                echo 'building the application...'
            }
        }
        stage('Docker Login and Push'){
            steps {
                script {
                    withDockerRegistry([credentialsId: "myblog-boot", url: "https://index.docker.io/v1/"]) {
                        sh "docker push $repository:$BUILD_NUMBER"
                    }
                }
            }
        }
    }
}