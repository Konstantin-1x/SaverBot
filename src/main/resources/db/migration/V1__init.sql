create table users (
    id bigserial primary key,
    telegram_id bigint not null unique,
    username varchar(255),
    first_name varchar(255),
    last_name varchar(255),
    language_code varchar(16),
    interface_language varchar(16) not null default 'RU',
    is_blocked boolean not null default false,
    role varchar(32) not null default 'USER',
    created_at timestamptz not null,
    last_active_at timestamptz not null
);

create table download_requests (
    id bigserial primary key,
    user_id bigint not null references users(id),
    source_url text not null,
    normalized_url text not null,
    platform varchar(32) not null,
    media_type varchar(32) not null,
    selected_quality varchar(32),
    status varchar(32) not null,
    telegram_chat_id bigint,
    loading_message_id integer,
    error_message text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    completed_at timestamptz
);

create table stored_files (
    id bigserial primary key,
    user_id bigint references users(id),
    download_request_id bigint references download_requests(id),
    normalized_url text not null,
    platform varchar(32) not null,
    media_type varchar(32) not null,
    selected_quality varchar(32),
    file_path text not null,
    file_size bigint not null default 0,
    telegram_file_id text,
    created_at timestamptz not null,
    expires_at timestamptz not null,
    deleted_at timestamptz,
    status varchar(32) not null
);

create index idx_stored_files_cache
    on stored_files (normalized_url, media_type, selected_quality, status, expires_at);

create table download_jobs (
    id bigserial primary key,
    request_id bigint not null references download_requests(id),
    status varchar(32) not null,
    attempt_count integer not null default 0,
    priority integer not null default 100,
    started_at timestamptz,
    finished_at timestamptz,
    worker_id varchar(128),
    error_code varchar(64),
    error_details text
);

create index idx_download_jobs_status_priority on download_jobs (status, priority, id);

create table platform_settings (
    id bigserial primary key,
    platform varchar(32) not null unique,
    is_enabled boolean not null default true,
    max_file_size_mb integer not null default 50,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table error_logs (
    id bigserial primary key,
    user_id bigint references users(id),
    request_id bigint references download_requests(id),
    error_code varchar(64) not null,
    error_message text not null,
    stack_trace text,
    created_at timestamptz not null
);

create table admin_actions (
    id bigserial primary key,
    admin_user_id bigint references users(id),
    action_type varchar(64) not null,
    target_user_id bigint references users(id),
    description text,
    created_at timestamptz not null
);

insert into platform_settings (platform, is_enabled, max_file_size_mb, created_at, updated_at)
values
    ('YOUTUBE', true, 50, now(), now()),
    ('YOUTUBE_SHORTS', true, 50, now(), now()),
    ('INSTAGRAM', true, 50, now(), now()),
    ('TIKTOK', true, 50, now(), now());
