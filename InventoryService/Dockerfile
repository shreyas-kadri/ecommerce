FROM openjdk:17

WORKDIR /app

COPY target/InventoryService-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8092

ENTRYPOINT ["java", "-jar", "app.jar"]