package com.companion.android.trainingcompanion.fragments

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.databinding.FragmentMainBinding
import com.companion.android.trainingcompanion.dialogs.WorkoutStartDialog
import com.companion.android.trainingcompanion.utils.CountDownService
import com.companion.android.trainingcompanion.utils.CountDownTimer
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel
import java.util.*

private const val DIALOG_START = "dialog-start" // Метка фрагмента
private const val TIMER_IS_GOING = "timer-data"
private const val TIMER_REMAINING_TIME = "timer-current-time"

/**
 * Данный фрагмент сопровождает пользователя во время тренировки и является основным
 */
class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding               // Объект класса привязки данных
    private val viewModel: WorkoutViewModel by activityViewModels() // Общая ViewModel
//    private lateinit var timer: DynamicTimer                        // Таймер

    // Инициализация объекта Intent для общего времени
    private lateinit var serviceIntent: Intent
    // ViewModel для инкапсуляции некоторых данных времени
    private lateinit var timer: CountDownTimer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil // определяем привязку данных
            .inflate(layoutInflater, R.layout.fragment_main, container, false)
        Log.d("FFF", "onCreateView")
        binding.setTimer.visibility = View.GONE

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Слушатель нажатий для кнопки начала тренировки
        binding.startButton.setOnClickListener {
            WorkoutStartDialog().apply {
                show(this@MainFragment.parentFragmentManager, DIALOG_START)
            }
        }

        // Инициализация интента для класса сервиса
        serviceIntent = Intent(requireActivity().applicationContext, CountDownService::class.java)
        timer = CountDownTimer(requireContext(), serviceIntent)
        // Связывание объекта отправляющего обновленное время и получающего по интенту TIMER_UPDATED
        requireContext().registerReceiver(timer.timeReceiver, IntentFilter(CountDownService.TIMER_UPDATED))

        savedInstanceState?.also { bundle->
            timer.setTime(bundle.getInt(TIMER_REMAINING_TIME))
            binding.setTimer.text = timer.getTimeInFormatMMSS(timer.getRemaining())
            timer.setGoing(bundle.getBoolean(TIMER_IS_GOING))
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.workoutSuccessfullyStarted.observe(requireActivity()) { started->
            if (!started || viewModel.settingsAlreadyApplied) { return@observe }
            timer.startOrStop()
            Log.d("MyTag", "workoutSuccessfullyStarted success")
            binding.startButton.visibility = View.GONE
            binding.setTimer.visibility = View.VISIBLE
//            timer.setTime(viewModel.restTime.value!!.toLong() * 1_000)
//            timer.start()
        }

        viewModel.restTime.observe(requireActivity()) {
            if (!viewModel.settingsAlreadyApplied) {
                timer.setTime(viewModel.restTime.value!! + 1) // На 1 больше, чтобы отсчет начинался с нужного числа
                Log.d("MyTag", "restTime")
            }
        }

        //TODO: Удалить эту реализацию
        binding.setTimer.setOnClickListener {
            it.isEnabled = false
            timer.startOrStop()
            Handler(requireContext().mainLooper).postDelayed( {
                it.isEnabled = true
            }, 1000)

        }
        //

        if (viewModel.workoutSuccessfullyStarted.value == true) {
            binding.startButton.visibility = View.GONE
            binding.setTimer.visibility = View.VISIBLE
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("FFF", "stopped from wf")
        viewModel.settingsAlreadyApplied = true
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(timer.timeReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putBoolean(TIMER_IS_GOING, timer.isGoing())
            putInt(TIMER_REMAINING_TIME, timer.getRemaining())
        }
        super.onSaveInstanceState(outState)
    }
}