# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies first (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -S tradeforge && adduser -S tradeforge -G tradeforge

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown tradeforge:tradeforge app.jar

USER tradeforge

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]