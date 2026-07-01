create index idx_telegram_updates_completed_at
    on telegram_updates (completed_at)
    where completed_at is not null;

create index idx_error_logs_created_at on error_logs (created_at);
create index idx_admin_actions_created_at on admin_actions (created_at);
create index idx_download_requests_completed_at
    on download_requests (completed_at)
    where completed_at is not null;
create index idx_download_jobs_finished_at
    on download_jobs (finished_at)
    where finished_at is not null;
create index idx_stored_files_deleted_at
    on stored_files (deleted_at)
    where deleted_at is not null;

