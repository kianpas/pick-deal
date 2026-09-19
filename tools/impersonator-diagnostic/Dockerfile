FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY backend/gradlew ./gradlew
COPY backend/gradle ./gradle
COPY tools/impersonator-diagnostic/build.gradle tools/impersonator-diagnostic/settings.gradle ./
COPY tools/impersonator-diagnostic/src ./src
RUN sed -i 's/\r$//' gradlew
RUN --mount=type=cache,target=/root/.gradle sh ./gradlew --no-daemon --max-workers=2 test installDist

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/build/install/pickdeal-impersonator-diagnostic/ ./
USER 10001:10001
ENTRYPOINT ["java", "-Xms32m", "-Xmx128m", "-cp", "/app/lib/*", "com.pickdeal.diagnostic.ImpersonatorDiagnostic"]
