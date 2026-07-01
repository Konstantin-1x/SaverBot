FROM alpine:3.22

RUN apk add --no-cache curl openssl postgresql16-client rclone tzdata

COPY backup.sh /usr/local/bin/backup.sh
COPY restore-check.sh /usr/local/bin/restore-check.sh
RUN chmod 0555 /usr/local/bin/backup.sh /usr/local/bin/restore-check.sh \
    && addgroup -S -g 10002 backup \
    && adduser -S -D -u 10002 -G backup -h /backups -s /sbin/nologin backup

USER backup
ENTRYPOINT ["/usr/local/bin/backup.sh"]
