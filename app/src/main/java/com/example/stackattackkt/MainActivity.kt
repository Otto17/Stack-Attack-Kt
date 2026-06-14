// Copyright (c) 2026 Otto
// License: GPL-2.0-or-later (see LICENSE)

package com.example.stackattackkt

import android.content.Intent
import android.widget.FrameLayout
import android.widget.ImageButton
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

// MainActivity Представляет главное меню приложения и навигацию
class MainActivity : AppCompatActivity() {

    private lateinit var currentTheme: ColorTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeOrdinal =
            getSharedPreferences("stack_attack_prefs", MODE_PRIVATE).getInt("color_theme", 0)
        window.decorView.setBackgroundColor(ColorTheme.fromOrdinal(themeOrdinal).primaryBg)

        setContentView(R.layout.activity_main)

        currentTheme = loadTheme()
        applyTheme()

        val tvRecord: TextView = findViewById(R.id.tvRecord)
        val btnPlay: Button = findViewById(R.id.btnPlay)
        val btnExit: Button = findViewById(R.id.btnExit)
        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        val tvAuthor: TextView = findViewById(R.id.tvAuthor)

        tvRecord.text = getString(R.string.record_value, loadCurrentRecord())

        btnPlay.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        btnExit.setOnClickListener {
            // Завершает все связанные активности для полного выхода из приложения
            finishAffinity()
        }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        val tvLicense: TextView = findViewById(R.id.tvLicense)
        tvLicense.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html".toUri()
            )
            startActivity(intent)
        }
        tvAuthor.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://gitflic.ru/project/otto/stack-attack-kt".toUri()
            )
            startActivity(intent)
        }
    }

    // onResume Синхронизирует состояние интерфейса и настройки экрана при возврате пользователя в меню
    override fun onResume() {
        super.onResume()
        OrientationManager.apply(this)
        val tvRecord: TextView = findViewById(R.id.tvRecord)
        tvRecord.text = getString(R.string.record_value, loadCurrentRecord())
        hideSystemUI()
        currentTheme = loadTheme()
        applyTheme()
    }

    // loadCurrentRecord Извлекает рекорд для активного режима:
    // в кастомном режиме берёт ключ "CUSTOM", иначе — имя выбранной сложности
    private fun loadCurrentRecord(): Int {
        val prefs = getSharedPreferences("stack_attack_prefs", MODE_PRIVATE)
        val key = if (prefs.getBoolean("use_custom_difficulty", false)) "CUSTOM"
        else prefs.getString("difficulty", Difficulty.MEDIUM.name) ?: Difficulty.MEDIUM.name
        return prefs.getInt("best_record_$key", 0)
    }

    // loadTheme Получает активную цветовую схему из постоянного хранилища
    private fun loadTheme(): ColorTheme {
        val ordinal = getSharedPreferences("stack_attack_prefs", MODE_PRIVATE)
            .getInt("color_theme", 0)
        return ColorTheme.fromOrdinal(ordinal)
    }

    // applyTheme Применяет и настраивает визуальное оформление элементов меню
    private fun applyTheme() {
        findViewById<FrameLayout>(R.id.mainRootLayout).setBackgroundColor(currentTheme.primaryBg)

        // Кнопки со скруглениями
        findViewById<Button>(R.id.btnPlay).apply {
            background = makeRoundedDrawable(currentTheme.secondaryBg, currentTheme.textStroke)
            setTextColor(currentTheme.textStroke)
        }
        findViewById<Button>(R.id.btnExit).apply {
            background = makeRoundedDrawable(currentTheme.secondaryBg, currentTheme.textStroke)
            setTextColor(currentTheme.textStroke)
        }

        findViewById<TextView>(R.id.tvTitle).setTextColor(currentTheme.textStroke)
        findViewById<TextView>(R.id.tvRecord).setTextColor(currentTheme.textStroke)
        findViewById<TextView>(R.id.tvLicense).setTextColor(currentTheme.textStroke)
        findViewById<TextView>(R.id.tvAuthor).setTextColor(currentTheme.textStroke)

        findViewById<ImageButton>(R.id.btnSettings).setColorFilter(currentTheme.textStroke)
    }

    // makeRoundedDrawable Генерирует фон для кнопок меню
    private fun makeRoundedDrawable(
        bgColor: Int,
        strokeColor: Int
    ): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 4f * resources.displayMetrics.density
            setColor(bgColor)
            setStroke(
                (1.5f * resources.displayMetrics.density).toInt(),
                strokeColor
            )
        }
    }

    @Suppress("DEPRECATION")
    // hideSystemUI Убирает панели навигации и статуса для чистого вида меню
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
            window.insetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    )
        }
    }
}