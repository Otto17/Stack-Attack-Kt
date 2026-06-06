// Copyright (c) 2026 Otto
// License: GPL-2.0-or-later (see LICENSE)

package com.example.stackattackkt

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

@Suppress("CustomSplashScreen")
// SplashActivity Обеспечивает бесшовный запуск приложения с корректной ориентацией
class SplashActivity : AppCompatActivity() {

    // onCreate Устанавливает ориентацию на самом раннем этапе для предотвращения лишних вращений экрана
    override fun onCreate(savedInstanceState: Bundle?) {
        val isLandscape = getSharedPreferences("stack_attack_prefs", MODE_PRIVATE)
            .getBoolean("is_landscape", true)
        requestedOrientation = if (isLandscape)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        super.onCreate(savedInstanceState)

        // Переходит к главному меню без видимой задержки и анимации
        startActivity(Intent(this, MainActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}