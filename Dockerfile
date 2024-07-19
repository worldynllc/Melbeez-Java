# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-alpine
# Set the working directory in the container
WORKDIR /app
# Copy the application JAR file
COPY target/feeder-0.0.1-SNAPSHOT.jar app.jar
# Expose the port that the application runs on
EXPOSE 8083
# Set environment variables (these should be provided via docker-compose)
COPY .env /app/
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

