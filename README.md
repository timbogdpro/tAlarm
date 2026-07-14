# tAlarm

**English** · [Версия на русском языке ↓](#talarm---громкий-будильник-с-калькулятором-циклов-сна)

A no-nonsense **very loud** alarm clock for Android, built to actually wake you up — plus a sleep-cycle calculator. Native Kotlin, offline, no ads, no trackers, no accounts.

- **minSdk 26** (Android 8.0+) · **targetSdk 36** (Android 16)
- Package: `com.tim.loudalarm`
- License: MIT (see [LICENSE](LICENSE))

## Features

**Loud — and louder**
- Plays on the alarm audio channel at full volume, bypassing silent / Do-Not-Disturb.
- A separate volume-**boost** slider (LoudnessEnhancer) pushes well past the normal maximum for heavy sleepers.
- Four attention-grabbing sounds.

**Wake-up challenges** (so you can't just swipe it off half-asleep)
- **Button** — simple tap to dismiss.
- **Solve a problem** — single-digit addition/subtraction or multiplication. Touching the answer field silences the sound so you can think in peace, while the phone keeps vibrating.
- **Shake the phone** — choose how many shakes it takes.

**Sleep-cycle calculator**
- "I go to bed at X" → the best times to wake up.
- "I want to wake at X" → the best times to go to bed.
- Based on ~90-minute cycles (plus time to fall asleep); 5–6 cycles are marked optimal.
- Tap any suggested time to create an alarm instantly.

**More**
- Repeat by days with "Every day / Weekdays / Weekends" presets.
- Snooze for a chosen number of minutes.
- Light & dark themes with a few accent colors.
- English and Russian.
- Rings **over the lock screen**, survives reboot (re-schedules alarms on boot).

**Private by design** — works fully offline, no account, no analytics, no ads, and it collects/sends **no data** (the app has no `INTERNET` permission).

## Install

**Direct APK (sideload)**
1. Download the latest `talarm-*.apk` from the [Releases](../../releases) page.
2. Open it on your phone; when prompted, allow "Install unknown apps" for your browser/file manager.
3. Open the app and grant the permissions it asks for (notifications, exact alarms, display over lock screen; optionally exclude it from battery optimization for reliable ringing).

**Auto-updates via [Obtainium](https://github.com/ImranR98/Obtainium)** (recommended)
- Install Obtainium, tap *Add App*, paste this repository URL, and Obtainium will track new releases and update automatically.

## Build from source

```bash
# Android SDK (platform 36 + build-tools 36), JDK 17, Gradle 8.7
./gradlew assembleDebug        # debug APK  -> app/build/outputs/apk/debug/
./gradlew assembleRelease      # signed release APK (needs keystore.properties, see below)
```

Release signing reads secrets from a `keystore.properties` file (git-ignored). Copy `keystore.properties.template` to `keystore.properties` and fill in your own keystore path and passwords.

## Tech

Native Android, Kotlin, View Binding, Material Components 3. No Google Play Services, no Firebase.
Alarms use `AlarmManager.setAlarmClock` (exact, Doze-exempt); the ringing screen is a full-screen-intent activity shown over the lock screen; playback runs in a foreground service.

```
app/src/main/java/com/tim/loudalarm/
  Alarm.kt              alarm model + dismiss modes + math type
  Sounds.kt             bundled ringtone registry
  AlarmStore.kt         persistence (JSON in SharedPreferences)
  AlarmScheduler.kt     scheduling via AlarmManager (exact alarms)
  AlarmReceiver.kt      fires the alarm, starts the service, re-schedules
  BootReceiver.kt       restores alarms after reboot
  AlarmService.kt       loud playback + notification + vibration
  MainActivity.kt       host: tabs (alarms + sleep) + permissions + onboarding
  AlarmsFragment.kt     alarm list
  SleepFragment.kt      sleep-cycle calculator
  SleepCalculator.kt    pure cycle math (unit-testable)
  EditAlarmActivity.kt  alarm editor
  AlarmActivity.kt      the "ringing" screen + 3 dismiss modes
```

## Credits & license

Application code is MIT-licensed. Bundled alarm sounds are third-party assets under their own licenses — see [CREDITS.md](CREDITS.md).

---

## tAlarm - громкий будильник с калькулятором циклов сна

Очень **громкий** будильник для Android, который реально разбудит, плюс калькулятор циклов сна. Нативный Kotlin, работает офлайн, без рекламы, трекеров и аккаунтов.

- **minSdk 26** (Android 8.0+) · **targetSdk 36** (Android 16)
- Пакет: `com.tim.loudalarm`
- Лицензия: MIT (см. [LICENSE](LICENSE))

### Возможности

**Громко — и ещё громче**
- Играет по каналу будильника на полной громкости, в обход «без звука» / «Не беспокоить».
- Отдельный ползунок **усиления** (LoudnessEnhancer) поднимает звук выше обычного максимума — для крепко спящих.
- Четыре пробивных сигнала.

**Задания на пробуждение** (чтобы не смахнуть спросонья)
- **Кнопкой** — простое выключение.
- **Решить пример** — одноцифровое сложение/вычитание или умножение. Касание поля ответа глушит звук, чтобы считать в тишине, а телефон продолжает вибрировать.
- **Потрясти телефон** — задаёшь число встряхиваний.

**Калькулятор циклов сна**
- «Ложусь в X» → когда лучше встать.
- «Встать в X» → когда лучше лечь.
- На основе циклов ~90 минут (плюс время на засыпание); 5–6 циклов помечены оптимальными.
- Тап по времени сразу создаёт будильник.

**Ещё**
- Повтор по дням с пресетами «Все дни / Будни / Выходные».
- Отложить (snooze) на выбранное число минут.
- Светлая и тёмная темы, несколько акцентных цветов.
- Русский и английский.
- Звонок **поверх блокировки**, восстановление будильников после перезагрузки.

**Приватность по умолчанию** — работает офлайн, без аккаунта, аналитики и рекламы, данные не собираются и не передаются (у приложения нет разрешения `INTERNET`).

### Установка и сборка

Скачай `talarm-*.apk` со страницы [Releases](../../releases), открой на телефоне, разреши установку из неизвестных источников. Для автообновлений — [Obtainium](https://github.com/ImranR98/Obtainium). Сборка из исходников — см. английскую секцию (`./gradlew assembleDebug`).

### Лицензия

Код приложения — под MIT. Встроенные звуки — сторонние ассеты под своими лицензиями (см. [CREDITS.md](CREDITS.md)).
