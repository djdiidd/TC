package com.companion.android.trainingcompanion.viewmodels

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.utils.CDTimerService

class CDTimer {

    private var time = 60 // Общее время (в секундах)
    var timeIsGoing = false // Идет ли счет времени

    /**
     * Получение времени в String из Int
     */
    private fun getTimeStringFromInt(time: Int): String {
        time.also {
            return String.format(
                "%02d:%02d",
                it % 86400 % 3600 / 60,
                it % 86400 % 3600 % 60
            )
        }
    }

    fun setTime(newTime: Int) {
        time = newTime
    }

    /**
     * Объект, который будет сохранять полученное значение
     * и отображать его на TextView (clockView)
     */
    val updateTime: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("MyTag", "T: context-${context.toString().takeLast(5)}")
            (context as Activity).findViewById<TextView>(R.id.set_timer).apply {
                if (this != null) text = getTimeStringFromInt(time)
            }

            time = intent.getIntExtra(CDTimerService.TIME_EXTRA, 0)
        }
    }

    /**
     * Запуск секундомера если он не запущен и,
     * соответственно, остановка, если он запущен
     */
    fun startOrStopTimer(context: Context, serviceIntent: Intent) {
        if (timeIsGoing) stopTimer(context, serviceIntent)
        else if (!timeIsGoing) startTimer(context, serviceIntent)
    }

    /**
     * Запуск секундомера посредством запуска сервиса
     * с переданным интентом в виде значения текущего времени
     */
    private fun startTimer(context: Context, serviceIntent: Intent) {
//        val imageView = (context as Activity).findViewById(R.id.pause_resume_button) as ImageView
//        imageView.setImageResource(R.drawable.ic_pause)

        if (time <= 0) {
            Log.d("MyTag", "Start dismissed")
            return
        }
        serviceIntent.putExtra(CDTimerService.TIME_EXTRA, time)
        context.startService(serviceIntent)
        timeIsGoing = true
    }

    /**
     * Остановка секундомера путем вызова stopService(serviceIntent), после которого будет прекращен
     */
    private fun stopTimer(context: Context, serviceIntent: Intent) {
//        val imageView = (context as Activity).findViewById(R.id.pause_resume_button) as ImageView
//        imageView.setImageResource(R.drawable.ic_play)
        context.stopService(serviceIntent)
        timeIsGoing = false
    }

}