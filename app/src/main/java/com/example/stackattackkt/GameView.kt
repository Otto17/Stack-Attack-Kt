// Copyright (c) 2026 Otto
// License: GPL-2.0-or-later (see LICENSE)

package com.example.stackattackkt

import android.content.Context
import android.graphics.*
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withRotation
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


private const val PLAYER_SPEED = 6f // Задает скорость перемещения грузчика
private const val JUMP_VELOCITY = -16f  // Определяет начальное ускорение при прыжке
private const val GRAVITY = 0.85f    // Отвечает за ускорение свободного падения
private const val BOX_FALL_SPEED_MIN = 3.0f  // Минимальная скорость падения ящиков
private const val BOX_FALL_SPEED_MAX = 10.0f  // Максимальная скорость падения ящиков
private const val MAX_ROWS_LANDSCAPE = 8    // Ограничивает высоту башни в альбомном режиме
private const val MAX_ROWS_PORTRAIT = 20    // Ограничивает высоту башни в портретном режиме
private const val GRID_CELL_DP = 14f    // Базовый размер ячейки сетки в dp
private const val BOX_SLIDE_SPEED = 0.12f    // Регулирует плавность перемещения ящика

// BoxPattern Описывает типы рисунков на гранях ящиков
enum class BoxPattern {
    CROSS,        // Перекрестье (текущий)
    STRIPES_H,    // Горизонтальные полосы
    STRIPES_V,    // Вертикальные полосы
    DIAMOND,      // Ромб
    DOTS,         // Четыре точки по углам
    ZIGZAG        // Зигзаг
}

// BoxAnim Содержит данные для плавного перемещения ящика в сетке
data class BoxAnim(
    val col: Int, val row: Int, var visualX: Float, var visualY: Float, val pattern: BoxPattern
)

// FBox Представляет физический объект падающего ящика
data class FBox(
    var cx: Float,
    var top: Float,
    var col: Int,
    val sz: Float,
    val speed: Float,
    val pattern: BoxPattern
) {
    val left get() = cx - sz / 2f
    val right get() = cx + sz / 2f
    val bottom get() = top + sz
}

// FCrane Описывает состояние и параметры движения грейфера
data class FCrane(
    var cx: Float,
    val speed: Float,
    var hasBox: Boolean = true,
    var dropped: Boolean = false,
    var jawAngle: Float = 0f,
    var dropAtX: Float = 0f,
    val pattern: BoxPattern = BoxPattern.entries.let { it[Random.nextInt(it.size)] }
)

@Suppress("SpellCheckingInspection")
// GameView Реализует графическое отображение и физику игрового процесса
class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var onScoreChanged: ((Int) -> Unit)? = null
    var onGameOver: ((Int) -> Unit)? = null
    var onLand: (() -> Unit)? = null
    var onJump: (() -> Unit)? = null

    private var sw = 0f
    private var sh = 0f
    private var groundY = 0f
    private var railY = 0f
    private var craneZoneH = 0f
    private var bsz = 0f
    private var cellSz = 0f
    private var gridCols = 0

    private var grid = IntArray(0)
    private val boxAnims = mutableMapOf<Int, BoxAnim>()

    // Постоянное хранилище паттернов — ключ тот же что и boxAnims
    // Паттерн записывается один раз при создании и никогда не меняется
    private val boxPatterns = mutableMapOf<Int, BoxPattern>()

    private var gameRunning = false
    private var isPaused = false
    private var score = 0

    private val maxRows: Int get() = if (sh > sw) MAX_ROWS_PORTRAIT else MAX_ROWS_LANDSCAPE

    private var px = 0f
    private var py = 0f
    private var pvx = 0f
    private var pvy = 0f
    private var pw = 0f
    private var ph = 0f
    private var ponGround = true
    private var pfaceRight = true

    private val cranes = mutableListOf<FCrane>()
    private val falling = mutableListOf<FBox>()

    private var clearAnim = false
    private var clearFrames = 0
    private var clearRow = -1

    private var moveLeft = false
    private var moveRight = false
    private var jumpPressed = false
    private var gameThread: Thread? = null

    private var difficulty: Difficulty = Difficulty.MEDIUM
    fun setDifficulty(d: Difficulty) {
        difficulty = d
    }

    private var customDifficulty: CustomDifficulty? = null

    // setCustomDifficulty Переключает движок на использование кастомных параметров
    fun setCustomDifficulty(c: CustomDifficulty) {
        customDifficulty = c
    }

    // clearCustomDifficulty Возвращает движок к стандартной Difficulty
    fun clearCustomDifficulty() {
        customDifficulty = null
    }

    // effectiveMaxCranes Возвращает актуальный лимит грейферов с учётом режима
    private val effectiveMaxCranes: Int
        get() = customDifficulty?.maxCranes ?: difficulty.maxCranes

    // effectiveCraneSpeedMin Возвращает нижний порог скорости с учётом режима
    private val effectiveCraneSpeedMin: Float
        get() = customDifficulty?.craneSpeedMin ?: difficulty.craneSpeedMin

    // effectiveCraneSpeedMax Возвращает верхний порог скорости с учётом режима
    private val effectiveCraneSpeedMax: Float
        get() = customDifficulty?.craneSpeedMax ?: difficulty.craneSpeedMax

    // effectiveCanPushInJump Возвращает флаг прыжка-толчка с учётом режима
    private val effectiveCanPushInJump: Boolean
        get() = customDifficulty?.canPushInJump ?: difficulty.canPushInJump

    // effectiveCanPushChain Возвращает флаг цепочки с учётом режима
    private val effectiveCanPushChain: Boolean
        get() = customDifficulty?.canPushChain ?: difficulty.canPushChain

    // effectiveMaxPushChain Возвращает лимит цепочки с учётом режима
    private val effectiveMaxPushChain: Int
        get() = customDifficulty?.maxPushChain ?: difficulty.maxPushChain

    // Кисти
    private val bgP = Paint().apply { color = "#FF6600".toColorInt() }
    private val gndP = Paint().apply { color = "#CC5200".toColorInt() }
    private val gndL = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val boxP = Paint().apply { color = "#CC5200".toColorInt() }
    private val blinkP = Paint().apply { color = "#FFAA44".toColorInt() }
    private val boxStr = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val hilP = Paint().apply { color = "#44FFFFFF".toColorInt() }
    private val patP = Paint().apply {
        color = "#55000000".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
    }
    private val patFillP = Paint().apply {
        color = "#33000000".toColorInt()
        style = Paint.Style.FILL
    }
    private val railP = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val cBodyP = Paint().apply { color = "#993D00".toColorInt() }
    private val cStrP = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val cTrosP = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }
    private val plP = Paint().apply { color = Color.BLACK }
    private val helP = Paint().apply { color = "#FFAA00".toColorInt() }
    private val eyeP = Paint().apply { color = "#FF6600".toColorInt() }

    private var theme: ColorTheme = ColorTheme.CLASSIC

    // setTheme Обновляет тему и перерисовывает все кисти
    fun setTheme(t: ColorTheme) {
        theme = t
        updatePaints()
    }

    // updatePaints Синхронизирует цвета кистей с выбранной темой оформления
    private fun updatePaints() {
        bgP.color = theme.primaryBg
        gndP.color = theme.secondaryBg
        boxP.color = theme.secondaryBg
        gndL.color = theme.textStroke
        boxStr.color = theme.textStroke
        railP.color = theme.textStroke
        cStrP.color = theme.textStroke
        cTrosP.color = theme.textStroke
        plP.color = theme.textStroke
        helP.color = theme.accent

        // Дополнительные цвета
        blinkP.color = theme.accent
        cBodyP.color = theme.secondaryBg
        eyeP.color = theme.accent

        patP.color = Color.argb(
            85,
            Color.red(theme.textStroke),
            Color.green(theme.textStroke),
            Color.blue(theme.textStroke)
        )
        patFillP.color = Color.argb(
            51,
            Color.red(theme.textStroke),
            Color.green(theme.textStroke),
            Color.blue(theme.textStroke)
        )
        hilP.color = Color.argb(68, 255, 255, 255)
    }

    // setBestScore Устанавливает значение лучшего результата для отображения в игровом интерфейсе
    fun setBestScore(@Suppress("UNUSED_PARAMETER") b: Int) {}

    // setMoveLeft Управляет состоянием движения грузчика в левую сторону
    fun setMoveLeft(v: Boolean) {
        moveLeft = v
    }

    // setMoveRight Управляет состоянием движения грузчика в правую сторону
    fun setMoveRight(v: Boolean) {
        moveRight = v
    }

    // setJump Передает сигнал о нажатии кнопки прыжка в физический движок
    fun setJump(v: Boolean) {
        jumpPressed = v
    }

    // startGame Инициализирует состояние и запускает игровой поток
    fun startGame() {
        reset()
        gameRunning = true
        isPaused = false
        startLoop()
    }

    // pauseGame Приостанавливает расчеты физики и движение объектов
    fun pauseGame() {
        isPaused = true
    }

    // resumeGame Возвращает игру в активное состояние после паузы
    fun resumeGame() {
        isPaused = false
    }

    // stopGame Прекращает выполнение всех процессов для безопасного выхода
    fun stopGame() {
        gameRunning = false; gameThread?.interrupt(); gameThread = null
    }

    // onSizeChanged Пересчитывает параметры сетки и размеры объектов под новый экран
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        sw = w.toFloat(); sh = h.toFloat()
        val dp = resources.displayMetrics.density

        val isPortrait = sh > sw

        val gridCellDp = if (isPortrait) 10f else GRID_CELL_DP
        val idealCols = (sw / (gridCellDp * dp)).toInt()
        gridCols = if (idealCols % 2 == 0) idealCols else idealCols - 1

        cellSz = sw / gridCols
        bsz = cellSz * 2f

        grid = IntArray(gridCols)
        craneZoneH = sh * 0.15f
        railY = if (isPortrait) craneZoneH * 0.05f else craneZoneH * 0.14f
        groundY = sh - bsz * 0.35f
        pw = bsz * 0.68f
        ph = bsz * 0.85f
        if (!gameRunning) reset()
    }

    // colLeft Вычисляет горизонтальную координату начала указанной колонки
    private fun colLeft(col: Int) = col * cellSz

    // rowTop Вычисляет вертикальную координату верхней границы указанного ряда
    private fun rowTop(row: Int) = groundY - (row + 1) * cellSz

    // xToCol Преобразует экранную координату X в соответствующий индекс колонки
    private fun xToCol(x: Float) = (x / cellSz).toInt().coerceIn(0, gridCols - 1)

    // animKey Создаёт составной ключ для быстрого поиска анимации конкретного ящика
    private fun animKey(col: Int, row: Int) = col * 10000 + row

    // getOrCreatePattern Возвращает существующий узор ящика или создает новый для постоянства вида
    private fun getOrCreatePattern(key: Int): BoxPattern {
        return boxPatterns.getOrPut(key) {
            BoxPattern.entries.let { it[Random.nextInt(it.size)] }
        }
    }

    // reset Обнуляет все игровые параметры для нового раунда
    private fun reset() {
        score = 0
        if (grid.size != gridCols) grid = IntArray(gridCols)
        grid.fill(0)
        boxAnims.clear()
        boxPatterns.clear()
        falling.clear(); cranes.clear()
        clearAnim = false; clearFrames = 0; clearRow = -1
        px = sw / 2f - pw / 2f; py = groundY - ph
        pvx = 0f; pvy = 0f; ponGround = true; pfaceRight = true
        moveLeft = false; moveRight = false; jumpPressed = false
        spawnCranes()
    }

    // spawnCranes Заполняет список грейферов согласно сложности
    private fun spawnCranes() {
        repeat(effectiveMaxCranes) { i ->
            val spd = rndSpeed()
            val fl = i % 2 == 0
            val startX = if (fl) -bsz * 2 - i * sw * 0.40f
            else sw + bsz * 2 + i * sw * 0.40f
            cranes.add(
                FCrane(
                    cx = startX, speed = if (fl) spd else -spd, dropAtX = randomDropX()
                )
            )
        }
    }

    // rndSpeed Вычисляет случайную скорость с нелинейным распределением для динамики
    private fun rndSpeed(): Float {
        val t = Random.nextFloat()
        return effectiveCraneSpeedMin + t * t * (effectiveCraneSpeedMax - effectiveCraneSpeedMin)
    }


    // randomDropX Выбирает случайную колонку для сброса, защищая края игрового поля
    private fun randomDropX(): Float {
        val raw = Random.nextFloat() * sw
        val col = (xToCol(raw) / 2) * 2

        val edgeColL1 = 0
        val edgeColL2 = 2
        val edgeColR2 = gridCols - 4
        val edgeColR1 = gridCols - 2

        // Уменьшение вероятности падения ящика по краям экрана
        val isPortrait = sh > sw
        val chanceL1 = if (isPortrait) 0.06f else 0.05f // Крайняя левая: портрет 6%, ландшафт 5%
        val chanceL2 = if (isPortrait) 0.03f else 0.02f // Вторая слева: портрет 3%, ландшафт 2%
        val chanceR1 = if (isPortrait) 0.06f else 0.05f // Крайняя правая: портрет 6%, ландшафт 5%
        val chanceR2 = if (isPortrait) 0.03f else 0.03f // Вторая справа: портрет 3%, ландшафт 3%

        when (col) {
            edgeColL1 -> {
                if (Random.nextFloat() < chanceL1) return randomDropX()
            }

            edgeColL2 -> {
                if (Random.nextFloat() < chanceL2) return randomDropX()
            }

            edgeColR1 -> {
                if (Random.nextFloat() < chanceR1) return randomDropX()
            }

            edgeColR2 -> {
                if (Random.nextFloat() < chanceR2) return randomDropX()
            }
        }

        return raw
    }

    // startLoop Запускает фоновый поток для стабильной частоты обновления кадров
    private fun startLoop() {
        gameThread?.interrupt()
        gameThread = Thread {
            val ms = 1000L / 60L
            while (gameRunning && !Thread.currentThread().isInterrupted) {
                val t0 = System.currentTimeMillis()
                if (!isPaused) {
                    synchronized(this) { tick() }; postInvalidate()
                }
                val sl = ms - (System.currentTimeMillis() - t0)
                if (sl > 0) try {
                    Thread.sleep(sl)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }.also { it.start() }
    }

    // tick Выполняет расчет физики и состояний всех объектов за один такт
    private fun tick() {
        moveCranes()
        moveFalling()
        movePlayer()
        updateBoxAnims()
        checkClear()
        tickClearAnim()
    }

    // updateBoxAnims Обновляет визуальные координаты ящиков для плавного скольжения
    private fun updateBoxAnims() {
        val toRemove = boxAnims.keys.filter { key ->
            val col = key / 10000
            val row = key % 10000
            col >= gridCols || row >= grid[col]
        }
        toRemove.forEach { boxAnims.remove(it) }

        for (col in 0 until gridCols step 2) {
            val boxRows = grid[col] / 2
            for (boxRow in 0 until boxRows) {
                val row = boxRow * 2
                val key = animKey(col, row)
                val targetX = colLeft(col)
                val targetY = rowTop(row + 1)

                val anim = boxAnims[key]
                if (anim == null) {
                    val pattern = getOrCreatePattern(key)
                    boxAnims[key] = BoxAnim(col, row, targetX, targetY, pattern)
                } else {
                    val dx = targetX - anim.visualX
                    val dy = targetY - anim.visualY
                    anim.visualX += dx * BOX_SLIDE_SPEED
                    anim.visualY += dy * BOX_SLIDE_SPEED
                    if (abs(dx) < 0.15f) anim.visualX = targetX
                    if (abs(dy) < 0.15f) anim.visualY = targetY
                }
            }
        }
    }

    // moveCranes Управляет движением грейферов и их повторным появлением
    private fun moveCranes() {
        val rem = mutableListOf<FCrane>()
        val add = mutableListOf<FCrane>()
        for (c in cranes) {
            c.cx += c.speed
            // Плавно раскрывает клешни после сброса ящика
            if (c.dropped && c.jawAngle < 1f) c.jawAngle = min(1f, c.jawAngle + 0.04f)
            if (!c.dropped && c.hasBox) {
                val passedDrop =
                    (c.speed > 0 && c.cx >= c.dropAtX) || (c.speed < 0 && c.cx <= c.dropAtX)
                if (passedDrop && c.dropAtX in 0f..sw) dropBox(c)
            }
            val gone = (c.speed > 0 && c.cx > sw + bsz * 3) || (c.speed < 0 && c.cx < -bsz * 3)
            if (gone) {
                rem.add(c)
                // Новый кран добавляем только если не превышен лимит
                if (cranes.size - rem.size + add.size < effectiveMaxCranes) {
                    val spd = rndSpeed()
                    val fl = c.speed > 0
                    val others = cranes.filter { it !== c }.map { it.cx }
                    add.add(
                        FCrane(
                            cx = if (fl) min(-bsz * 2, (others.minOrNull() ?: 0f) - sw * 0.35f)
                            else max(sw + bsz * 2, (others.maxOrNull() ?: sw) + sw * 0.35f),
                            speed = if (fl) spd else -spd,
                            dropAtX = randomDropX()
                        )
                    )
                }
            }
        }
        cranes.removeAll(rem); cranes.addAll(add)
    }

    // dropBox Переводит ящик из состояния захвата в состояние свободного падения
    private fun dropBox(c: FCrane) {
        c.dropped = true; c.hasBox = false
        val col = (xToCol(c.dropAtX) / 2) * 2
        val exactCx = colLeft(col) + bsz / 2f
        // Рандомная скорость падения ящиков
        val fallSpeed =
            BOX_FALL_SPEED_MIN + Random.nextFloat() * (BOX_FALL_SPEED_MAX - BOX_FALL_SPEED_MIN)
        falling.add(
            FBox(
                cx = exactCx,
                top = craneZoneH,
                col = col,
                sz = bsz,
                speed = fallSpeed,
                pattern = c.pattern
            )
        )
    }

    // moveFalling Рассчитывает траекторию падающих ящиков и их столкновения
    private fun moveFalling() {
        val rem = mutableListOf<FBox>()
        for (fb in falling) {
            fb.top += fb.speed

            val overX = fb.right > px + pw * 0.15f && fb.left < px + pw * 0.85f
            val hitsHead =
                fb.bottom >= py && fb.bottom <= py + ph * 0.55f && fb.top <= py + ph * 0.4f
            val hitsBottom = py <= fb.bottom && py >= fb.top && pvy < 0
            if (overX && (hitsHead || hitsBottom)) {
                gameRunning = false; post { onGameOver?.invoke(score) }
                return
            }

            val landY = groundY - grid[fb.col] * cellSz
            if (fb.bottom >= landY) {
                val newRow = grid[fb.col]
                grid[fb.col] += 2
                grid[fb.col + 1] += 2
                val key = animKey(fb.col, newRow)
                boxPatterns[key] = fb.pattern
                boxAnims[key] = BoxAnim(
                    col = fb.col,
                    row = newRow,
                    visualX = fb.cx - bsz / 2f,
                    visualY = fb.top,
                    pattern = fb.pattern
                )
                rem.add(fb)
                post { onLand?.invoke() }
                score += 1
                post { onScoreChanged?.invoke(score) }
                if (grid[fb.col] / 2 >= maxRows) {
                    gameRunning = false; post { onGameOver?.invoke(score) }; return
                }
            }
        }
        falling.removeAll(rem)
    }

    // movePlayer Обрабатывает ввод пользователя и перемещает грузчика
    private fun movePlayer() {
        pvx = when {
            moveLeft && !moveRight -> -PLAYER_SPEED
            moveRight && !moveLeft -> PLAYER_SPEED
            else -> 0f
        }
        if (pvx < 0) pfaceRight = false
        if (pvx > 0) pfaceRight = true
        if (jumpPressed && ponGround) {
            pvy = JUMP_VELOCITY; ponGround = false; post { onJump?.invoke() }
        }
        jumpPressed = false
        pvy += GRAVITY
        px += pvx
        px = px.coerceIn(0f, sw - pw)
        resolvePlayerH()
        py += pvy
        resolvePlayerV()
        if (py < craneZoneH) {
            py = craneZoneH; pvy = 0f
        }
    }

    // resolvePlayerH Проверяет горизонтальные коллизии с ящиками и инициирует толкание
    private fun resolvePlayerH() {
        if (pvx == 0f) return
        val pT = py
        val pB = py + ph

        // Стопки
        var pushed = false
        for (col in 0 until gridCols step 2) {
            if (pushed) break
            if (grid[col] == 0) continue

            val boxRows = grid[col] / 2
            for (boxRow in boxRows - 1 downTo 0) {
                val row = boxRow * 2
                val key = animKey(col, row)
                val anim = boxAnims[key]

                val visL = anim?.visualX ?: colLeft(col)
                val visR = visL + bsz
                val visT = anim?.visualY ?: rowTop(row + 1)
                val visB = visT + bsz

                if (pB <= visT + 2f || pT >= visB - 2f) continue

                val pL = px
                val pR = px + pw

                if (pvx > 0f && pR > visL + 2f && pL < visL) {
                    px = visL - pw
                    if (boxRow == boxRows - 1 && pB > visT + bsz * 0.1f) {
                        tryPushCol(col, +1)
                        pushed = true
                    }
                    break
                } else if (pvx < 0f && pL < visR - 2f && pR > visR) {
                    px = visR
                    if (boxRow == boxRows - 1 && pB > visT + bsz * 0.1f) {
                        tryPushCol(col, -1)
                        pushed = true
                    }
                    break
                }
            }
        }

        // Падающие ящики
        for (fb in falling.toList()) {
            val fL = fb.cx - bsz / 2f
            val fR = fb.cx + bsz / 2f
            val fT = fb.top
            val fB = fb.bottom

            val pL = px
            val pR = px + pw

            // Горизонтальное перекрытие: игрок движется в сторону ящика
            val hOver = if (pvx > 0f) pR > fL + 2f && pL < fL
            else pL < fR - 2f && pR > fR
            if (!hOver) continue
            val vOverSide = pB > fT && pT < fB
            if (!vOverSide) continue

            // Прекращает игру при попытке толкнуть падающий ящик в прыжке на высокой сложности
            if (!ponGround && !effectiveCanPushInJump) {
                gameRunning = false; post { onGameOver?.invoke(score) }; return
            }

            // Доп. защита от смерти: если игрок движется вверх (pvy < 0)
            // и горизонтально входит в ящик сбоку — это боковое столкновение, а не удар снизу.
            // Смерть обрабатывается только в moveFalling через hitsBottom/hitsHead.
            px = if (pvx > 0f) fL - pw else fR

            val newCol = if (pvx > 0f) fb.col + 2 else fb.col - 2
            if (newCol < 0 || newCol + 1 >= gridCols) continue

            fb.col = newCol
            fb.cx = colLeft(newCol) + bsz / 2f
        }

        px = px.coerceIn(0f, sw - pw)
    }

    // resolvePlayerV Обеспечивает приземление на поверхности и столкновения сверху/снизу
    private fun resolvePlayerV() {
        ponGround = false

        if (py + ph >= groundY) {
            py = groundY - ph; pvy = 0f; ponGround = true; return
        }

        val colMin = xToCol(px + pw * 0.15f)
        val colMax = xToCol(px + pw * 0.85f)

        if (pvy >= 0f) {
            var bestSurface = groundY
            for (c in colMin..colMax) {
                if (c >= gridCols) continue
                val baseCol = (c / 2) * 2
                if (baseCol >= gridCols || grid[baseCol] == 0) continue
                val topBoxRow = ((grid[baseCol] / 2) - 1) * 2
                val key = animKey(baseCol, topBoxRow)
                val anim = boxAnims[key]
                val visTop = anim?.visualY ?: rowTop(topBoxRow + 1)
                if (visTop < bestSurface) bestSurface = visTop
            }
            if (py + ph >= bestSurface - 1f && py + ph <= bestSurface + pvy + 8f) {
                py = bestSurface - ph; pvy = 0f; ponGround = true; return
            }
        }

        if (pvy < 0f) {
            for (c in colMin..colMax) {
                if (c >= gridCols) continue
                val baseCol = (c / 2) * 2
                if (baseCol >= gridCols || grid[baseCol] == 0) continue
                val topBoxRow = ((grid[baseCol] / 2) - 1) * 2
                val key = animKey(baseCol, topBoxRow)
                val anim = boxAnims[key]
                val visTop = anim?.visualY ?: rowTop(topBoxRow + 1)
                if (py <= visTop && py >= visTop + pvy - 4f) {
                    py = visTop; pvy = 0f; break
                }
            }
        }

        for (c in colMin..colMax) {
            if (c >= gridCols) continue
            val baseCol = (c / 2) * 2
            if (baseCol >= gridCols) continue
            val surface = if (grid[baseCol] > 0) {
                val topBoxRow = ((grid[baseCol] / 2) - 1) * 2
                val key = animKey(baseCol, topBoxRow)
                boxAnims[key]?.visualY ?: rowTop(topBoxRow + 1)
            } else groundY
            if (abs(py + ph - surface) < 4f) {
                ponGround = true; break
            }
        }
    }

    // tryPushCol Реализует групповое перемещение ящиков по горизонтали
    private fun tryPushCol(col: Int, dir: Int): Boolean {
        if (grid[col] == 0) return false

        // Начинает с col, идёт в направлении dir, пока ящик существует и мешает
        val chainCols = mutableListOf<Int>()
        var c = col
        while (true) {
            if (c + 1 >= gridCols) break
            if (grid[c] == 0) break
            chainCols.add(c)
            if (chainCols.size >= effectiveMaxPushChain) break
            val next = c + dir * 2

            // Следующий ящик мешает только если он на том же уровне или выше
            if (next < 0 || next + 1 >= gridCols) break

            // Определяет высоту верхнего ящика текущей колонки
            val curTopRow = ((grid[c] / 2) - 1) * 2

            // Если в следующей колонке есть ящик на этом уровне — он тоже в цепи
            if (grid[next] > curTopRow) {
                c = next
            } else {
                break
            }
        }

        if (chainCols.isEmpty()) return false

        // На EXTREME нельзя толкать цепочку из 2+ ящиков
        if (!effectiveCanPushChain && chainCols.size > 1) return false

        // Высота верхнего ящика первого элемента цепи (это и есть "уровень толчка")
        val pushRow = ((grid[chainCols.first()] / 2) - 1) * 2

        // Проверяет что за последним ящиком цепи есть место для сдвига
        val lastCol = chainCols.last()
        val lastTarget = lastCol + dir * 2
        if (lastTarget < 0 || lastTarget + 1 >= gridCols) return false

        // Проверяет что за цепью нет ещё одного мешающего ящика
        if (grid[lastTarget] > pushRow) return false

        // Целевая высота не должна быть выше pushRow+2 (нельзя закинуть ящик на стопку выше)
        data class Move(
            val oldCol: Int, val oldRow: Int, val newCol: Int, val newRow: Int
        )

        val moves = mutableListOf<Move>()
        for (cc in chainCols) {
            val boxRow = ((grid[cc] / 2) - 1) * 2
            val newCol = cc + dir * 2
            if (newCol < 0 || newCol + 1 >= gridCols) return false
            val newRow = grid[newCol]
            if (newRow > pushRow + 2) return false
            moves.add(Move(cc, boxRow, newCol, newRow))
        }

        // Сохраняет все паттерны старых ящиков
        val savedPatterns: List<BoxPattern> = moves.map { m ->
            val oldKey = animKey(m.oldCol, m.oldRow)
            boxPatterns[oldKey] ?: boxAnims[oldKey]?.pattern ?: BoxPattern.CROSS
        }

        for (m in moves.reversed()) {
            grid[m.oldCol] -= 2
            grid[m.oldCol + 1] -= 2
            grid[m.newCol] += 2
            grid[m.newCol + 1] += 2
        }

        val correctedMoves = moves.map { m ->
            m.copy(newRow = grid[m.newCol] - 2)
        }

        // Удаляет старые записи
        for (m in correctedMoves) {
            val oldKey = animKey(m.oldCol, m.oldRow)
            boxAnims.remove(oldKey)
        }

        // Создаёт новые записи с правильными ключами
        for ((index, m) in correctedMoves.withIndex()) {
            val newKey = animKey(m.newCol, m.newRow)
            val pattern = savedPatterns[index]
            boxPatterns[newKey] = pattern
            boxAnims[newKey] = BoxAnim(
                col = m.newCol,
                row = m.newRow,
                visualX = colLeft(m.newCol),
                visualY = rowTop(m.newRow + 1),
                pattern = pattern
            )
        }

        // Очки за сброс ящика на более низкий ряд
        for (m in moves) {
            val dropped = (m.oldRow / 2) - (m.newRow / 2)
            if (dropped > 0) {
                score += dropped * 2
                post { onScoreChanged?.invoke(score) }
            }
        }

        return true
    }

    // checkClear Проверяет наличие полностью заполненных рядов для удаления
    private fun checkClear() {
        if (clearAnim) return
        val minH = if (grid.isEmpty()) 0 else grid.minOrNull() ?: 0
        if (minH <= 0) return
        for (row in 0 until minH step 2) {
            var full = true
            for (col in 0 until gridCols step 2) {
                if (grid[col] <= row) {
                    full = false; break
                }
            }
            if (full) {
                clearRow = row; clearAnim = true; clearFrames = 20; return
            }
        }
    }

    // tickClearAnim Управляет таймером анимации исчезновения ряда ящиков
    private fun tickClearAnim() {
        if (!clearAnim) return
        clearFrames--
        if (clearFrames <= 0) {
            // Чем выше ряд — тем больше очков (ряд 0 = нижний = +20, ряд 2 = +40 и т.д.)
            val rowBonus = (clearRow / 2 + 1) * 20
            for (col in 0 until gridCols) {
                if (grid[col] > clearRow) grid[col] -= 2
            }
            boxAnims.clear()
            boxPatterns.clear()
            score += rowBonus
            post { onScoreChanged?.invoke(score) }
            clearAnim = false; clearRow = -1
        }
    }

    // onDraw Выводит актуальное состояние игровых объектов на экран устройства
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (sw == 0f || gridCols == 0) return
        // Блокирует доступ к данным на время отрисовки для предотвращения артефактов
        synchronized(this) {
            canvas.drawRect(0f, 0f, sw, sh, bgP)
            drawCranes(canvas)
            drawGridBoxes(canvas)
            drawFallingBoxes(canvas)
            drawGround(canvas)
            drawPlayer(canvas)
        }
    }

    // drawGridBoxes Отрисовывает все ящики, находящиеся в сетке игрового поля
    private fun drawGridBoxes(canvas: Canvas) {
        val blinking = clearAnim && clearFrames % 5 < 3
        for (col in 0 until gridCols step 2) {
            val boxRows = grid[col] / 2
            for (boxRow in 0 until boxRows) {
                val row = boxRow * 2
                val paint = if (clearAnim && row == clearRow && blinking) blinkP else boxP
                val key = animKey(col, row)
                val anim = boxAnims[key]
                val l = anim?.visualX ?: colLeft(col)
                val t = anim?.visualY ?: rowTop(row + 1)
                val pattern = anim?.pattern ?: BoxPattern.CROSS
                drawBox(canvas, l, t, l + bsz, t + bsz, paint, pattern)
            }
        }
    }

    // drawBox Рисует один ящик с тенями, бликами и выбранным узором
    private fun drawBox(
        canvas: Canvas,
        l: Float,
        t: Float,
        r: Float,
        b: Float,
        fill: Paint,
        pattern: BoxPattern = BoxPattern.CROSS
    ) {
        // Фон и обводка
        canvas.drawRoundRect(l, t, r, b, 3f, 3f, fill)
        canvas.drawRoundRect(l, t, r, b, 3f, 3f, boxStr)
        // Блик сверху
        canvas.drawRect(l + 3f, t + 3f, r - 3f, t + (b - t) * 0.18f, hilP)

        val pad = (r - l) * 0.12f
        val il = l + pad
        val ir = r - pad
        val it2 = t + pad
        val ib = b - pad
        val cx = (l + r) / 2f
        val cy = (t + b) / 2f

        when (pattern) {
            BoxPattern.CROSS -> {
                // Классическое перекрестье
                canvas.drawLine(il, it2, ir, ib, patP)
                canvas.drawLine(ir, it2, il, ib, patP)
            }

            BoxPattern.STRIPES_H -> {
                // Три горизонтальные полосы
                val step = (ib - it2) / 4f
                canvas.drawLine(il, it2 + step, ir, it2 + step, patP)
                canvas.drawLine(il, it2 + step * 2f, ir, it2 + step * 2f, patP)
                canvas.drawLine(il, it2 + step * 3f, ir, it2 + step * 3f, patP)
            }

            BoxPattern.STRIPES_V -> {
                // Три вертикальные полосы
                val step = (ir - il) / 4f
                canvas.drawLine(il + step, it2, il + step, ib, patP)
                canvas.drawLine(il + step * 2f, it2, il + step * 2f, ib, patP)
                canvas.drawLine(il + step * 3f, it2, il + step * 3f, ib, patP)
            }

            BoxPattern.DIAMOND -> {
                // Ромб
                val path = Path().apply {
                    moveTo(cx, it2)
                    lineTo(ir, cy)
                    lineTo(cx, ib)
                    lineTo(il, cy)
                    close()
                }
                canvas.drawPath(path, patFillP)
                canvas.drawPath(path, patP)
            }

            BoxPattern.DOTS -> {
                // Четыре точки по углам + центр
                val r2 = (ir - il) * 0.09f
                canvas.drawCircle(il + r2, it2 + r2, r2, patFillP)
                canvas.drawCircle(ir - r2, it2 + r2, r2, patFillP)
                canvas.drawCircle(il + r2, ib - r2, r2, patFillP)
                canvas.drawCircle(ir - r2, ib - r2, r2, patFillP)
                canvas.drawCircle(cx, cy, r2, patFillP)
                canvas.drawCircle(il + r2, it2 + r2, r2, patP)
                canvas.drawCircle(ir - r2, it2 + r2, r2, patP)
                canvas.drawCircle(il + r2, ib - r2, r2, patP)
                canvas.drawCircle(ir - r2, ib - r2, r2, patP)
                canvas.drawCircle(cx, cy, r2, patP)
            }

            BoxPattern.ZIGZAG -> {
                // Зигзаг по горизонтали
                val path = Path().apply {
                    val step = (ir - il) / 4f
                    moveTo(il, cy)
                    lineTo(il + step, it2)
                    lineTo(il + step * 2f, cy)
                    lineTo(il + step * 3f, it2)
                    lineTo(ir, cy)
                }
                canvas.drawPath(path, patP)
                val path2 = Path().apply {
                    val step = (ir - il) / 4f
                    moveTo(il, cy)
                    lineTo(il + step, ib)
                    lineTo(il + step * 2f, cy)
                    lineTo(il + step * 3f, ib)
                    lineTo(ir, cy)
                }
                canvas.drawPath(path2, patP)
            }
        }
    }

    // drawFallingBoxes Отображает ящики, которые в данный момент находятся в воздухе
    private fun drawFallingBoxes(canvas: Canvas) {
        for (fb in falling) {
            val l = fb.cx - bsz / 2f
            drawBox(canvas, l, fb.top, l + bsz, fb.top + bsz, boxP, fb.pattern)
        }
    }

    // drawCranes Отрисовывает рельсу и все активные грейферы
    private fun drawCranes(canvas: Canvas) {
        canvas.drawLine(0f, railY, sw, railY, railP)
        for (c in cranes) drawCrane(canvas, c)
    }

    // drawCrane Рисует корпус, трос и клешни конкретного грейфера
    private fun drawCrane(canvas: Canvas, c: FCrane) {
        val cx = c.cx
        val bw = bsz * 0.85f
        val bh = bsz * 0.38f
        val wR = bh * 0.32f
        val cabB = railY + bh
        canvas.drawCircle(cx - bw * 0.28f, railY, wR, cBodyP)
        canvas.drawCircle(cx - bw * 0.28f, railY, wR, cStrP)
        canvas.drawCircle(cx + bw * 0.28f, railY, wR, cBodyP)
        canvas.drawCircle(cx + bw * 0.28f, railY, wR, cStrP)
        canvas.drawRect(cx - bw / 2f, railY + wR, cx + bw / 2f, cabB, cBodyP)
        canvas.drawRect(cx - bw / 2f, railY + wR, cx + bw / 2f, cabB, cStrP)
        val trosBot = cabB + bsz * 0.30f
        canvas.drawLine(cx, cabB, cx, trosBot, cTrosP)
        if (c.hasBox) drawGripClosed(canvas, cx, trosBot, c.pattern)
        else drawGripOpen(canvas, cx, trosBot, c.jawAngle)
    }

    // drawGripClosed Рисует захват в закрытом состоянии вместе с ящиком
    private fun drawGripClosed(canvas: Canvas, cx: Float, y: Float, pattern: BoxPattern) {
        val jw = bsz * 0.14f
        val jh = bsz * 0.55f
        val hs = bsz / 2f
        canvas.drawLine(cx - hs - jw / 2f, y, cx + hs + jw / 2f, y, cTrosP)
        drawJaw(canvas, cx - hs - jw, y, jw, jh, true, 0f)
        drawJaw(canvas, cx + hs, y, jw, jh, false, 0f)
        drawBox(canvas, cx - hs, y, cx - hs + bsz, y + bsz, boxP, pattern)
    }

    // drawGripOpen Рисует раскрывающиеся когти после сброса груза
    private fun drawGripOpen(canvas: Canvas, cx: Float, y: Float, angle: Float) {
        val jw = bsz * 0.14f
        val jh = bsz * 0.55f
        val hs = bsz / 2f
        canvas.drawLine(cx - hs - jw / 2f, y, cx + hs + jw / 2f, y, cTrosP)
        drawJaw(canvas, cx - hs - jw, y, jw, jh, true, angle)
        drawJaw(canvas, cx + hs, y, jw, jh, false, angle)
    }

    // drawJaw Рисует одну створку челюсти с учётом угла поворота
    private fun drawJaw(
        canvas: Canvas, lx: Float, ly: Float, jw: Float, jh: Float, isLeft: Boolean, angle: Float
    ) {
        // Основа челюсти (вертикальная штанга)
        canvas.drawRoundRect(lx, ly, lx + jw, ly + jh, jw / 3f, jw / 3f, cBodyP)
        canvas.drawRoundRect(lx, ly, lx + jw, ly + jh, jw / 3f, jw / 3f, cStrP)

        val pivX = lx + jw / 2f
        val pivY = ly + jh

        // Коготь
        val clW = jw * 2.0f // Длина когтя
        val clH = jw * 0.55f // Толщина у основания

        val startDeg = if (isLeft) 0f else 180f
        val endDeg = 90f
        val deg = startDeg + (endDeg - startDeg) * angle

        // Рисует коготь как заострённый треугольник (Path)
        val clawPath = Path().apply {
            // Основание у шарнира (широкое)
            moveTo(pivX, pivY - clH / 2f)
            // Острый конец
            lineTo(pivX + clW, pivY)
            // Основание снизу
            lineTo(pivX, pivY + clH / 2f)
            close()
        }
        canvas.withRotation(deg, pivX, pivY) {
            drawPath(clawPath, cBodyP)
            drawPath(clawPath, cStrP)
        }
    }

    // drawGround Отрисовывает поверхность земли под стопками ящиков
    private fun drawGround(canvas: Canvas) {
        canvas.drawRect(0f, groundY, sw, sh, gndP)
        canvas.drawLine(0f, groundY, sw, groundY, gndL)
    }

    // drawPlayer Отрисовывает грузчика со всеми деталями: каска, глаз и руки
    private fun drawPlayer(canvas: Canvas) {
        val bT = py + ph * 0.30f
        val bB = py + ph * 0.72f
        canvas.drawRect(px + pw * 0.18f, bT, px + pw * 0.82f, bB, plP)
        canvas.drawOval(px + pw * 0.20f, py, px + pw * 0.80f, py + ph * 0.34f, plP)
        canvas.drawArc(
            px + pw * 0.12f,
            py - ph * 0.04f,
            px + pw * 0.88f,
            py + ph * 0.22f,
            180f,
            180f,
            true,
            helP
        )
        val ex = if (pfaceRight) px + pw * 0.62f else px + pw * 0.38f
        canvas.drawCircle(ex, py + ph * 0.15f, pw * 0.09f, eyeP)
        val lo =
            if (pvx != 0f && ponGround) (System.currentTimeMillis() / 110 % 2).toInt() * ph * 0.10f else 0f
        canvas.drawRect(px + pw * 0.22f, bB, px + pw * 0.44f, py + ph + lo, plP)
        canvas.drawRect(px + pw * 0.56f, bB, px + pw * 0.78f, py + ph - lo, plP)
        if (pfaceRight) canvas.drawRect(
            px + pw * 0.78f,
            bT + ph * 0.06f,
            px + pw + 2f,
            bT + ph * 0.20f,
            plP
        )
        else canvas.drawRect(px - 2f, bT + ph * 0.06f, px + pw * 0.22f, bT + ph * 0.20f, plP)
    }
}