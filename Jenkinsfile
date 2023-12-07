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
        stage('Building our image') {
            steps {
                script {
                    sh "cp /var/lib/jenkins/workspace/myblog-boot/build/libs/myblog-boot-0.0.1-SNAPSHOT.war /var/lib/jenkins/workspace/pipeline/" // war 파일을 현재 위치로 복사 
                    // Docker 빌드
                    dockerImage = docker.build repository + ":$BUILD_NUMBER"
                }
                echo 'building the application...'
            }
        }
        stage('Deploy our image') { 
          steps { 
              script {
                sh 'docker push $repository:$BUILD_NUMBER' //docker push
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