# Сторонние работы

## Kyant0/AndroidLiquidGlass

Эффект жидкого стекла - линза, блики, тени, преломление - берётся из библиотеки
[AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (Kyant0),
подключённой как `io.github.kyant0:backdrop-android`. На ней же сделано приложение,
которое служило референсом.

Высокоуровневых компонентов библиотека не публикует - они лежат в её каталоге
примеров. Поэтому `LiquidToggle`, `LiquidButton`, `LiquidBottomTabs`,
`LiquidBottomTab` и вспомогательные к ним классы перенесены в проект как есть, в
`com/kyant/backdrop/catalog`. Изменения от оригинала минимальные и отмечены на месте:
свой акцентный цвет, признак доступности, отказ от expect/actual там, где платформа
у нас одна.

    Copyright (c) Kyant0
    Licensed under the Apache License, Version 2.0
    http://www.apache.org/licenses/LICENSE-2.0

Полный текст лицензии: https://github.com/Kyant0/AndroidLiquidGlass/blob/master/LICENSE
