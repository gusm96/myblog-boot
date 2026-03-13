FROM openjdk:17
WORKDIR /myblog-boot
VOLUME /tmp
ARG JAR_FILE=./build/libs/*.jar
ADD ${JAR_FILE} myblog-boot.jar
ENTRYPOINT ["java", "-jar", "myblog-boot.jar"]

