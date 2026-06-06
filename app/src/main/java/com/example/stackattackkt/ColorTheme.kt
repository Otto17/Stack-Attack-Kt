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
        "#FF6600".toColorInt(),
        "#CC5200".toColorInt(),
        "#1A0A00".toColorInt(),
        "#FFCC00".toColorInt()
    ),

    // 2. Светлая
    LIGHT(
        "#F0EAD6".toColorInt(),
        "#D6C9A8".toColorInt(),
        "#2C2010".toColorInt(),
        "#B06000".toColorInt()
    ),

    // 3. Тёмно-синяя
    SLATE(
        "#2E3D50".toColorInt(),
        "#1E2D3E".toColorInt(),
        "#C8D8E8".toColorInt(),
        "#E8A030".toColorInt()
    ),

    // 4. Тёмно-зелёная
    MOSS(
        "#3A5C30".toColorInt(),
        "#2A4020".toColorInt(),
        "#D8ECC8".toColorInt(),
        "#F0C040".toColorInt()
    ),

    // 5. Фиолетовая
    DUSK(
        "#3D2850".toColorInt(),
        "#2A1838".toColorInt(),
        "#E8D8F8".toColorInt(),
        "#F0A030".toColorInt()
    ),

    // 6. Медная
    COPPER(
        "#C47A45".toColorInt(),
        "#8B4A20".toColorInt(),
        "#1A0A00".toColorInt(),
        "#FFD080".toColorInt()
    ),

    // 7. Серо-голубая
    MIST(
        "#7A9AAA".toColorInt(),
        "#567080".toColorInt(),
        "#101820".toColorInt(),
        "#F0D060".toColorInt()
    );

    companion object {
        // fromOrdinal Возвращает тему по порядковому номеру или классическую при ошибке
        fun fromOrdinal(index: Int): ColorTheme {
            return entries.getOrNull(index) ?: CLASSIC
        }
    }
}