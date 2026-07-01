update error_logs
set error_message = regexp_replace(
        regexp_replace(error_message, 'bot[0-9]+:[A-Za-z0-9_-]+', 'bot[REDACTED]', 'g'),
        'https?://[^[:space:]]+', '[URL]', 'g')
where error_message is not null;

update error_logs
set stack_trace = regexp_replace(
        regexp_replace(stack_trace, 'bot[0-9]+:[A-Za-z0-9_-]+', 'bot[REDACTED]', 'g'),
        'https?://[^[:space:]]+', '[URL]', 'g')
where stack_trace is not null;
