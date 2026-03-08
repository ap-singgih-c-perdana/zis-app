FROM adoptopenjdk/openjdk11:alpine-jre
MAINTAINER Singgih Perdana "singgihcp88@gmail.com"
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/application.jar"]
