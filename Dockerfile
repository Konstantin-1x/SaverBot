FROM eclipse-temurin:17-jre-alpine@sha256:02320dd4ce20e243dfb915c686089cf9315c763084fafbb12d5c9993aee18b57

WORKDIR /app
RUN apk add --no-cache python3 py3-pip ffmpeg ca-certificates curl \
    && python3 -m venv /opt/yt-dlp \
    && /opt/yt-dlp/bin/pip install --no-cache-dir "yt-dlp[default,curl-cffi]==2026.6.9"

ENV PATH="/opt/yt-dlp/bin:${PATH}"

COPY build/libs/*.jar app.jar
RUN addgroup -S -g 10001 app \
    && adduser -S -D -u 10001 -G app -h /app -s /sbin/nologin app \
    && mkdir -p /app/tmp /app/storage /app/logs \
    && chown -R app:app /app

USER app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
