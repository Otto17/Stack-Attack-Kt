// Copyright (c) 2026 Otto
// License: GPL-2.0-or-later (see LICENSE)

package com.example.stackattackkt

import androidx.core.graphics.toColorInt

// ColorTheme Определяет доступные цветовые палитры игрового мира
enum class ColorTheme(
    val primaryBg: Int,      // Основной фон
    val secondaryBg: Int,    // Вторичный фон (земля, ящики, кнопки)
    val textStroke: Int,     // Текст и обводки
    val accent: Int          // Шляпа, блик, выделение
) {
    // 1. Оранжевая (по умолчанию)
    CLASSIC(
        "#FF6600".toColorInt(), // Основной фон — ярко-оранжевый
        "#CC5200".toColorInt(), // Вторичный фон — тёмно-оранжевый
        "#1A0A00".toColorInt(), // Текст — почти чёрный
        "#FFCC00".toColorInt()  // Акцент — жёлтый
    ),

    // 2. Сакура
    SAKURA(
        "#F8C8D4".toColorInt(), // Основной фон — нежно-розовый
        "#E8A0B0".toColorInt(), // Вторичный фон — розовый
        "#5C1A2A".toColorInt(), // Текст — тёмно-бордовый
        "#C0392B".toColorInt()  // Акцент — алый
    ),

    // 3. Светлая
    LIGHT(
        "#F0EAD6".toColorInt(), // Основной фон — кремово-бежевый
        "#D6C9A8".toColorInt(), // Вторичный фон — песочный
        "#2C2010".toColorInt(), // Текст — тёмно-коричневый
        "#B06000".toColorInt()  // Акцент — медно-коричневый
    ),

    // 4. Мятная
    MINT(
        "#E8F8F0".toColorInt(), // Основной фон — мятно-белый
        "#A8DCC0".toColorInt(), // Вторичный фон — мятный
        "#1A3A28".toColorInt(), // Текст — тёмно-зелёный
        "#E07830".toColorInt()  // Акцент — оранжевый
    ),

    // 5. Небесная
    SKY(
        "#E8F4FD".toColorInt(), // Основной фон — почти белый с голубинкой
        "#AED6F1".toColorInt(), // Вторичный фон — голубой
        "#1A3A5C".toColorInt(), // Текст — тёмно-синий
        "#E07820".toColorInt()  // Акцент — янтарный
    ),

    // 6. Серо-голубая
    MIST(
        "#7A9AAA".toColorInt(), // Основной фон — серо-голубой
        "#567080".toColorInt(), // Вторичный фон — тёмно-серо-голубой
        "#101820".toColorInt(), // Текст — почти чёрный
        "#F0D060".toColorInt()  // Акцент — золотисто-жёлтый
    ),

    // 7. Закатная
    SUNSET(
        "#C0392B".toColorInt(), // Основной фон — тёмно-красный
        "#922B21".toColorInt(), // Вторичный фон
        "#FDEBD0".toColorInt(), // Текст — кремовый
        "#F39C12".toColorInt()  // Акцент — янтарный
    ),

    // 8. Медная
    COPPER(
        "#C47A45".toColorInt(), // Основной фон — медно-оранжевый
        "#8B4A20".toColorInt(), // Вторичный фон — тёмно-медный
        "#1A0A00".toColorInt(), // Текст — почти чёрный
        "#FFD080".toColorInt()  // Акцент — светло-золотистый
    ),

    // 9. Тёмно-зелёная
    MOSS(
        "#3A5C30".toColorInt(), // Основной фон — мшисто-зелёный
        "#2A4020".toColorInt(), // Вторичный фон — тёмно-зелёный
        "#D8ECC8".toColorInt(), // Текст — светло-зелёный
        "#F0C040".toColorInt()  // Акцент — жёлто-золотистый
    ),

    // 10. Тёмно-синяя
    SLATE(
        "#1C2F40".toColorInt(), // Основной фон — глубокий синий
        "#0F1E2E".toColorInt(), // Вторичный фон — почти чёрно-синий
        "#D0E8FF".toColorInt(), // Текст — голубовато-белый
        "#4FC3F7".toColorInt()  // Акцент — ярко-голубой (вместо жёлтого)
    ),

    // 11. Фиолетовая
    DUSK(
        "#4A2863".toColorInt(), // Основной фон — насыщенный фиолетовый
        "#341A48".toColorInt(), // Вторичный фон — тёмно-фиолетовый
        "#F0E8FF".toColorInt(), // Текст — светло-сиреневый
        "#B39DDB".toColorInt()  // Акцент — лавандовый
    ),

    // 12. Угольно-серая
    NOIR(
        "#1C1C1C".toColorInt(), // Основной фон — почти чёрный
        "#2D2D2D".toColorInt(), // Вторичный фон — тёмно-серый
        "#E0E0E0".toColorInt(), // Текст — светло-серый
        "#C0392B".toColorInt()  // Акцент — красный
    );

    companion object {
        // fromOrdinal Возвращает тему по порядковому номеру или классическую при ошибке
        fun fromOrdinal(index: Int): ColorTheme {
            return entries.getOrNull(index) ?: CLASSIC
        }
    }
}