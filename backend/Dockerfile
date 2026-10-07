# Build stage: dependencies are resolved in their own layer so that editing source
# code does not re-download the world on every rebuild.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# No la variante alpine: esa sale solo para x86, y el servidor de Oracle es ARM
# (Ampere). Con alpine el build fallaba con "no match for platform in manifest".
FROM eclipse-temurin:17-jre
WORKDIR /app

# Running as root inside the container would mean a container escape lands as root
# on the VPS too.
RUN groupadd --system app && useradd --system --gid app --no-create-home --shell /usr/sbin/nologin app

COPY --from=build /build/target/*.jar app.jar

# Photos live on volumes mounted here; the app user has to own them to write new ones.
# Docker copies this ownership into a volume the first time it is mounted.
RUN mkdir -p /data/place-photos /data/rating-photos && chown -R app:app /data
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
