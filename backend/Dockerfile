FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /workspace
# Bound the build JVMs. Left alone, both the Gradle launcher and the daemon it forks size
# their heaps against the host's memory, which on Docker Desktop is a VM that may have far
# less than the JVM assumes. Compose also builds this image at the same time as the frontend's
# npm install, so the two compete. The failure that follows is opaque: the BuildKit worker is
# killed and the client reports "error reading from server: EOF" with no mention of memory.
# 1g compiles this project with room to spare; it builds under 512m.
ENV GRADLE_OPTS="-Xmx512m -Dorg.gradle.jvmargs=-Xmx1g"
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
RUN gradle clean bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S kji && adduser -S kji -G kji
COPY --from=build /workspace/build/libs/*.jar app.jar
USER kji
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
