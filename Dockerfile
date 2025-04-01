FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/nl-rooster.jar nl-rooster.jar
ENTRYPOINT ["java", "-jar", "nl-rooster.jar"]
