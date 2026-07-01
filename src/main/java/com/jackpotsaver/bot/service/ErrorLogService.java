package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.DownloadRequest;
import com.jackpotsaver.bot.domain.ErrorLog;
import com.jackpotsaver.bot.domain.User;
import com.jackpotsaver.bot.repository.ErrorLogRepository;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ErrorLogService {
    private static final Logger log = LoggerFactory.getLogger(ErrorLogService.class);
    private final ErrorLogRepository repository;
    private final Clock clock;

    public ErrorLogService(ErrorLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(User user, DownloadRequest request, String code, String message, Throwable throwable) {
        log.warn("{}: {}", code, message, throwable);
        repository.save(new ErrorLog(user, request, code, message, stackTrace(throwable), clock.instant()));
    }

    private String stackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
