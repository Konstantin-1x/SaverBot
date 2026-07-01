update bot_texts
set text_value = 'Привет! Я помогу скачать видео из YouTube, Instagram и TikTok.
Просто отправь мне ссылку на видео, а я пришлю тебе файл.
Видео из YouTube скачиваются в низком качестве, чтобы ускорить обработку.
Для YouTube Shorts, Instagram и TikTok качество выбирается автоматически.'
where message_key = 'start' and language = 'RU';

update bot_texts
set text_value = 'Hi! I can download videos from YouTube, Instagram and TikTok.
Send me a video link and I will send the file back.
YouTube videos are downloaded in low quality to speed up processing.
For YouTube Shorts, Instagram and TikTok quality is selected automatically.'
where message_key = 'start' and language = 'EN';

update bot_texts
set text_value = 'Как пользоваться ботом:
1. Отправь ссылку на видео.
2. Дождись сообщения «Загрузка».
3. Получи готовый видеофайл.

Поддерживаются: YouTube, YouTube Shorts, Instagram и TikTok.
YouTube скачивается в низком качестве без выбора.'
where message_key = 'help' and language = 'RU';

update bot_texts
set text_value = 'How to use the bot:
1. Send a video link.
2. Wait for the "Loading" message.
3. Receive the video file.

Supported: YouTube, YouTube Shorts, Instagram and TikTok.
YouTube is downloaded in low quality without selection.'
where message_key = 'help' and language = 'EN';
