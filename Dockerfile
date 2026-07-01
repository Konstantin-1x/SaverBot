FROM eclipse-temurin:25-jre-alpine@sha256:28db6fdf60e38945e43d840c0333aeaec66c15943070104f7586fd3c9d1665b0

WORKDIR /app
RUN apk add --no-cache python3 py3-pip ffmpeg ca-certificates curl deno \
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
