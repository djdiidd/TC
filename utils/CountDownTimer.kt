package com.companion.android.trainingcompanion.utils

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import java.io.Serializable
import java.util.*

/**
 * Сервис таймер, использующий broadCast
 */
class CountDownTimer(
    private val context: Context,
    private val serviceIntent: Intent
) : Serializable {


    val ID = UUID.randomUUID()


    var startTime: Int = 0
    var isFinished = false    // Закончился ли таймер;
        private set(value) { field = value}
    private var clockTextView: TextView? = null
    private var clockProgressBar: ProgressBar? = null
    private var callback: Callback = context as Callback

    private lateinit var anim: ObjectAnimator
    private var time: Int = 60       // Общее время (в секундах);

    var isGoing = false // Идет ли счет времени;

    private var animCoefficient: Int = 10
        get() {
            if (startTime == 0) throw Error("Start time is 0")
            return ((270 - startTime) * 0.2).toInt()
        }
        set(value) {
            if (value > 0)
                field = value
            else throw Error("Coefficient of animation cannot be <= 0")
        }

    fun getTime() = time
    fun setTime(time: Int) {
        Log.d("MyTag", "!!! time changed from ${this.time} to $time")
        this.time = time
        if (startTime == 0 || !isGoing) {
            if (startTime == 0)
                Log.d("MyTag", "start time changed because start time is 0")
            else
                Log.d("MyTag", "start time changed because time is not going")
            startTime = time
        }
    }

    /**
     * Получение времени в формате "ММ:СС"
     */
    fun getTimeInFormatMMSS(time: Int = this.time): String {
        time.also {
            return String.format(
                "%02d:%02d",
                it % 86400 % 3600 / 60,
                it % 86400 % 3600 % 60
            )
        }
    }


    private fun restoreProgressBar(maxValue: Int = startTime, currentValue: Int = time) {
        Log.d("MyTag", "Passed in restoreProgressBar startValue==$startTime, time==$time" +
                "\n animCoefficient is set to $animCoefficient")
        if (clockProgressBar == null) {
            Log.d("MyTag", "BAD SITUATION: clockProgressBar is not initialized")
            return
        }
        clockProgressBar!!.max = (maxValue * animCoefficient)
        val currentProgress = if (currentValue != maxValue) {
            Log.d("MyTag", "currentValue is not max: currentValue = $currentValue; max = $maxValue")
            currentValue * animCoefficient
        } else {
            Log.d("MyTag", "currentValue is max: currentValue = $currentValue; max = $maxValue")
            maxValue * animCoefficient
        }
        anim = ObjectAnimator.ofInt(
            clockProgressBar!!, "progress", currentProgress, 0
        ).apply {
            duration = (currentProgress.toLong() * 2000.0/animCoefficient.toDouble()).toLong()
            interpolator = null
            if (isGoing) {
                start()
            } else {
                clockProgressBar!!.progress = currentProgress
            }
            Log.d("MyTag", "Animation started on progress -> $currentProgress and progressView : ${clockProgressBar?.progress}")
        }
    }

    /**
     * Объект, который будет сохранять полученное
     * значение и отображать его на экране
     */
    val timeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            time = intent.getIntExtra(CountDownService.TIME_EXTRA, 0)
            Log.d("MyTag", "time changed to $time")
            updateUI()
            if (time == 0) {
                isFinished = true
                callback.timerFinished()
                startTime = 0
            }
        }
    }

    /**
     * Запуск секундомера если он не запущен и,
     * соответственно, остановка, если он запущен
     */
    fun startOrStop() {
        if (isGoing) stop()
        else start()
    }

    /**
     * Запуск секундомера посредством запуска сервиса
     * с переданным интентом в виде значения текущего времени
     */
    private fun start() {
        Log.d("MyTag", "starting timer")
        if (time <= 0) {
            return
        } else {
            isFinished = false
        }
        if (startTime == 0)
            throw Error("startTime is 0.\nPossible problem: Time was not selected automatically")
        serviceIntent.putExtra(CountDownService.TIME_EXTRA, time)
        context.startService(serviceIntent)
        isGoing = true
        restoreProgressBar()
    }

    /**
     * Остановка секундомера путем вызова stopService(serviceIntent), после которого будет прекращен
     */
    private fun stop() {
        anim.pause()
        Log.d("MyTag", "When stopped timer startTime == $startTime")
        context.stopService(serviceIntent)
        isGoing = false
    }

    private fun updateUI() {
        if (clockTextView == null) Log.d("MyTag", "clockTextView is null now")
        else clockTextView!!.text = getTimeInFormatMMSS(time)
    }

    fun attachUI(textView: TextView, progressBar: ProgressBar) {
        clockTextView = textView
        textView.text = getTimeInFormatMMSS()
        clockProgressBar = progressBar
        restoreProgressBar()
    }
    fun detachUI() {
        clockProgressBar = null
        clockTextView = null
    }

    interface Callback {
        fun timerFinished()
    }
}