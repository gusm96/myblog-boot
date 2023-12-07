pipeline {
    environment {
        repository = "seongmogu/myblog-boot"
        DOCKERHUB_CREDENTIALS = credentials("myblog-boot")
        dockerImage = ''
    }
    agent any
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                // Gradle build
                sh './gradlew build'
                // Copy the JAR file to the workspace
                sh 'cp build/libs/myblog-boot-0.0.1-SNAPSHOT.jar $WORKSPACE/'
            }
        }
        stage('Building our image') {
            steps {
                script {
                    // Docker 빌드
                    dockerImage = docker.build repository + ":$BUILD_NUMBER"
                }
                echo 'building the application...'
            }
        }
        stage('Deploy our image') { 
          steps { 
              script {
                sh "docker push $repository:$BUILD_NUMBER" //docker push
              } 
          }
        } 
        stage('Cleaning up') { 
            steps { 
                sh "docker rmi $repository:$BUILD_NUMBER" // docker image 제거
            }
        }
    }
}