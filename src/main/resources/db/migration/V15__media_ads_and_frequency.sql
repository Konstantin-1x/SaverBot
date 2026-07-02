alter table ad_settings
    add column media_type varchar(16),
    add column telegram_file_id text,
    add column frequency integer not null default 1,
    add constraint chk_ad_settings_frequency check (frequency between 1 and 100000),
    add constraint chk_ad_settings_media check (
        (media_type is null and telegram_file_id is null)
        or (media_type in ('PHOTO', 'VIDEO') and telegram_file_id is not null)
    );

alter table users
    add column completed_downloads bigint not null default 0,
    add constraint chk_users_completed_downloads check (completed_downloads >= 0);
