alter table download_requests
    add column resource_key varchar(64);

alter table download_requests
    alter column source_url drop not null,
    alter column normalized_url drop not null;

update download_requests request
set resource_key = job.dedup_key
from download_jobs job
where job.request_id = request.id
  and job.dedup_key is not null
  and request.resource_key is null;

update download_requests
set source_url = null,
    normalized_url = null
where status in ('SUCCESS', 'FAILED', 'CACHED');

create index idx_download_requests_resource_key
    on download_requests (resource_key);

drop index if exists idx_stored_files_cache;

alter table stored_files
    drop column user_id,
    drop column normalized_url,
    drop column platform,
    drop column media_type,
    drop column selected_quality;

create index idx_stored_files_cache
    on stored_files (download_request_id, status, expires_at desc);

alter table download_jobs
    add constraint uk_download_jobs_request unique (request_id);

alter table stored_files
    add constraint uk_stored_files_request unique (download_request_id);

alter table download_requests
    add constraint chk_download_requests_resource_key
        check (resource_key is null or resource_key ~ '^[0-9a-f]{64}$');
