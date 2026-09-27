# Ward для Windows и Linux

Настольный клиент на Compose Desktop. Первая версия работает системным прокси:
Xray поднимает SOCKS и HTTP на 127.0.0.1, приложение прописывает их в настройках
системы и возвращает прежние при отключении, выходе и после аварийного завершения.
Туннеля (TUN) пока нет - см. `docs/DESKTOP.md`.

## Сборка

```
# ядро и geo-файлы для своей системы - в resources/<windows|linux>
./gradlew run              # запустить из исходников
./gradlew test             # тесты; с бинарником xray проверяется и запуск ядра
./gradlew packageMsi       # Windows: .msi (и packageExe)
./gradlew packageDeb       # Linux: .deb
```

CI: `.github/workflows/desktop.yml`. Сборка идёт на каждый push в `desktop/`;
релиз - запуском вручную с тегом вида `desktop-v1.0.0`. Настольный релиз никогда
не становится «последним»: по `/releases/latest` проверяет обновления Android.

## Откуда код

`src/main/kotlin/com/v2ray/ang/` - **копии** файлов из `V2rayNG/app/src/main/java/com/v2ray/ang/`.
Исправили что-то там в разборе ссылок, подписках или сборке конфига - перенесите
сюда: `diff -r` по этим папкам покажет, где копии разошлись.

Скопированы как есть: `AppConfig`, `fmt/`, `enums/`, `dto/` (часть), `extension/StringExt`,
`ListExt`, `util/JsonUtil`, `CustomConfigUtil`, `LogUtil`,
`core/CoreConfigManager`, `CoreOutboundBuilder`, `CoreConfigContextBuilder`,
`handler/MmkvManager`, `SettingsChangeManager`, `SubscriptionHeaders`,
`ui/compose/GlassSurface`, `InnerEdge`, `LiquidBackground`, `LiquidPowerButton`.

С правками (каждая помечена комментарием «Настольная версия»):

- `util/Utils.kt` - буфер обмена, браузер, ассеты и пути;
- `util/HttpUtil.kt` - система в User-Agent;
- `handler/AngConfigManager.kt` - без QR, предупреждений о лимите и
  фонового планировщика, x-device-os берётся из системы;
- `handler/SettingsManager.kt` - режим всегда «прокси», geo-файлы из установки;
- `extension/_Ext.kt` - без функций для Bundle и Intent.

Своё, настольное:

- `android/` и `com/tencent/mmkv/MMKV.kt` - заглушки: Base64, Log, TextUtils,
  Context и хранилище MMKV поверх JSON-файлов;
- `util/DeviceInfo.kt`, `util/PackageUidResolver.kt`, `handler/LogFileManager.kt`;
- `ui/compose/Theme.kt` - палитры взяты из Android, сборка темы своя;
- `com/ward/desktop/` - окно, трей, запуск ядра, системный прокси.

## Где лежат данные

Windows - `%APPDATA%\Ward`, Linux - `~/.local/share/ward`. Внутри `store/`
(подписки, серверы, настройки), `logs/` (журнал приложения и ядра) и `assets/`.
