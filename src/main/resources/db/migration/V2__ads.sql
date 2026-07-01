create table ad_settings (
    id bigint primary key,
    after_download_text text,
    updated_by_user_id bigint references users(id),
    updated_at timestamptz
);

insert into ad_settings (id, after_download_text, updated_at)
values (1, null, now());
