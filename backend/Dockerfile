FROM maven:3.9.9-eclipse-temurin-17@sha256:f58d59b6273e785ac0a4477f6e9b5ba1d7731c75b906c0f7b34076f1851318cc AS build
WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp package

FROM eclipse-temurin:17-jre-jammy@sha256:89e68b9bb83713510b63e2059a415792a7fc77e14b739a7d7ede97f6d9ca2c38
WORKDIR /app
RUN groupadd --gid 10001 companion \
    && useradd --no-log-init --uid 10001 --gid companion --create-home companion \
    && mkdir -p /app/generated-images \
    && chown companion:companion /app/generated-images
COPY --from=build --chown=companion:companion /workspace/target/ai-virtual-companion-1.0.0.jar /app/app.jar
USER companion
EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
