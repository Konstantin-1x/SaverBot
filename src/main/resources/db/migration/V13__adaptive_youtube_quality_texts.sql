update bot_texts
set text_value = 'Привет! Я помогу скачать видео из YouTube, Instagram и TikTok.
Просто отправь мне ссылку на видео, а я пришлю тебе файл.
Для обычных видео YouTube можно выбрать качество.
Для YouTube Shorts сначала используется 1080p, затем качество снижается только при превышении лимита Telegram.'
where message_key = 'start' and language = 'RU';

update bot_texts
set text_value = 'Hi! I can download videos from YouTube, Instagram and TikTok.
Send me a video link and I will send the file back.
For regular YouTube videos you can choose quality.
YouTube Shorts start at 1080p and fall back only when the Telegram size limit is exceeded.'
where message_key = 'start' and language = 'EN';

update bot_texts
set text_value = 'Как пользоваться ботом:
1. Отправь ссылку на видео.
2. Для обычного YouTube выбери качество.
3. Дождись готового видеофайла.

YouTube Shorts загружаются в максимально доступном качестве, которое помещается в лимит Telegram.'
where message_key = 'help' and language = 'RU';

update bot_texts
set text_value = 'How to use the bot:
1. Send a video link.
2. Choose quality for regular YouTube videos.
3. Receive the video file.

YouTube Shorts use the highest available quality that fits the Telegram size limit.'
where message_key = 'help' and language = 'EN';
