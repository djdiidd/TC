package com.companion.android.trainingcompanion.utils

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.util.*

/**
 * Класс для работы основного секундомера в фоновом режиме
 */
class StopwatchService: Service() {
    override fun onBind(p0: Intent?): IBinder? = null

    // Экземпляр класса для планирования выполнения задачи
    private val timer = Timer()

    /**
     * Данный метод начнет свою работу только тогда, когда будет вызвана startService(Intent)
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val time = intent.getIntExtra(TIME_EXTRA, 0)
        // Отправка обновленного времени на 1 секунду каждую секунду с задержкой 0
        timer.scheduleAtFixedRate(TimeTask(time), 0, 1000)
        return START_NOT_STICKY
    }

    /**
     * После уничтожения приложения, отменим нашу длительную операцию обновления времени
     */
    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        Log.d("MyTag", "SW Destroyed")
    }

    /**
     * Внутренний класс, который выполняет обновление времени
     */
    private inner class TimeTask(private var time: Int): TimerTask() {
        /** Создаем новый интент и обновляем время */
        override fun run() {
            val intent = Intent(TIMER_UPDATED)
            time++ // Обновляем полученное время на секунду
            intent.putExtra(TIME_EXTRA, time)
            // Отправляем интент с временем, который будет получен
            sendBroadcast(intent) //  в TimeViewModel.updateTime (Receiver)
        }
    }

    /**
     * Использование констант извне
     */
    companion object {
        const val TIMER_UPDATED = "timerUpdated"
        const val TIME_EXTRA = "timerExtra"
    }
}