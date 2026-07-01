insert into bot_texts (message_key, language, text_value) values
('auto_quality_fallback', 'RU', 'Не удалось заранее проверить качества. Попробую автоматически подобрать качество, которое поместится в Telegram.'),
('auto_quality_fallback', 'EN', 'Could not pre-check qualities. I will automatically try to pick a quality that fits Telegram.')
on conflict (message_key, language) do update set text_value = excluded.text_value;
