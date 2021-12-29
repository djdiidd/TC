package com.companion.android.trainingcompanion.viewmodels

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.companion.android.trainingcompanion.utils.TimerService
import kotlin.math.roundToInt
import android.view.View
import android.widget.ImageView
import com.companion.android.trainingcompanion.R

/**
 * ViewModel для обработки времени в приложении
 */
class TimeViewModel: ViewModel() {
    val generalTime = MutableLiveData<Double>() // Общее время (в секундах)
    val generalTimeIsGoing = MutableLiveData<Boolean>() // Идет ли счет времени

    init {   // Инициализация изначальными значениями
        generalTime.value = 0.0
        generalTimeIsGoing.value = false
    }

    /**
     * Получение времени в String из Double
     */
    private fun getTimeStringFromDouble(time: Double): String {
        val resultInt = time.roundToInt()
        val hours = resultInt % 86400 / 3600
        val minutes = resultInt % 86400 % 3600 / 60
        val seconds = resultInt % 86400 % 3600 % 60
        return String.format("%d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Объект, который будет сохранять полученное значение
     * и отображать его на TextView (clockView)
     */
    val updateTime: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val clockView = (context as Activity)
                .findViewById<View>(R.id.general_clock) as TextView
            generalTime.value = intent.getDoubleExtra(TimerService.TIME_EXTRA, 0.0)
            clockView.text = getTimeStringFromDouble(generalTime.value!!)
        }
    }

    /**
     * Запуск секундомера если он не запущен и,
     * соответственно, остановка, если он запущен
     */
    fun startOrStopTimer(context: Context, serviceIntent: Intent) {
        if (generalTimeIsGoing.value == true) stopTimer(context, serviceIntent)
        else if (generalTimeIsGoing.value == false) startTimer(context, serviceIntent)
    }

    /**
     * Запуск секундомера посредством запуска сервиса
     * с переданным интентом в виде значения текущего времени
     */
    private fun startTimer(context: Context, serviceIntent: Intent) {
        val imageView = (context as Activity).findViewById(R.id.pause_resume_button) as ImageView
        imageView.setImageResource(R.drawable.ic_pause)
        serviceIntent.putExtra(TimerService.TIME_EXTRA, generalTime.value)
        context.startService(serviceIntent)
        generalTimeIsGoing.value = true
    }

    /**
     * Остановка секундомера путем вызова stopService(serviceIntent), после которого будет прекращен
     */
    private fun stopTimer(context: Context, serviceIntent: Intent) {
        val imageView = (context as Activity).findViewById(R.id.pause_resume_button) as ImageView
        imageView.setImageResource(R.drawable.ic_play)
        context.stopService(serviceIntent)
        generalTimeIsGoing.value = false
    }


}