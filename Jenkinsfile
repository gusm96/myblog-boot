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
                bat './gradlew build -x test'
            }
        }
        stage('Docker Hub Login and build image'){
            steps{
                script{
                    withDockerRegistry([credentialsId: "docker-hub", url: "https://index.docker.io/v1/"]) {
                        // Docker build image
                        dockerImage = docker.build(repository + ":$BUILD_NUMBER")
                    }
                }
            }
        }
        stage('Docker Login and Push'){
            steps {
                script {
                     bat "docker push $repository:$BUILD_NUMBER"
                }
            }
        }
    }
}