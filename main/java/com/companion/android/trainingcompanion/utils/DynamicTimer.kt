package com.companion.android.trainingcompanion.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar

// Ключи, необходимые для востановления данных
private const val TIME_PREFERENCE = "time-shared-preference"
private const val TIME_IN_MILLIS = "time-millis"
private const val TIMER_IS_FINISHED = "time-is-finished"
private const val TIME_IS_RUNNING = "time-is-running"
private const val OWNER_WAS_DESTROYED = "lc-owner-was-destroyed"

/**
 * Таймер, который независит от жизненного цикла
 */
class DynamicTimer(private var millis: Long, private val timerView: TextView) {

    //--------------------------------------[Данные]----------------------------------------//
    private var preference: SharedPreferences? = null
    private var remainingMillis: Long = millis
    private var isFinished: Boolean = false
    private var lifecycleOwnerWasDestroyed: Boolean = false
    private var isRunning = false
    private var timer: CountDownTimer = object : CountDownTimer(millis, 1000L) {
        override fun onTick(millis: Long) {
            remainingMillis = millis
            timerView.text = String.format("%02d:%02d", millis / 60000, millis % 60000 / 1000)
        }

        override fun onFinish() {
            isFinished = true
            Snackbar.make(timerView, "Timer finished", Snackbar.LENGTH_LONG).show()
            //TODO: начать соответствующее уведомление пользователя, для начала тренировки.
        }
    }

    // Добавление наблюдателей жизненных циклов активити и определение свойства preference;
    init {
        addLifeCycleObserver()
        preference = timerView.context.getSharedPreferences(
            TIME_PREFERENCE, Context.MODE_PRIVATE
        )
    }

    //----------------------[Базовые функции для управления таймером]-------------------------------

    /** Начало таймера со стартового значения */
    fun start() {
        timer.start()
        isRunning = true
    }

    /** Продолжение таймера с предыдущего значения */
    fun `continue`() {
        setTime(remainingMillis)
        timer.start()
        isRunning = true
    }

    /** Сброс времени до изначального значения */
    fun stop() {
        pause()
        setTime(millis)
    }

    /** Установка паузы */
    fun pause() {
        timer.cancel()
        isRunning = false
    }

    //---------------------------------[Геттеры и сеттеры]------------------------------------------

    /** Получение оставшегося времени */
    fun getRemainingTime(): Long = remainingMillis

    /** Дошел ли таймер до конца? */
    fun isFinished(): Boolean = isFinished

    /** Запущен ли таймер в данный момент? */
    fun isRunning(): Boolean = isRunning

    /** Установка нового времени */
    fun setTime(millisToFinish: Long): DynamicTimer {
        timer.cancel()
        millis = millisToFinish
        timer = object : CountDownTimer(millis, 1000L) {
            override fun onTick(millis: Long) {
                remainingMillis = millis
                timerView.text = String.format("%02d:%02d", millis / 60000, millis % 60000 / 1000)
            }

            override fun onFinish() {
                Toast.makeText(timerView.context, "Finished", Toast.LENGTH_LONG).show()
                //TODO: начать соответствующее уведомление пользователя, для начала тренировки.
            }
        }
        return this
    }

    private fun addLifeCycleObserver() {
        val defaultLifecycleObserver = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                restoreData()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                super.onCreate(owner)
                saveData(remainingMillis)
                lifecycleOwnerWasDestroyed = true
            }
        }
        (timerView.context as LifecycleOwner).lifecycle.addObserver(defaultLifecycleObserver)
    }

    private fun saveData(remainingMs: Long) {

        preference?.edit()?.apply {
            putLong(TIME_IN_MILLIS, remainingMs)
            putBoolean(TIMER_IS_FINISHED, isFinished)
            putBoolean(TIME_IS_RUNNING, isRunning)
            putBoolean(OWNER_WAS_DESTROYED, true)
            apply()
        }
    }

    private fun restoreData() {
        lifecycleOwnerWasDestroyed = preference!!.getBoolean(OWNER_WAS_DESTROYED, false)
        if (isRunning) `continue`()
        if (lifecycleOwnerWasDestroyed) {
            preference!!.apply {
                isFinished = getBoolean(TIMER_IS_FINISHED, false)
                isRunning = getBoolean(TIME_IS_RUNNING, true)
                setTime(getLong(TIME_IN_MILLIS, remainingMillis))
                if (isRunning) start()
                edit().clear().apply() //TODO: нужно ли? мб
            }
        }
    }

}