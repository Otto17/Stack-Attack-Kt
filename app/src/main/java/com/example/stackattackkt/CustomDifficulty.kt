// Copyright (c) 2026 Otto
// License: GPL-2.0-or-later (see LICENSE)

package com.example.stackattackkt

import android.content.Context
import androidx.core.content.edit

// CustomDifficulty Хранит параметры пользовательской настройки сложности
data class CustomDifficulty(
    val canPushInJump: Boolean,   // Разрешено ли толкать ящик в прыжке
    val maxPushChain: Int,        // Максимальное число толкаемых ящиков (1..3)
    val maxCranes: Int,           // Максимум грейферов на экране (4..9)
    val craneSpeedLevel: Int      // Уровень скорости грейферов (1..5)
) {
    // craneSpeedMin Нижний порог скорости в зависимости от уровня
    val craneSpeedMin: Float
        get() = when (craneSpeedLevel) {
            1 -> 0.8f
            2 -> 1.5f
            3 -> 2.0f
            4 -> 2.8f
            5 -> 3.8f
            else -> 2.0f
        }

    // craneSpeedMax Верхний порог скорости в зависимости от уровня
    val craneSpeedMax: Float
        get() = when (craneSpeedLevel) {
            1 -> 3.0f
            2 -> 5.5f
            3 -> 7.0f
            4 -> 8.5f
            5 -> 10.5f
            else -> 7.0f
        }

    // canPushChain Разрешает толкание цепочки ящиков если лимит больше 1
    val canPushChain: Boolean
        get() = maxPushChain > 1

    companion object {
        // DEFAULTS Параметры по умолчанию (средний уровнь сложности)
        val DEFAULTS = CustomDifficulty(
            canPushInJump = true,
            maxPushChain = 2,
            maxCranes = 5,
            craneSpeedLevel = 3
        )

        private const val KEY_CAN_PUSH_JUMP = "custom_can_push_jump"
        private const val KEY_MAX_PUSH_CHAIN = "custom_max_push_chain"
        private const val KEY_MAX_CRANES = "custom_max_cranes"
        private const val KEY_SPEED_LEVEL = "custom_speed_level"

        // load Загружает кастомные параметры из SharedPreferences
        fun load(context: Context): CustomDifficulty {
            val prefs = context.getSharedPreferences("stack_attack_prefs", Context.MODE_PRIVATE)
            return CustomDifficulty(
                canPushInJump = prefs.getBoolean(KEY_CAN_PUSH_JUMP, DEFAULTS.canPushInJump),
                maxPushChain = prefs.getInt(KEY_MAX_PUSH_CHAIN, DEFAULTS.maxPushChain),
                maxCranes = prefs.getInt(KEY_MAX_CRANES, DEFAULTS.maxCranes),
                craneSpeedLevel = prefs.getInt(KEY_SPEED_LEVEL, DEFAULTS.craneSpeedLevel)
            )
        }

        // save Сохраняет кастомные параметры в SharedPreferences
        fun save(context: Context, custom: CustomDifficulty) {
            context.getSharedPreferences("stack_attack_prefs", Context.MODE_PRIVATE).edit {
                putBoolean(KEY_CAN_PUSH_JUMP, custom.canPushInJump)
                putInt(KEY_MAX_PUSH_CHAIN, custom.maxPushChain)
                putInt(KEY_MAX_CRANES, custom.maxCranes)
                putInt(KEY_SPEED_LEVEL, custom.craneSpeedLevel)
            }
        }

        // isCustomMode Проверяет, активен ли кастомный режим сложности
        fun isCustomMode(context: Context): Boolean {
            return context.getSharedPreferences("stack_attack_prefs", Context.MODE_PRIVATE)
                .getBoolean("use_custom_difficulty", false)
        }

        // setCustomMode Сохраняет флаг активности кастомного режима
        fun setCustomMode(context: Context, enabled: Boolean) {
            context.getSharedPreferences("stack_attack_prefs", Context.MODE_PRIVATE)
                .edit { putBoolean("use_custom_difficulty", enabled) }
        }

        // calcDifficultyLabel Вычисляет приблизительный уровень сложности по параметрам
        fun calcDifficultyLabel(custom: CustomDifficulty): Pair<String, String> {
            // Набор очков сложности по каждому параметру
            var score = 0

            // Прыжок: нельзя толкать — сложнее (+2)
            if (!custom.canPushInJump) score += 2

            // Цепочка: чем меньше ящиков, тем сложнее
            score += when (custom.maxPushChain) {
                1 -> 3
                2 -> 1
                3 -> 0
                else -> 0
            }

            // Грейферы: чем больше, тем сложнее
            score += when {
                custom.maxCranes <= 4 -> 0
                custom.maxCranes <= 5 -> 1
                custom.maxCranes <= 6 -> 2
                custom.maxCranes <= 7 -> 3
                custom.maxCranes <= 8 -> 4
                else -> 5
            }

            // Скорость: чем выше, тем сложнее
            score += custom.craneSpeedLevel - 1  // 0..4

            // Итоговый label по сумме баллов (0..14)
            return when {
                score <= 1  -> Pair("😴 Скучно",       "Даже бабушка справится одной рукой, попивая чай другой")
                score <= 3  -> Pair("😊 Легко",         "Здесь проигрывают только легенды. Причём случайно")
                score <= 6  -> Pair("🙂 Средне",        "Вот это годнота! Мозг работает, руки не отваливаются")
                score <= 8  -> Pair("😤 Сложно",        "Придётся включить мозг и отложить бутерброд в сторонку")
                score <= 11 -> Pair("😨 Очень сложно",  "Рекорд? Какой рекорд? Выжить бы! Но ты ведь не сдашься?")
                else        -> Pair("💀 Кошмар",        "Это безумие! Ну нахер....")
            }
        }
    }
}