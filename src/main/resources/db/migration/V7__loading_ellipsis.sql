update bot_texts
set text_value = 'Загрузка...'
where message_key = 'loading' and language = 'RU';

update bot_texts
set text_value = 'Loading...'
where message_key = 'loading' and language = 'EN';

update bot_texts
set text_value = replace(text_value, '«Загрузка»', '«Загрузка...»')
where message_key = 'help' and language = 'RU';

update bot_texts
set text_value = replace(text_value, '"Loading"', '"Loading..."')
where message_key = 'help' and language = 'EN';
