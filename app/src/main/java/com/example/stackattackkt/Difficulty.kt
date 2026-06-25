// Copyright (c) 2026 Otto
// License: GPL-2.0-or-later (see LICENSE)

package com.example.stackattackkt

// Difficulty Содержит параметры игрового баланса для разных уровней сложности
enum class Difficulty {
    EASY, MEDIUM, HARD, EXTREME;

    // maxCranes Задает предельное количество активных грейферов на экране
    val maxCranes: Int
        get() = when (this) {
            EASY -> 5
            MEDIUM -> 6
            HARD -> 7
            EXTREME -> 8
        }

    // craneSpeedMin Устанавливает нижний порог скорости движения грейферов
    val craneSpeedMin: Float
        get() = when (this) {
            EASY -> 1.5f
            MEDIUM -> 2.0f
            HARD -> 2.8f
            EXTREME -> 3.9f
        }

    // craneSpeedMax Устанавливает верхний порог скорости движения грейферов
    val craneSpeedMax: Float
        get() = when (this) {
            EASY -> 5.5f
            MEDIUM -> 7.0f
            HARD -> 8.5f
            EXTREME -> 11.0f
        }

    // canPushInJump Определяет возможность взаимодействия с ящиком во время прыжк
    val canPushInJump: Boolean
        get() = when (this) {
            EASY, MEDIUM -> true
            HARD, EXTREME -> false
        }

    // canPushChain Разрешает или запрещает передвижение группы ящиков одновременно
    val canPushChain: Boolean
        get() = when (this) {
            EASY, MEDIUM, HARD -> true
            EXTREME -> false
        }

    // maxPushChain Возвращает лимит количества ящиков в толкаемой цепочке
    val maxPushChain: Int
        get() = when (this) {
            EASY -> 3
            MEDIUM -> 2
            HARD -> 2
            EXTREME -> 1
        }
}