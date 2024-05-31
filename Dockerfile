# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the application JAR file
COPY target/feeder-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the application runs on
EXPOSE 8083

# Set environment variables (these should be provided via docker-compose)
ENV AWS_S3_KEY=${AWS_S3_KEY}
ENV AWS_S3_SECRET=${AWS_S3_SECRET}
ENV AWS_S3_BUCKET=${AWS_S3_BUCKET}
ENV AWS_REGION=${AWS_REGION}
ENV MEDIA_STORE=${MEDIA_STORE}
ENV SERVER_PORT=${SERVER_PORT}
ENV SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}
ENV SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}
ENV CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
