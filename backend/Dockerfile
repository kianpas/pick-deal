# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
# Windows 체크아웃의 줄바꿈도 Linux에서 실행할 수 있게 정규화한다.
RUN sed -i 's/\r$//' gradlew

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    sh ./gradlew --no-daemon --max-workers=2 bootJar && \
    cp build/libs/*.jar /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --gid 10001 pickdeal && \
    useradd --uid 10001 --gid pickdeal --no-create-home --shell /usr/sbin/nologin pickdeal

COPY --from=build --chown=10001:10001 /workspace/app.jar ./app.jar
USER 10001:10001

# 힙 외 메모리도 필요하므로 컨테이너 메모리 제한은 별도로 지정한다.
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx768m -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
