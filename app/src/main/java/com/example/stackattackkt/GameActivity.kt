// Copyright (c) 2026 Otto
// License: GPL-2.0-or-later (see LICENSE)

package com.example.stackattackkt

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

// GameActivity Управляет игровым экраном, интерфейсом и жизненным циклом игры
class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var btnJump: Button
    private lateinit var btnPause: Button
    private lateinit var btnPauseLandscape: Button
    private lateinit var btnResumeOverlay: Button
    private lateinit var btnMenuOverlay: Button
    private lateinit var btnPlayAgain: Button
    private lateinit var btnMenuGameOver: Button

    private lateinit var tvScore: TextView
    private lateinit var tvBest: TextView
    private lateinit var tvRecord: TextView
    private lateinit var tvGameOverScore: TextView

    private lateinit var pauseOverlay: FrameLayout
    private lateinit var gameOverOverlay: FrameLayout

    private lateinit var soundManager: SoundManager
    private var currentDifficulty: Difficulty = Difficulty.MEDIUM

    private var isPaused = false
    private var currentScore = 0
    private var bestThisSession = 0
    private var allTimeRecord = 0

    // gameStarted Предотвращает повторную инициализацию игрового движка при смене конфигурации
    private var gameStarted = false

    // Задает цвет фона окна до загрузки разметки для исключения белых вспышек
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeOrdinal =
            getSharedPreferences("stack_attack_prefs", MODE_PRIVATE).getInt("color_theme", 0)
        window.decorView.setBackgroundColor(ColorTheme.fromOrdinal(themeOrdinal).primaryBg)

        setContentView(R.layout.activity_game)

        currentDifficulty = loadDifficulty()
        allTimeRecord = loadRecord()
        bestThisSession = loadLastScore()
        soundManager = SoundManager(this)

        gameView = findViewById(R.id.gameView)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        btnJump = findViewById(R.id.btnJump)
        btnPause = findViewById(R.id.btnPause)
        btnPauseLandscape = findViewById(R.id.btnPauseLandscape)
        btnResumeOverlay = findViewById(R.id.btnResumeOverlay)
        btnMenuOverlay = findViewById(R.id.btnMenuOverlay)
        btnPlayAgain = findViewById(R.id.btnPlayAgain)
        btnMenuGameOver = findViewById(R.id.btnMenuGameOver)

        tvScore = findViewById(R.id.tvScore)
        tvBest = findViewById(R.id.tvBest)
        tvRecord = findViewById(R.id.tvRecord)
        tvGameOverScore = findViewById(R.id.tvGameOverScore)

        pauseOverlay = findViewById(R.id.pauseOverlay)
        gameOverOverlay = findViewById(R.id.gameOverOverlay)

        val theme = loadTheme()
        gameView.setTheme(theme)
        applyTheme(theme)

        updateScoreUI(0)
        setupGameCallbacks()
        setupButtons()

        applyDifficultyToView()

        // Ждём когда layout реально отрисуется с правильными размерами,
        // затем применяем adjustForOrientation() и запускаем игру
        val rootView = findViewById<FrameLayout>(R.id.gameRootLayout)
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Использует размеры только после завершения прохода компоновки View
                rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                adjustForOrientation()

                if (!gameStarted) {
                    gameStarted = true
                    gameView.setBestScore(allTimeRecord)
                    gameView.startGame()
                }
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Адаптирует интерфейс без пересоздания активности для сохранения состояния игры
        adjustForOrientation()
        hideSystemUI()
    }

    // isPortrait Вычисляет текущую ориентацию на основе пропорций экрана
    private fun isPortrait(): Boolean {
        val dm = resources.displayMetrics
        return dm.heightPixels > dm.widthPixels
    }

    // adjustForOrientation Пересчитывает размеры и веса элементов управления для удобства игры
    private fun adjustForOrientation() {
        val dp = resources.displayMetrics.density
        val portrait = isPortrait()

        val topPanel = findViewById<LinearLayout>(R.id.topPanel)
        val topRowTitle = findViewById<LinearLayout>(R.id.topRowTitle)
        val controlsPanel = findViewById<LinearLayout>(R.id.controlsPanel)
        val spacerMiddle = findViewById<View>(R.id.spacerMiddle)

        if (portrait) {
            topPanel.layoutParams = (topPanel.layoutParams as LinearLayout.LayoutParams).apply {
                height = (50 * dp).toInt()
            }
            topRowTitle.visibility = View.VISIBLE
            btnPauseLandscape.visibility = View.GONE

            controlsPanel.layoutParams =
                (controlsPanel.layoutParams as LinearLayout.LayoutParams).apply {
                    height = (90 * dp).toInt()
                    topMargin = 0
                }
            controlsPanel.setPadding(
                (4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt()
            )
            (spacerMiddle.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 0.1f
            }.also { spacerMiddle.layoutParams = it }

            // Портрет: стандартные веса кнопок
            (btnLeft.layoutParams as LinearLayout.LayoutParams).weight = 1f
            (btnRight.layoutParams as LinearLayout.LayoutParams).weight = 1f
            (btnJump.layoutParams as LinearLayout.LayoutParams).weight = 1f

        } else {
            topPanel.layoutParams = (topPanel.layoutParams as LinearLayout.LayoutParams).apply {
                height = (32 * dp).toInt()
            }
            topRowTitle.visibility = View.GONE
            btnPauseLandscape.visibility = View.VISIBLE

            controlsPanel.layoutParams =
                (controlsPanel.layoutParams as LinearLayout.LayoutParams).apply {
                    height = (70 * dp).toInt()
                    topMargin = (-10 * dp).toInt()
                }
            controlsPanel.setPadding(
                (5 * dp).toInt(), (5 * dp).toInt(), (5 * dp).toInt(), (5 * dp).toInt()
            )
            (spacerMiddle.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 0.2f
            }.also { spacerMiddle.layoutParams = it }

            // Ландшафт: сужение кнопок ◀ ▶ и ▲
            (btnLeft.layoutParams as LinearLayout.LayoutParams).weight = 0.2f
            (btnRight.layoutParams as LinearLayout.LayoutParams).weight = 0.2f
            (btnJump.layoutParams as LinearLayout.LayoutParams).weight = 0.25f
        }

        // Обновляет макеты после программного изменения параметров layoutParams
        btnLeft.requestLayout()
        btnRight.requestLayout()
        btnJump.requestLayout()
        topPanel.requestLayout()
        controlsPanel.requestLayout()
    }

    // loadTheme Извлекает сохраненную тему оформления из настроек
    private fun loadTheme(): ColorTheme {
        val ordinal =
            getSharedPreferences("stack_attack_prefs", MODE_PRIVATE).getInt("color_theme", 0)
        return ColorTheme.fromOrdinal(ordinal)
    }

    // applyTheme Настраивает цвета всех визуальных компонентов согласно выбранной теме
    private fun applyTheme(theme: ColorTheme) {
        findViewById<FrameLayout>(R.id.gameRootLayout).setBackgroundColor(theme.primaryBg)

        // Делает верхнюю панель полупрозрачной для лучшей интеграции с фоном
        findViewById<LinearLayout>(R.id.topPanel).setBackgroundColor(
            android.graphics.Color.argb(
                68,
                android.graphics.Color.red(theme.textStroke),
                android.graphics.Color.green(theme.textStroke),
                android.graphics.Color.blue(theme.textStroke)
            )
        )

        listOf(btnPause, btnPauseLandscape).forEach { btn ->
            btn.background = makeRoundedDrawable(theme.secondaryBg, theme.textStroke)
            btn.setTextColor(theme.textStroke)
        }

        tvScore.setTextColor(theme.textStroke)
        tvBest.setTextColor(theme.textStroke)
        tvRecord.setTextColor(theme.textStroke)
        findViewById<TextView>(R.id.tvGameTitle).setTextColor(theme.textStroke)

        findViewById<LinearLayout>(R.id.controlsPanel).setBackgroundColor(theme.secondaryBg)

        btnLeft.background = makeRoundedDrawable(theme.secondaryBg, theme.textStroke)
        btnLeft.setTextColor(theme.textStroke)
        btnRight.background = makeRoundedDrawable(theme.secondaryBg, theme.textStroke)
        btnRight.setTextColor(theme.textStroke)
        btnJump.background = makeRoundedDrawable(theme.secondaryBg, theme.textStroke)
        btnJump.setTextColor(theme.textStroke)

        findViewById<LinearLayout>(R.id.pauseContent).setBackgroundColor(theme.secondaryBg)
        findViewById<TextView>(R.id.tvPauseTitle).setTextColor(theme.textStroke)
        btnResumeOverlay.background = makeRoundedDrawable(theme.primaryBg, theme.textStroke)
        btnResumeOverlay.setTextColor(theme.textStroke)
        btnMenuOverlay.background = makeRoundedDrawable(theme.primaryBg, theme.textStroke)
        btnMenuOverlay.setTextColor(theme.textStroke)

        findViewById<LinearLayout>(R.id.gameOverContent).setBackgroundColor(theme.secondaryBg)
        findViewById<TextView>(R.id.tvGameOverTitle).setTextColor(theme.textStroke)
        tvGameOverScore.setTextColor(theme.textStroke)
        btnPlayAgain.background = makeRoundedDrawable(theme.primaryBg, theme.textStroke)
        btnPlayAgain.setTextColor(theme.textStroke)
        btnMenuGameOver.background = makeRoundedDrawable(theme.primaryBg, theme.textStroke)
        btnMenuGameOver.setTextColor(theme.textStroke)
    }

    // makeRoundedDrawable Создает графический объект со скругленными углами и обводкой
    private fun makeRoundedDrawable(
        bgColor: Int,
        strokeColor: Int
    ): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 4f * resources.displayMetrics.density
            setColor(bgColor)
            setStroke((1.5f * resources.displayMetrics.density).toInt(), strokeColor)
        }
    }

    // setupGameCallbacks Назначает обработчики событий игрового движка
    private fun setupGameCallbacks() {
        gameView.onScoreChanged = { score ->
            currentScore = score
            if (score > bestThisSession) bestThisSession = score
            if (score > allTimeRecord) {
                allTimeRecord = score
                saveRecord(allTimeRecord)
            }
            // Передаёт обновление текста в UI-поток из игрового цикла
            runOnUiThread { updateScoreUI(score) }
        }

        gameView.onGameOver = { score ->
            currentScore = score
            if (score > bestThisSession) bestThisSession = score
            saveLastScore(score)
            if (score > allTimeRecord) {
                allTimeRecord = score
                saveRecord(allTimeRecord)
            }
            runOnUiThread {
                updateScoreUI(score)
                tvGameOverScore.text = getString(R.string.score_value, score)
                gameOverOverlay.visibility = View.VISIBLE
            }
        }

        gameView.onJump = { soundManager.playJump() }
        gameView.onLand = { soundManager.playLand() }
    }

    // togglePause Переключает состояние паузы и видимость оверлея
    private fun togglePause() {
        isPaused = !isPaused
        if (isPaused) {
            gameView.pauseGame()
            btnPause.text = getString(R.string.resume)
            btnPauseLandscape.text = getString(R.string.resume)
            pauseOverlay.visibility = View.VISIBLE
        } else {
            gameView.resumeGame()
            btnPause.text = getString(R.string.pause)
            btnPauseLandscape.text = getString(R.string.pause)
            pauseOverlay.visibility = View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    // setupButtons Инициализирует слушатели нажатий кнопок управления
    private fun setupButtons() {
        btnLeft.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> gameView.setMoveLeft(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    gameView.setMoveLeft(false); v.performClick()
                }
            }
            true
        }
        btnRight.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> gameView.setMoveRight(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    gameView.setMoveRight(false); v.performClick()
                }
            }
            true
        }
        btnJump.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> gameView.setJump(true)
                MotionEvent.ACTION_UP -> v.performClick()
            }
            true
        }

        btnPause.setOnClickListener { togglePause() }
        btnPauseLandscape.setOnClickListener { togglePause() }
        btnResumeOverlay.setOnClickListener { togglePause() }
        btnMenuOverlay.setOnClickListener { goToMenu() }

        btnPlayAgain.setOnClickListener {
            // Очищает состояние и перезапускает игру при нажатии "Ещё раз"
            gameOverOverlay.visibility = View.GONE
            currentScore = 0
            updateScoreUI(0)
            currentDifficulty = loadDifficulty()
            allTimeRecord = loadRecord()
            bestThisSession = loadLastScore()
            applyDifficultyToView()
            updateScoreUI(0)
            gameView.startGame()
        }
        btnMenuGameOver.setOnClickListener { goToMenu() }
    }

    // goToMenu Останавливает игру и переходит в главное меню
    private fun goToMenu() {
        if (currentScore > 0) {
            if (currentScore > bestThisSession) bestThisSession = currentScore
            saveLastScore(currentScore)
            if (currentScore > allTimeRecord) {
                allTimeRecord = currentScore
                saveRecord(allTimeRecord)
            }
        }
        gameView.stopGame()
        startActivity(
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP })
        finish()
    }

    // updateScoreUI Обновляет текстовые поля со счетом на экране
    private fun updateScoreUI(score: Int) {
        tvScore.text = getString(R.string.score_value, score)
        tvBest.text = getString(R.string.best_value, bestThisSession)
        tvRecord.text = getString(R.string.record_value, allTimeRecord)
    }

    // saveRecord Сохраняет лучший результат для текущего режима сложности
    private fun saveRecord(record: Int) {
        getSharedPreferences(
            "stack_attack_prefs",
            MODE_PRIVATE
        ).edit { putInt("best_record_${scoreKey()}", record) }
    }

    // loadRecord Загружает рекорд из постоянного хранилища для текущего режима
    private fun loadRecord(): Int = getSharedPreferences("stack_attack_prefs", MODE_PRIVATE).getInt(
            "best_record_${scoreKey()}",
            0
        )

    // saveLastScore Запоминает результат последней сессии для текущего режима
    private fun saveLastScore(score: Int) {
        getSharedPreferences(
            "stack_attack_prefs",
            MODE_PRIVATE
        ).edit { putInt("last_score_${scoreKey()}", score) }
    }

    // loadLastScore Возвращает счёт предыдущей игры для текущего режима
    private fun loadLastScore(): Int =
        getSharedPreferences("stack_attack_prefs", MODE_PRIVATE).getInt(
                "last_score_${scoreKey()}",
                0
            )

    // onResume Возобновляет работу активности
    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    // onPause Приостанавливает активность
    override fun onPause() {
        super.onPause()
        // Активирует паузу автоматически при сворачивании приложения для сохранности прогресса
        if (!isPaused) {
            isPaused = true
            gameView.pauseGame()
            btnPause.text = getString(R.string.resume)
            btnPauseLandscape.text = getString(R.string.resume)
            pauseOverlay.visibility = View.VISIBLE
        }
    }

    // onDestroy Очищает ресурсы перед уничтожением активности
    override fun onDestroy() {
        super.onDestroy()
        gameView.stopGame()
        soundManager.release()
    }

    @Suppress("DEPRECATION")
    // hideSystemUI Переводит экран в иммерсивный полноэкранный режим
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
            window.insetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    // loadDifficulty Читает выбранный уровень сложности из настроек
    private fun loadDifficulty(): Difficulty {
        val name = getSharedPreferences("stack_attack_prefs", MODE_PRIVATE).getString(
                "difficulty",
                Difficulty.MEDIUM.name
            ) ?: Difficulty.MEDIUM.name
        return try {
            Difficulty.valueOf(name)
        } catch (_: Exception) {
            Difficulty.MEDIUM
        }
    }

    // scoreKey Возвращает ключ для сохранения очков в зависимости от активного режима
    private fun scoreKey(): String = if (CustomDifficulty.isCustomMode(this)) {
        val custom = CustomDifficulty.load(this)
        CustomDifficulty.calcDifficultyKey(custom)
    } else {
        currentDifficulty.name
    }

    // applyDifficultyToView Применяет стандартную или кастомную сложность к игровому движку
    private fun applyDifficultyToView() {
        if (CustomDifficulty.isCustomMode(this)) {
            val custom = CustomDifficulty.load(this)
            gameView.clearCustomDifficulty()
            gameView.setDifficulty(currentDifficulty) // Базовый fallback
            gameView.setCustomDifficulty(custom)
        } else {
            gameView.clearCustomDifficulty()
            gameView.setDifficulty(currentDifficulty)
        }
    }
}