update platform_settings
set max_file_size_mb = 48,
    updated_at = now()
where max_file_size_mb > 48;
