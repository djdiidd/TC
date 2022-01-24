package com.companion.android.trainingcompanion.viewmodels

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.utils.StopwatchService

/**
 * ViewModel для обработки времени в приложении
 */
class StopwatchViewModel : ViewModel() {
    private var generalTime = 0 // Общее время (в секундах)
    val generalTimeIsGoing = MutableLiveData(false) // Идет ли счет времени

    /**
     * Получение времени в String из Int
     */
    private fun getTimeStringFromInt(time: Int): String {
        time.also {
            return String.format(
                "%d:%02d:%02d",
                it % 86400 / 3600,
                it % 86400 % 3600 / 60,
                it % 86400 % 3600 % 60
            )
        }
    }

    /**
     * Объект, который будет сохранять полученное значение
     * и отображать его на TextView (clockView)
     */
    val updateTime: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("MyTag", "SW: context-${context.toString().takeLast(5)}")
            (context as Activity).findViewById<TextView>(R.id.general_clock).apply {
                text = getTimeStringFromInt(generalTime)
            }
            generalTime = intent.getIntExtra(StopwatchService.TIME_EXTRA, 0)
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
        serviceIntent.putExtra(StopwatchService.TIME_EXTRA, generalTime)
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