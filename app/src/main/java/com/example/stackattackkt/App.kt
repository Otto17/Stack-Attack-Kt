// Copyright (c) 2026 Otto
// License: GPL-2.0-or-later (see LICENSE)

package com.example.stackattackkt

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle

class App : Application() {

    // splashActivity Хранит ссылку на экран заставки для управления ориентацией
    var splashActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()

        // Регистрирует слушатель жизненного цикла для управления ориентацией окон
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Обеспечивает корректную ориентацию на версиях ниже Android Q
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    applyOrientation(activity)
                }
                if (activity is SplashActivity) splashActivity = activity
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (activity is SplashActivity) splashActivity = null
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                    // Устанавливает ориентацию на этапе до создания UI для исключения мерцания
                    applyOrientation(activity)
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (activity is SplashActivity) splashActivity = activity
                }

                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    if (activity is SplashActivity) splashActivity = null
                }
            })
        }
    }

    // applyOrientationToAll Обновляет ориентацию через активность-якорь
    fun applyOrientationToAll(isLandscape: Boolean) {
        val desired = if (isLandscape)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        splashActivity?.requestedOrientation = desired
    }

    // applyOrientation Считывает настройки и применяет их к конкретной активности
    private fun applyOrientation(activity: Activity) {
        val isLandscape = getSharedPreferences("stack_attack_prefs", MODE_PRIVATE)
            .getBoolean("is_landscape", true)
        val desired = if (isLandscape)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Изменяет состояние только при несовпадении для предотвращения лишних циклов пересоздания
        if (activity.requestedOrientation != desired) {
            activity.requestedOrientation = desired
        }
    }
}