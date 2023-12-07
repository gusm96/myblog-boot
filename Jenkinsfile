pipeline {
    environment {
        repository = "seongmogu/myblog-boot"
        dockerImage = ''
    }
    agent any
    stages {
        stage('Build') {
            steps {
                // Gradle build
                bat './gradlew build'
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
                        bat "docker push $repository:$BUILD_NUMBER"
                    }
                }
            }
        }
    }
}