package com.companion.android.trainingcompanion.utils

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.util.*

class CDTimerService : Service() {

    override fun onBind(p: Intent?): IBinder? = null

    // Экземпляр класса для планирования выполнения задачи
    private val timer = Timer()

    override fun onCreate() {
        super.onCreate()
        Log.d("service", "created")
    }

    /**
     * Данный метод начнет свою работу только тогда, когда будет вызвана startService(Intent)
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val time = intent.getIntExtra(TIME_EXTRA, 0)
        if (time <= 0) stopSelf(startId)
        Log.d("MyTag", "Service started")
        // Отправка обновленного времени на 1 секунду каждую секунду с задержкой 0
        timer.scheduleAtFixedRate(TimeTask(time), 0, 1000)
        Log.d("MyTag", "Command started")
        return START_NOT_STICKY
    }

    // срабатывает при паузе
    override fun onDestroy() {
        timer.cancel()
        super.onDestroy()
    }

    /**
     * Внутренний класс, который выполняет обновление времени
     */
    private inner class TimeTask(private var time: Int): TimerTask() {
        /** Создаем новый интент и обновляем время */
        override fun run() {
            val intent = Intent(TIMER_UPDATED)
            time-- // Обновляем полученное время на секунду
            if (time <= -1)
                timer.cancel()
            intent.putExtra(TIME_EXTRA, time)
            // Отправляем интент с временем, который будет получен
            sendBroadcast(intent) //  в TimeViewModel.updateTime (Receiver)
        }
    }

    /**
     * Использование констант извне
     */
    companion object {
        const val TIMER_UPDATED = "timerUpdated-cd"
        const val TIME_EXTRA = "timerExtra-cd"
    }

}