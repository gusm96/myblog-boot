pipeline {
    environment {
        repository = "seongmogu/myblog-boot"
        DOCKERHUB_CREDENTIALS = credentials("myblog-boot")
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
        stage('Login'){
            steps {
                bat "echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin"
            }
        }
        stage('Deploy our image') { 
          steps { 
              script {
                bat "docker push $repository:$BUILD_NUMBER" //docker push
              } 
          }
        } 
    }
}