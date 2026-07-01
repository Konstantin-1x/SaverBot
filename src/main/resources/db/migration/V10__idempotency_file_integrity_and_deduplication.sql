create table telegram_updates (
    update_id bigint primary key,
    status varchar(32) not null,
    processing_at timestamptz not null,
    completed_at timestamptz,
    error_message text
);

alter table download_jobs add column dedup_key varchar(64);

create unique index uk_download_jobs_active_dedup
    on download_jobs (dedup_key)
    where dedup_key is not null and status in ('CREATED', 'RUNNING');

create table download_job_subscribers (
    id bigserial primary key,
    job_id bigint not null references download_jobs(id) on delete cascade,
    request_id bigint not null references download_requests(id) on delete cascade,
    created_at timestamptz not null,
    constraint uk_download_job_subscriber_request unique (request_id)
);

create index idx_download_job_subscribers_job on download_job_subscribers(job_id);
