package com.jackpotsaver.bot.telegram;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("telegram")
public class TelegramConnectivityState implements HealthIndicator {
    private final Clock clock;
    private final Duration unhealthyAfter;
    private final Instant startedAt;
    private final AtomicReference<Instant> lastSuccess = new AtomicReference<>();
    private final AtomicReference<Instant> firstFailure = new AtomicReference<>();
    private final AtomicInteger consecutiveFailures = new AtomicInteger();

    public TelegramConnectivityState(
            Clock clock,
            @Value("${bot.polling.unhealthy-after-seconds:300}") long unhealthyAfterSeconds) {
        this.clock = clock;
        this.unhealthyAfter = Duration.ofSeconds(unhealthyAfterSeconds);
        this.startedAt = clock.instant();
    }

    public void success() {
        lastSuccess.set(clock.instant());
        firstFailure.set(null);
        consecutiveFailures.set(0);
    }

    public void failure() {
        firstFailure.compareAndSet(null, clock.instant());
        consecutiveFailures.incrementAndGet();
    }

    @Override
    public Health health() {
        Instant now = clock.instant();
        Instant failureSince = firstFailure.get();
        Instant reference = failureSince == null ? startedAt : failureSince;
        boolean prolongedFailure = consecutiveFailures.get() > 0
                && Duration.between(reference, now).compareTo(unhealthyAfter) >= 0;
        Health.Builder builder = prolongedFailure ? Health.down() : Health.up();
        Instant success = lastSuccess.get();
        return builder
                .withDetail("consecutiveFailures", consecutiveFailures.get())
                .withDetail("lastSuccess", success == null ? "not-yet" : success.toString())
                .build();
    }
}

