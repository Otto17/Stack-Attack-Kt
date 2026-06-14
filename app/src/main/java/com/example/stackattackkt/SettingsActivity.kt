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

    // Переключатель режима сложности
    private lateinit var btnModeStandard: Button
    private lateinit var btnModeCustom: Button

    // Панели режимов
    private lateinit var panelStandard: android.widget.ScrollView
    private lateinit var panelCustom: android.widget.ScrollView
    private lateinit var panelStandardRight: LinearLayout
    private lateinit var panelCustomRight: LinearLayout

    // Кастомные элементы управления
    private lateinit var btnPushJumpOn: Button
    private lateinit var btnPushJumpOff: Button
    private lateinit var btnChain1: Button
    private lateinit var btnChain2: Button
    private lateinit var btnChain3: Button
    private lateinit var btnCranes4: Button
    private lateinit var btnCranes5: Button
    private lateinit var btnCranes6: Button
    private lateinit var btnCranes7: Button
    private lateinit var btnCranes8: Button
    private lateinit var btnCranes9: Button
    private lateinit var btnSpeed1: Button
    private lateinit var btnSpeed2: Button
    private lateinit var btnSpeed3: Button
    private lateinit var btnSpeed4: Button
    private lateinit var btnSpeed5: Button
    private lateinit var tvCustomDiffLabel: TextView
    private lateinit var tvCustomDiffHint: TextView
    private lateinit var leftColumn: LinearLayout

    private var selected: Difficulty = Difficulty.MEDIUM
    private var currentTheme: ColorTheme = ColorTheme.CLASSIC
    private var isLandscape: Boolean = true
    private var useCustomMode: Boolean = false
    private var customParams: CustomDifficulty = CustomDifficulty.DEFAULTS

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

        // Переключатели режима
        btnModeStandard = findViewById(R.id.btnModeStandard)
        btnModeCustom = findViewById(R.id.btnModeCustom)
        panelStandard = findViewById(R.id.panelStandard)
        panelCustom = findViewById(R.id.panelCustom)
        panelStandardRight = findViewById(R.id.panelStandardRight)
        panelCustomRight = findViewById(R.id.panelCustomRight)
        leftColumn = findViewById(R.id.leftColumn)

        // Кастомные кнопки
        btnPushJumpOn = findViewById(R.id.btnPushJumpOn)
        btnPushJumpOff = findViewById(R.id.btnPushJumpOff)
        btnChain1 = findViewById(R.id.btnChain1)
        btnChain2 = findViewById(R.id.btnChain2)
        btnChain3 = findViewById(R.id.btnChain3)
        btnCranes4 = findViewById(R.id.btnCranes4)
        btnCranes5 = findViewById(R.id.btnCranes5)
        btnCranes6 = findViewById(R.id.btnCranes6)
        btnCranes7 = findViewById(R.id.btnCranes7)
        btnCranes8 = findViewById(R.id.btnCranes8)
        btnCranes9 = findViewById(R.id.btnCranes9)
        btnSpeed1 = findViewById(R.id.btnSpeed1)
        btnSpeed2 = findViewById(R.id.btnSpeed2)
        btnSpeed3 = findViewById(R.id.btnSpeed3)
        btnSpeed4 = findViewById(R.id.btnSpeed4)
        btnSpeed5 = findViewById(R.id.btnSpeed5)
        tvCustomDiffLabel = findViewById(R.id.tvCustomDiffLabel)
        tvCustomDiffHint = findViewById(R.id.tvCustomDiffHint)

        selected = loadDifficulty()
        currentTheme = loadTheme()
        isLandscape = OrientationManager.load(this)
        useCustomMode = CustomDifficulty.isCustomMode(this)
        customParams = CustomDifficulty.load(this)

        createThemeSquares()
        applyTheme()
        updateDiffUI()
        updateOrientUI()
        updateModeUI()
        updateCustomUI()
        adjustColumnsForOrientation()

        // Переключатели режима сложности
        btnModeStandard.setOnClickListener {
            useCustomMode = false
            CustomDifficulty.setCustomMode(this, false)
            updateModeUI()
        }
        btnModeCustom.setOnClickListener {
            useCustomMode = true
            CustomDifficulty.setCustomMode(this, true)
            updateModeUI()
        }

        btnEasy.setOnClickListener { select(Difficulty.EASY) }
        btnMedium.setOnClickListener { select(Difficulty.MEDIUM) }
        btnHard.setOnClickListener { select(Difficulty.HARD) }
        btnExtreme.setOnClickListener { select(Difficulty.EXTREME) }
        btnBack.setOnClickListener { finish() }

        // Кастомные слушатели: прыжок
        btnPushJumpOn.setOnClickListener {
            customParams = customParams.copy(canPushInJump = true)
            saveAndUpdateCustom()
        }
        btnPushJumpOff.setOnClickListener {
            customParams = customParams.copy(canPushInJump = false)
            saveAndUpdateCustom()
        }

        // Цепочка ящиков
        btnChain1.setOnClickListener {
            customParams = customParams.copy(maxPushChain = 1)
            saveAndUpdateCustom()
        }
        btnChain2.setOnClickListener {
            customParams = customParams.copy(maxPushChain = 2)
            saveAndUpdateCustom()
        }
        btnChain3.setOnClickListener {
            customParams = customParams.copy(maxPushChain = 3)
            saveAndUpdateCustom()
        }

        // Количество грейферов
        btnCranes4.setOnClickListener {
            customParams = customParams.copy(maxCranes = 4); saveAndUpdateCustom()
        }
        btnCranes5.setOnClickListener {
            customParams = customParams.copy(maxCranes = 5); saveAndUpdateCustom()
        }
        btnCranes6.setOnClickListener {
            customParams = customParams.copy(maxCranes = 6); saveAndUpdateCustom()
        }
        btnCranes7.setOnClickListener {
            customParams = customParams.copy(maxCranes = 7); saveAndUpdateCustom()
        }
        btnCranes8.setOnClickListener {
            customParams = customParams.copy(maxCranes = 8); saveAndUpdateCustom()
        }
        btnCranes9.setOnClickListener {
            customParams = customParams.copy(maxCranes = 9); saveAndUpdateCustom()
        }

        // Скорость грейферов
        btnSpeed1.setOnClickListener {
            customParams = customParams.copy(craneSpeedLevel = 1); saveAndUpdateCustom()
        }
        btnSpeed2.setOnClickListener {
            customParams = customParams.copy(craneSpeedLevel = 2); saveAndUpdateCustom()
        }
        btnSpeed3.setOnClickListener {
            customParams = customParams.copy(craneSpeedLevel = 3); saveAndUpdateCustom()
        }
        btnSpeed4.setOnClickListener {
            customParams = customParams.copy(craneSpeedLevel = 4); saveAndUpdateCustom()
        }
        btnSpeed5.setOnClickListener {
            customParams = customParams.copy(craneSpeedLevel = 5); saveAndUpdateCustom()
        }

        btnOrientLandscape.setOnClickListener {
            isLandscape = true
            OrientationManager.save(this, true)
            OrientationManager.apply(this)
            (application as App).applyOrientationToAll(true)
            updateOrientUI()
            themeContainer.post { createThemeSquares() }
        }

        btnOrientPortrait.setOnClickListener {
            isLandscape = false
            OrientationManager.save(this, false)
            OrientationManager.apply(this)
            (application as App).applyOrientationToAll(false)
            updateOrientUI()
            themeContainer.post { createThemeSquares() }
        }
    }

    // saveAndUpdateCustom Сохраняет кастомные параметры и обновляет UI
    private fun saveAndUpdateCustom() {
        CustomDifficulty.save(this, customParams)
        updateCustomUI()
    }

    // updateModeUI Переключает видимость панелей и подсвечивает активный режим
    private fun updateModeUI() {
        if (useCustomMode) {
            btnModeStandard.background =
                makeRoundedDrawable(currentTheme.secondaryBg, currentTheme.textStroke)
            btnModeStandard.alpha = 0.85f
            btnModeCustom.background =
                makeRoundedDrawable(currentTheme.accent, currentTheme.textStroke)
            btnModeCustom.alpha = 1f
            // Левая колонка: скрываем стандартные уровни, показываем кастомные параметры
            panelStandard.visibility = View.GONE
            panelCustom.visibility = View.VISIBLE
            // Правая колонка: скрываем описание уровня, показываем подсказку сложности
            panelStandardRight.visibility = View.GONE
            panelCustomRight.visibility = View.VISIBLE
        } else {
            btnModeStandard.background =
                makeRoundedDrawable(currentTheme.accent, currentTheme.textStroke)
            btnModeStandard.alpha = 1f
            btnModeCustom.background =
                makeRoundedDrawable(currentTheme.secondaryBg, currentTheme.textStroke)
            btnModeCustom.alpha = 0.85f
            // Левая колонка: показываем стандартные уровни, скрываем кастомные параметры
            panelStandard.visibility = View.VISIBLE
            panelCustom.visibility = View.GONE
            // Правая колонка: показываем описание уровня, скрываем подсказку сложности
            panelStandardRight.visibility = View.VISIBLE
            panelCustomRight.visibility = View.GONE
        }
        listOf(btnModeStandard, btnModeCustom).forEach { it.setTextColor(currentTheme.textStroke) }
    }

    // updateCustomUI Обновляет состояние кнопок и подсказку в кастомном режиме
    private fun updateCustomUI() {
        // Прыжок
        styleToggle(btnPushJumpOn, customParams.canPushInJump)
        styleToggle(btnPushJumpOff, !customParams.canPushInJump)

        // Цепочка
        styleToggle(btnChain1, customParams.maxPushChain == 1)
        styleToggle(btnChain2, customParams.maxPushChain == 2)
        styleToggle(btnChain3, customParams.maxPushChain == 3)

        // Грейферы
        val craneButtons = mapOf(
            4 to btnCranes4, 5 to btnCranes5, 6 to btnCranes6,
            7 to btnCranes7, 8 to btnCranes8, 9 to btnCranes9
        )
        craneButtons.forEach { (count, btn) ->
            styleToggle(btn, customParams.maxCranes == count)
        }

        // Скорость
        val speedButtons = mapOf(
            1 to btnSpeed1, 2 to btnSpeed2, 3 to btnSpeed3,
            4 to btnSpeed4, 5 to btnSpeed5
        )
        speedButtons.forEach { (level, btn) ->
            styleToggle(btn, customParams.craneSpeedLevel == level)
        }

        // Динамическая подсказка
        val (label, hint) = CustomDifficulty.calcDifficultyLabel(customParams)
        tvCustomDiffLabel.text = label
        tvCustomDiffHint.text = hint
        tvCustomDiffLabel.setTextColor(currentTheme.textStroke)
        tvCustomDiffHint.setTextColor(currentTheme.textStroke)
    }

    // styleToggle Выделяет активную кнопку в группе акцентным цветом
    private fun styleToggle(btn: Button, isActive: Boolean) {
        if (isActive) {
            btn.background = makeRoundedDrawable(currentTheme.accent, currentTheme.textStroke)
            btn.alpha = 1f
        } else {
            btn.background = makeRoundedDrawable(currentTheme.secondaryBg, currentTheme.textStroke)
            btn.alpha = 0.85f
        }
        btn.setTextColor(currentTheme.textStroke)
    }

    // onConfigurationChanged Пересчитывает геометрию элементов управления при смене параметров экрана
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        themeContainer.post { createThemeSquares() }
        adjustColumnsForOrientation()
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

        // Заголовки кастомного блока
        listOf(
            R.id.tvCustomTitle, R.id.tvModeSwitchLabel,
            R.id.tvPushJumpLabel, R.id.tvChainLabel,
            R.id.tvCranesLabel, R.id.tvSpeedLabel
        ).forEach {
            findViewById<TextView>(it)?.setTextColor(currentTheme.textStroke)
        }
        listOf(R.id.customDivider, R.id.modeDivider).forEach {
            findViewById<View>(it)?.setBackgroundColor(currentTheme.textStroke)
        }

        updateDiffUI()
        updateOrientUI()
        updateModeUI()
        updateCustomUI()
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

    // adjustColumnsForOrientation Подбирает ширину колонок под текущую ориентацию
    private fun adjustColumnsForOrientation() {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val isPortrait = screenH > screenW

        val leftWeight = if (isPortrait) 1.1f else 0.8f
        val rightWeight = if (isPortrait) 1.0f else 1.2f

        (leftColumn.layoutParams as LinearLayout.LayoutParams).apply {
            weight = leftWeight
        }.also { leftColumn.layoutParams = it }

        // Правая колонка — соседний элемент в том же родителе
        val rightColumn = leftColumn.parent?.let {
            (it as? LinearLayout)?.getChildAt(1)
        } as? LinearLayout
        rightColumn?.let {
            (it.layoutParams as LinearLayout.LayoutParams).apply {
                weight = rightWeight
            }.also { lp -> it.layoutParams = lp }
        }

        leftColumn.requestLayout()
        rightColumn?.requestLayout()
    }
}