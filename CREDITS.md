# Alarm sounds — sources & licenses

The four bundled alarm sounds in `app/src/main/res/raw/` were sourced from
**Wikimedia Commons** and are in the **public domain or under CC0** (no rights
reserved) — free to use, including in published apps, without required attribution.

Each file was processed for the app: loudness-normalized (ffmpeg `loudnorm`),
converted to OGG Vorbis, and trimmed to a short loop.

| File | In-app name (RU / EN) | Source | License |
|---|---|---|---|
| `air_raid_siren.ogg` | Воздушная тревога / Air-raid siren | Wikimedia Commons | Public Domain / CC0 |
| `loud_siren.ogg` | Громкая сирена / Loud siren | Wikimedia Commons | Public Domain / CC0 |
| `fantastic_siren.ogg` | Космическая сирена / Sci-fi siren | Wikimedia Commons | Public Domain / CC0 |
| `car_horn.ogg` | Клаксон / Car horn | Wikimedia Commons | Public Domain / CC0 |

The above reflects the developer's sourcing (Wikimedia Commons public-domain / CC0
audio); files were re-encoded, so they may not byte-match the originals. If you are
a rights holder with any concern about a sound, contact **timbogdeu2@gmail.com**
and it will be addressed promptly.

## Replacing a sound
Drop an `mp3` / `wav` / `ogg` into `app/src/main/res/raw/` under the same file name
(ASCII, no spaces) and rebuild. The registry is in `Sounds.kt`.
