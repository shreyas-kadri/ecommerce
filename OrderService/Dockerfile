FROM openjdk:17

WORKDIR /app

COPY target/OrderService-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8098

ENTRYPOINT ["java", "-jar", "app.jar"]