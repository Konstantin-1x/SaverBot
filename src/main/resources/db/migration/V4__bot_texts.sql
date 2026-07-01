create table bot_texts (
    id bigserial primary key,
    message_key varchar(128) not null,
    language varchar(16) not null,
    text_value text not null,
    constraint uk_bot_texts_key_language unique (message_key, language)
);

insert into bot_texts (message_key, language, text_value) values
('start', 'RU', 'Привет! Я помогу скачать видео из YouTube, Instagram и TikTok.
Просто отправь мне ссылку на видео, а я пришлю тебе файл.
Для обычных видео YouTube можно выбрать качество.
Для YouTube Shorts, Instagram и TikTok качество выбирается автоматически.'),
('start', 'EN', 'Hi! I can download videos from YouTube, Instagram and TikTok.
Send me a video link and I will send the file back.
For regular YouTube videos you can choose quality.
For YouTube Shorts, Instagram and TikTok quality is selected automatically.'),
('help', 'RU', 'Как пользоваться ботом:
1. Отправь ссылку на видео.
2. Дождись сообщения «Загрузка».
3. Получи готовый видеофайл.

Поддерживаются: YouTube, YouTube Shorts, Instagram и TikTok.
Выбор качества доступен только для обычных видео YouTube.'),
('help', 'EN', 'How to use the bot:
1. Send a video link.
2. Wait for the "Loading" message.
3. Receive the video file.

Supported: YouTube, YouTube Shorts, Instagram and TikTok.
Quality selection is available only for regular YouTube videos.'),
('loading', 'RU', 'Загрузка'),
('loading', 'EN', 'Loading'),
('send_link', 'RU', 'Отправь ссылку на видео из YouTube, Instagram или TikTok.'),
('send_link', 'EN', 'Send a video link from YouTube, Instagram or TikTok.'),
('unsupported', 'RU', 'Эта ссылка не поддерживается.'),
('unsupported', 'EN', 'This link is not supported.'),
('platform_disabled', 'RU', 'Загрузка с этой платформы временно отключена.'),
('platform_disabled', 'EN', 'Downloads from this platform are temporarily disabled.'),
('blocked', 'RU', 'Доступ к боту ограничен.'),
('blocked', 'EN', 'Access to the bot is restricted.'),
('limit', 'RU', 'Лимит загрузок временно исчерпан. Попробуй позже.'),
('limit', 'EN', 'Download limit is temporarily exhausted. Try again later.'),
('choose_quality', 'RU', 'Выбери качество видео:'),
('choose_quality', 'EN', 'Choose video quality:'),
('checking_quality', 'RU', 'Проверяю доступные качества видео...'),
('checking_quality', 'EN', 'Checking available video qualities...'),
('language_changed', 'RU', 'Язык интерфейса изменен.'),
('language_changed', 'EN', 'Interface language changed.'),
('unavailable', 'RU', 'Не удалось скачать видео. Возможно, оно удалено, приватное или недоступно.'),
('unavailable', 'EN', 'Could not download the video. It may be deleted, private or unavailable.'),
('server_error', 'RU', 'Произошла ошибка. Попробуй позже.'),
('server_error', 'EN', 'An error occurred. Try again later.'),
('too_large', 'RU', 'Видео слишком большое для отправки через Telegram.'),
('too_large', 'EN', 'The video is too large to send through Telegram.'),
('no_suitable_quality', 'RU', 'Это видео слишком большое для отправки через Telegram.'),
('no_suitable_quality', 'EN', 'This video is too large to send through Telegram.'),
('quality_check_unavailable', 'RU', 'Не удалось проверить качества: YouTube сейчас не отвечает. Попробуй позже.'),
('quality_check_unavailable', 'EN', 'Could not check available qualities because YouTube is not responding. Try again later.');

update ad_settings set after_download_text = null where after_download_text like 'https://vt.tiktok.com/%';
