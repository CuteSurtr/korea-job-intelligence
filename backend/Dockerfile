FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /workspace
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
