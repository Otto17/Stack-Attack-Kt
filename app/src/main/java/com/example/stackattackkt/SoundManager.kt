// Copyright (c) 2026 Otto
// License: GPL-2.0-or-later (see LICENSE)

package com.example.stackattackkt

import android.content.Context
import android.media.SoundPool

// SoundManager Обеспечивает воспроизведение звуковых эффектов без задержек
class SoundManager(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    private var soundJump: Int = 0
    private var soundBox: Int = 0
    private var loaded = false

    init {
        // Устанавливает флаг готовности только после полной загрузки всех ресурсов
        soundPool.setOnLoadCompleteListener { _, _, _ -> loaded = true }
        soundJump = soundPool.load(context, R.raw.jump, 1)
        soundBox = soundPool.load(context, R.raw.box, 1)
    }

    // playJump Запускает звук прыжка грузчика
    fun playJump() {
        if (loaded) soundPool.play(soundJump, 1f, 1f, 0, 0, 1f)
    }

    // playLand Запускает звук падения ящика на землю или стопку
    fun playLand() {
        if (loaded) soundPool.play(soundBox, 1f, 1f, 0, 0, 1f)
    }

    // release Освобождает системные ресурсы аудио-движка при уничтожении активности
    fun release() {
        soundPool.release()
    }
}