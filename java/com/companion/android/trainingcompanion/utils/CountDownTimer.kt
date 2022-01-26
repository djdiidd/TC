package com.companion.android.trainingcompanion.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.TextView
import com.companion.android.trainingcompanion.R

/**
 * Сервис таймер, использующий broadCast
 */
class CountDownTimer(private val context: Context, private val serviceIntent: Intent) {

    private var time: Int = 1       // Общее время (в секундах);
    private var timeIsGoing = false // Идет ли счет времени;
    private var finished = false    // Закончился ли таймер;
    private var clockView: TextView? = (context as Activity).findViewById(R.id.set_timer)

    /**
     * Получение времени в формате "ММ:СС"
     */
    fun getTimeInFormatMMSS(time: Int): String {
        time.also {
            return String.format(
                "%02d:%02d",
                it % 86400 % 3600 / 60,
                it % 86400 % 3600 % 60
            )
        }
    }
    // TODO: НЕ ПРОТЕСТИРОВАНО
    /** Установка нового времени */
    fun setTime(newTime: Int) {
        time = newTime
    }

    /** Идет ли счет времени */
    fun isGoing() = timeIsGoing

    /** Установить, что счет времени идет/не идет */
    fun setGoing(isGoing: Boolean) {
        timeIsGoing = isGoing
    }

    /** Получение оставшегося времени */
    fun getRemaining() = time

    /**
     * Объект, который будет сохранять полученное
     * значение и отображать его на экране
     */
    val timeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            time = intent.getIntExtra(CountDownService.TIME_EXTRA, 0)
            updateText()
            if (time == 0) finished = true
        }
    }

    /**
     * Запуск секундомера если он не запущен и,
     * соответственно, остановка, если он запущен
     */
    fun startOrStop() {
        if (timeIsGoing) stop()
        else start()
    }

    /**
     * Запуск секундомера посредством запуска сервиса
     * с переданным интентом в виде значения текущего времени
     */
    private fun start() {
        if (time <= 0) { return } else { finished = false }
        serviceIntent.putExtra(CountDownService.TIME_EXTRA, time)
        context.startService(serviceIntent)
        timeIsGoing = true
    }

    /**
     * Остановка секундомера путем вызова stopService(serviceIntent), после которого будет прекращен
     */
    private fun stop() {
        context.stopService(serviceIntent)
        timeIsGoing = false
    }

    private fun updateText() {
        clockView?.text = getTimeInFormatMMSS(time)
    }
}