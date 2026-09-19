# Build stage: dependencies are resolved in their own layer so that editing source
# code does not re-download the world on every rebuild.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Running as root inside the container would mean a container escape lands as root
# on the VPS too.
RUN addgroup -S app && adduser -S -G app app

COPY --from=build /build/target/*.jar app.jar

# Photos live on a volume mounted here; the app user has to own it to write new ones.
RUN mkdir -p /data/place-photos && chown -R app:app /data
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
