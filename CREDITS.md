# Звуки будильника — источники и лицензии

Все четыре сигнала взяты с Wikimedia Commons и имеют лицензии Public Domain / CC0,
то есть их можно свободно использовать, в том числе в приложении для Google Play,
без обязательной атрибуции. Ниже указано на всякий случай.

Обработка: каждый файл нормализован по громкости (ffmpeg `loudnorm I=-9 TP=-1`),
приведён к 44.1 кГц, стерео, OGG Vorbis, длина обрезана максимум до 12 с.

| Файл в приложении | Оригинал | Лицензия | Источник |
|---|---|---|---|
| `siren.ogg` | Motorsirene – Feuerwehralarm | Public Domain | https://commons.wikimedia.org/wiki/File:Motorsirene_-_Feuerwehralarm.ogg |
| `train_horn.ogg` | The horn of a Bluebird railcar | CC0 | https://commons.wikimedia.org/wiki/File:The_horn_of_a_Bluebird_railcar.ogg |
| `ship_horn.ogg` | Cruise ship Albatros ship horn | CC0 | https://commons.wikimedia.org/wiki/File:Cruise_ship_Albatros_ship_horn.ogg |
| `siren2.ogg` | Siren | Public Domain | https://commons.wikimedia.org/wiki/File:Siren.ogg |

Чтобы заменить любой сигнал своим: положите mp3/wav/ogg в `app/src/main/res/raw/`
под тем же именем (латиницей, без пробелов) и пересоберите.
