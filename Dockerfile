FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy only the built JAR from Jenkins
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]