FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
RUN groupadd --system guildmaster && useradd --system --gid guildmaster guildmaster
COPY --from=build /build/target/*.jar app.jar
USER guildmaster

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
