# Stage 1: Build the app using Maven + Temurin JDK
FROM maven:3.9.2-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom and source
COPY pom.xml .
COPY src ./src

# Build the jar (skip tests to speed up)
RUN mvn clean package -DskipTests

# Stage 2: Run the app using Temurin JDK
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
