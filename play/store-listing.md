# tAlarm — Google Play store listing (draft)

Package: `com.tim.loudalarm` · Category: **Tools** · Ads: **No** · In-app purchases: **No** · Data collected: **None**
Contact email: **timbogdeu2@gmail.com** · Privacy policy: **[GitHub Pages URL — заполнить после хостинга play/privacy-policy.html]**

> Правило Google: НИКАКОГО рекламного текста в заголовке/описании/на графике («#1», «Best», «Top», «New», «Sale» и т.п. — запрещены). Ниже уже без них.

---

## English (en-US)

**Title** (≤30): `tAlarm — Loud Alarm & Sleep`

**Short description** (≤80):
`A very loud alarm that truly wakes you up, plus a sleep-cycle calculator.`

**Full description** (≤4000):
```
tAlarm is a no-nonsense alarm clock built around one job: actually waking you up.

LOUD — and louder
• Plays on the alarm channel at full volume, bypassing silent mode.
• A separate volume-boost slider pushes the sound well past the normal maximum for heavy sleepers.
• Four attention-grabbing sounds (air-raid siren, loud siren, sci-fi siren, car horn).

Wake-up challenges (so you can't just swipe it off)
• Button — simple tap to dismiss.
• Solve a problem — single-digit addition/subtraction or multiplication. Tip: touching the answer field silences the sound so you can think in peace, while the phone keeps vibrating.
• Shake the phone — set how many shakes it takes to turn off.

Sleep-cycle calculator
• "I'm going to bed at X" → the best times to wake up.
• "I want to wake at X" → the best times to go to bed.
• Based on ~90-minute sleep cycles (plus time to fall asleep); 5–6 cycles are marked as optimal.
• Tap any suggested time to create an alarm for it instantly.

More
• Repeat by days, with quick "Every day / Weekdays / Weekends" presets.
• Snooze with a chosen number of minutes.
• Light and dark themes with a few accent colors.
• English and Russian.

Private by design
• Works fully offline. No account, no sign-in.
• No ads. No analytics. No data collected or shared.

Note: to guarantee your alarm always fires on time, allow "Alarms & reminders" and, on some phones (e.g. Samsung), exclude tAlarm from battery optimization / "sleeping apps".
```

**Category:** Tools
**Tags (max 5, pick from Google's suggested list):** Alarm clock, Clock, Sleep, Productivity, Utilities

---

## Русский (ru-RU)

**Заголовок** (≤30): `tAlarm — громкий будильник`

**Краткое описание** (≤80):
`Очень громкий будильник, который точно разбудит, и калькулятор циклов сна.`

**Полное описание** (≤4000):
```
tAlarm — будильник с одной честной задачей: реально тебя разбудить.

ГРОМКО — и ещё громче
• Играет по каналу будильника на полной громкости, в обход беззвучного режима.
• Отдельный ползунок усиления поднимает звук заметно выше обычного максимума — для крепко спящих.
• Четыре пробивных сигнала (воздушная тревога, громкая сирена, космическая сирена, клаксон).

Задания на пробуждение (чтобы не смахнуть спросонья)
• Кнопкой — простое выключение.
• Решить пример — одноцифровое сложение/вычитание или умножение. Фишка: как только касаешься поля ответа, звук смолкает, чтобы считать в тишине, а телефон продолжает вибрировать.
• Потрясти телефон — задай, сколько встряхиваний нужно для выключения.

Калькулятор циклов сна
• «Ложусь в X» → лучшее время для подъёма.
• «Встать в X» → лучшее время лечь.
• На основе циклов сна ~90 минут (плюс время на засыпание); 5–6 циклов помечены как оптимальные.
• Тап по любому времени сразу создаёт будильник на него.

Ещё
• Повтор по дням с быстрыми пресетами «Все дни / Будни / Выходные».
• Отложить на выбранное число минут.
• Светлая и тёмная темы, несколько акцентных цветов.
• Русский и английский.

Приватность по умолчанию
• Работает полностью офлайн. Без аккаунта и входа.
• Без рекламы. Без аналитики. Данные не собираются и не передаются.

Важно: чтобы будильник всегда срабатывал вовремя, разреши «Будильники и напоминания», а на некоторых телефонах (например, Samsung) исключи tAlarm из оптимизации батареи / «спящих приложений».
```

---

## Обоснования разрешений (для форм-деклараций в Play Console)

**USE_FULL_SCREEN_INTENT** (обязательная декларация, действует с 22.01.2025):
> tAlarm is an alarm clock. It uses a full-screen intent to show the ringing-alarm screen over the lock screen so the user can see and dismiss the alarm on time. This is the app's core alarm functionality.

**Foreground service — FOREGROUND_SERVICE_MEDIA_PLAYBACK** (декларация типа FGS; готовим короткое демо-видео срабатывания):
> A foreground service plays the alarm sound while an alarm is ringing. It starts only when a user-scheduled alarm fires and stops when the alarm is dismissed or snoozed. It is not used for background media playback. Demo video shows an alarm ringing and the foreground playback.

**USE_EXACT_ALARM / SCHEDULE_EXACT_ALARM (maxSdk 32):**
> Core alarm-clock functionality: alarms must fire at the exact time set by the user, including in Doze. Uses AlarmManager.setAlarmClock.

**REQUEST_IGNORE_BATTERY_OPTIMIZATIONS** (пограничное — обоснование, т.к. решили оставить):
> Requested (optional, user can decline) so aggressive OEM battery optimizers do not delay or kill scheduled alarms. Reliable, on-time alarms are the app's primary purpose; without this exemption some devices silently drop alarms. The app does no other background work.

## App content declarations (быстрые ответы)
- App access: **All functionality is available without special access** (нет входа/аккаунта).
- Ads: **No**.
- Target audience: **13+ / adults** (НЕ отмечать «дети до 13»).
- Data safety: **No data collected, no data shared** (ссылка на privacy policy).
- Content rating (IARC): будильник без спорного контента → Everyone / низкий возраст.
- Financial features: **None**. News: **No**. Government/COVID: **No**. Health: **No**.
