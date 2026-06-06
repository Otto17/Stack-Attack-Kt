// Copyright (c) 2026 Otto
// License: GPL-2.0-or-later (see LICENSE)

package com.example.stackattackkt

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

// SettingsActivity Управляет конфигурацией сложности, темы и ориентации
class SettingsActivity : AppCompatActivity() {

    private lateinit var btnEasy: Button
    private lateinit var btnMedium: Button
    private lateinit var btnHard: Button
    private lateinit var btnExtreme: Button
    private lateinit var tvDesc: TextView
    private lateinit var tvDiffRecord: TextView
    private lateinit var btnBack: Button
    private lateinit var themeContainer: LinearLayout
    private lateinit var rootLayout: LinearLayout
    private lateinit var btnOrientLandscape: Button
    private lateinit var btnOrientPortrait: Button

    private var selected: Difficulty = Difficulty.MEDIUM
    private var currentTheme: ColorTheme = ColorTheme.CLASSIC
    private var isLandscape: Boolean = true

    // onCreate Инициализирует интерфейс настроек и восстанавливает сохраненное состояние параметров
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeOrdinal =
            getSharedPreferences("stack_attack_prefs", MODE_PRIVATE).getInt("color_theme", 0)
        window.decorView.setBackgroundColor(ColorTheme.fromOrdinal(themeOrdinal).primaryBg)

        setContentView(R.layout.activity_settings)

        btnEasy = findViewById(R.id.btnDiffEasy)
        btnMedium = findViewById(R.id.btnDiffMedium)
        btnHard = findViewById(R.id.btnDiffHard)
        btnExtreme = findViewById(R.id.btnDiffExtreme)
        tvDesc = findViewById(R.id.tvDiffDesc)
        tvDiffRecord = findViewById(R.id.tvDiffRecord)
        btnBack = findViewById(R.id.btnSettingsBack)
        themeContainer = findViewById(R.id.themeContainer)
        rootLayout = findViewById(R.id.rootLayout)
        btnOrientLandscape = findViewById(R.id.btnOrientLandscape)
        btnOrientPortrait = findViewById(R.id.btnOrientPortrait)

        selected = loadDifficulty()
        currentTheme = loadTheme()
        isLandscape = OrientationManager.load(this)

        createThemeSquares()
        applyTheme()
        updateDiffUI()
        updateOrientUI()

        btnEasy.setOnClickListener { select(Difficulty.EASY) }
        btnMedium.setOnClickListener { select(Difficulty.MEDIUM) }
        btnHard.setOnClickListener { select(Difficulty.HARD) }
        btnExtreme.setOnClickListener { select(Difficulty.EXTREME) }
        btnBack.setOnClickListener { finish() }

        btnOrientLandscape.setOnClickListener {
            isLandscape = true
            OrientationManager.save(this, true)
            OrientationManager.apply(this)
            // Обновляет якорь — SplashActivity в стеке
            (application as App).applyOrientationToAll(true)
            updateOrientUI()
            // Перерисовывает сетку тем с задержкой для корректного расчета новых размеров
            themeContainer.post { createThemeSquares() }
        }

        btnOrientPortrait.setOnClickListener {
            isLandscape = false
            OrientationManager.save(this, false)
            OrientationManager.apply(this)
            // Обновляет якорь — SplashActivity в стеке
            (application as App).applyOrientationToAll(false)
            updateOrientUI()
            themeContainer.post { createThemeSquares() }
        }
    }

    // onConfigurationChanged Пересчитывает геометрию элементов управления при смене параметров экрана
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Перестраивает квадратики под новую ориентацию
        themeContainer.post { createThemeSquares() }
        hideSystemUI()
    }

    // updateOrientUI Подсвечивает выбранную кнопку ориентации
    private fun updateOrientUI() {
        if (isLandscape) {
            btnOrientLandscape.background =
                makeRoundedDrawable(currentTheme.accent, currentTheme.textStroke)
            btnOrientLandscape.alpha = 1f
            btnOrientPortrait.background =
                makeRoundedDrawable(currentTheme.secondaryBg, currentTheme.textStroke)
            btnOrientPortrait.alpha = 0.85f
        } else {
            btnOrientLandscape.background =
                makeRoundedDrawable(currentTheme.secondaryBg, currentTheme.textStroke)
            btnOrientLandscape.alpha = 0.85f
            btnOrientPortrait.background =
                makeRoundedDrawable(currentTheme.accent, currentTheme.textStroke)
            btnOrientPortrait.alpha = 1f
        }
    }

    // createThemeSquares Динамически создаёт палитры выбора тем в зависимости от ориентации
    private fun createThemeSquares() {
        themeContainer.removeAllViews()
        themeContainer.orientation = LinearLayout.VERTICAL

        val dp = resources.displayMetrics.density
        val size = (32 * dp).toInt()
        val margin = (5 * dp).toInt()

        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val isPortrait = screenH > screenW

        val themes = ColorTheme.entries
        // Группирует темы в два ряда для удобства в портретном режиме
        val itemsPerRow = if (isPortrait) (themes.size + 1) / 2 else themes.size

        var rowLayout: LinearLayout? = null
        themes.forEachIndexed { index, theme ->
            if (index % itemsPerRow == 0) {
                rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = margin }
                }
                themeContainer.addView(rowLayout)
            }

            val view = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = margin
                    marginEnd = margin
                }
                background = makeSquareDrawable(theme, theme == currentTheme)
                setOnClickListener {
                    currentTheme = theme
                    saveTheme(theme)
                    applyTheme()
                    refreshThemeSquares()
                }
            }
            rowLayout?.addView(view)
        }
    }

    // refreshThemeSquares Обновляет состояние обводок у квадратов выбора тем
    private fun refreshThemeSquares() {
        val themes = ColorTheme.entries
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val isPortrait = screenH > screenW
        val itemsPerRow = if (isPortrait) (themes.size + 1) / 2 else themes.size

        themes.forEachIndexed { index, theme ->
            val rowIndex = index / itemsPerRow
            val colIndex = index % itemsPerRow
            val row = themeContainer.getChildAt(rowIndex) as? LinearLayout ?: return@forEachIndexed
            val v = row.getChildAt(colIndex) ?: return@forEachIndexed
            v.background = makeSquareDrawable(theme, theme == currentTheme)
        }
    }

    // makeSquareDrawable Создаёт иконку палитры темы
    private fun makeSquareDrawable(theme: ColorTheme, selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 6f * resources.displayMetrics.density
            setColor(theme.primaryBg)
            // Добавляет жирную белую рамку для индикации активного выбора
            if (selected) {
                setStroke((4 * resources.displayMetrics.density).toInt(), Color.WHITE)
            } else {
                setStroke(
                    (1.5f * resources.displayMetrics.density).toInt(),
                    Color.argb(120, 0, 0, 0)
                )
            }
        }
    }

    // makeRoundedDrawable Создаёт стандартный фон для кнопок настроек
    private fun makeRoundedDrawable(bgColor: Int, strokeColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 4f * resources.displayMetrics.density
            setColor(bgColor)
            setStroke(
                (1.5f * resources.displayMetrics.density).toInt(),
                strokeColor
            )
        }
    }

    // applyTheme Обновляет все цвета интерфейса при смене темы в реальном времени
    private fun applyTheme() {
        rootLayout.setBackgroundColor(currentTheme.primaryBg)

        tvDesc.setTextColor(currentTheme.textStroke)
        tvDiffRecord.setTextColor(currentTheme.textStroke)

        findViewById<TextView>(R.id.tvDiffTitle).setTextColor(currentTheme.textStroke)
        findViewById<TextView>(R.id.tvThemeTitle).setTextColor(currentTheme.textStroke)
        findViewById<TextView>(R.id.tvDescLabel).setTextColor(currentTheme.textStroke)
        findViewById<TextView>(R.id.tvOrientTitle).setTextColor(currentTheme.textStroke)
        findViewById<View>(R.id.themeDivider).setBackgroundColor(currentTheme.textStroke)
        findViewById<View>(R.id.orientDivider).setBackgroundColor(currentTheme.textStroke)

        updateDiffUI()
        updateOrientUI()
    }

    // select Применяет выбранный уровень сложности
    private fun select(d: Difficulty) {
        selected = d
        saveDifficulty(d)
        updateDiffUI()
    }

    // updateDiffUI Обновляет состояние кнопок выбора сложности и текст описания
    private fun updateDiffUI() {
        val buttons = mapOf(
            Difficulty.EASY to btnEasy,
            Difficulty.MEDIUM to btnMedium,
            Difficulty.HARD to btnHard,
            Difficulty.EXTREME to btnExtreme
        )
        buttons.forEach { (diff, btn) ->
            if (diff == selected) {
                btn.background = makeRoundedDrawable(currentTheme.accent, currentTheme.textStroke)
                btn.alpha = 1f
            } else {
                btn.background =
                    makeRoundedDrawable(currentTheme.secondaryBg, currentTheme.textStroke)
                btn.alpha = 0.85f
            }
            btn.setTextColor(currentTheme.textStroke)
        }
        btnBack.background = makeRoundedDrawable(currentTheme.secondaryBg, currentTheme.textStroke)
        btnBack.setTextColor(currentTheme.textStroke)

        tvDesc.text = getString(
            when (selected) {
                Difficulty.EASY -> R.string.diff_easy_desc
                Difficulty.MEDIUM -> R.string.diff_medium_desc
                Difficulty.HARD -> R.string.diff_hard_desc
                Difficulty.EXTREME -> R.string.diff_extreme_desc
            }
        )

        tvDiffRecord.text = getString(R.string.record_value, loadRecordForDifficulty(selected))
    }

    // saveDifficulty Сохраняет выбор сложности
    private fun saveDifficulty(d: Difficulty) {
        getSharedPreferences("stack_attack_prefs", MODE_PRIVATE)
            .edit { putString("difficulty", d.name) }
    }

    // loadDifficulty Загружает сохраненную сложность
    private fun loadDifficulty(): Difficulty {
        val name = getSharedPreferences("stack_attack_prefs", MODE_PRIVATE)
            .getString("difficulty", Difficulty.MEDIUM.name) ?: Difficulty.MEDIUM.name
        return try {
            Difficulty.valueOf(name)
        } catch (_: Exception) {
            Difficulty.MEDIUM
        }
    }

    // saveTheme Сохраняет порядковый номер темы
    private fun saveTheme(theme: ColorTheme) {
        getSharedPreferences("stack_attack_prefs", MODE_PRIVATE)
            .edit { putInt("color_theme", theme.ordinal) }
    }

    // loadTheme Возвращает сохраненную тему оформления
    private fun loadTheme(): ColorTheme {
        val ordinal = getSharedPreferences("stack_attack_prefs", MODE_PRIVATE)
            .getInt("color_theme", 0)
        return ColorTheme.fromOrdinal(ordinal)
    }

    // loadRecordForDifficulty Загружает лучший результат для конкретного режима
    private fun loadRecordForDifficulty(d: Difficulty): Int =
        getSharedPreferences("stack_attack_prefs", MODE_PRIVATE)
            .getInt("best_record_${d.name}", 0)

    override fun onResume() {
        super.onResume()
        isLandscape = OrientationManager.load(this)
        updateDiffUI()
        updateOrientUI()
        hideSystemUI()
    }

    @Suppress("DEPRECATION")
    // hideSystemUI Активирует полноэкранный режим в настройках
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