// Copyright (c) 2026 Otto
// License: GPL-2.0-or-later (see LICENSE)

package com.example.stackattackkt

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.core.content.edit

// OrientationManager Централизует логику управления ориентацией экрана
object OrientationManager {

    // save Записывает предпочтение пользователя по ориентации в настройки
    fun save(context: Context, isLandscape: Boolean) {
        context.getSharedPreferences("stack_attack_prefs", Context.MODE_PRIVATE)
            .edit { putBoolean("is_landscape", isLandscape) }
    }

    // load Читает сохраненный флаг ориентации (по умолчанию альбомная)
    fun load(context: Context): Boolean {
        return context.getSharedPreferences("stack_attack_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_landscape", true)
    }

    // apply Немедленно меняет ориентацию выбранной активности
    fun apply(activity: Activity) {
        val isLandscape = load(activity)
        activity.requestedOrientation = if (isLandscape)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}