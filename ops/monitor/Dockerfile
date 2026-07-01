FROM alpine:3.22

RUN apk add --no-cache curl postgresql16-client tzdata

COPY monitor.sh /usr/local/bin/monitor.sh
RUN chmod 0555 /usr/local/bin/monitor.sh \
    && addgroup -S -g 10003 monitor \
    && adduser -S -D -u 10003 -G monitor -h /tmp -s /sbin/nologin monitor

USER monitor
ENTRYPOINT ["/usr/local/bin/monitor.sh"]

