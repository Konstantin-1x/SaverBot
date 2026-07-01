alter table download_jobs
    add column heartbeat_at timestamptz,
    add column next_attempt_at timestamptz;

update download_jobs
set heartbeat_at = started_at
where status = 'RUNNING' and heartbeat_at is null;

create index idx_download_jobs_ready
    on download_jobs (status, next_attempt_at, priority, id);

create index idx_download_jobs_lease
    on download_jobs (status, heartbeat_at);
