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
import com.companion.android.trainingcompanion.utils.CDTimerService
import com.companion.android.trainingcompanion.viewmodels.CDTimer
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel
import java.util.*

private const val DIALOG_START = "dialog-start" // Метка фрагмента

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
//    private val timerModel: TimerViewModel by lazy {
//        ViewModelProvider(this)[TimerViewModel::class.java]
//    }
    private val timer = CDTimer()



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil // определяем привязку данных
            .inflate(layoutInflater, R.layout.fragment_main, container, false)
        Log.d("FFF", "onCreateView")
        // Инициализация интента для класса сервиса
        serviceIntent = Intent(requireActivity().applicationContext, CDTimerService::class.java)
//        timer = DynamicTimer(viewModel.restTime.value!!.toLong() * 1_000, binding.setTimer)
        // Связывание объекта отправляющего обновленное время и получающего по интенту TIMER_UPDATED
        requireActivity().registerReceiver(timer.updateTime, IntentFilter(CDTimerService.TIMER_UPDATED))
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
    }

    override fun onStart() {
        super.onStart()
        viewModel.workoutSuccessfullyStarted.observe(requireActivity()) {
            if (incorrectCondition()) {
                //TODO
                return@observe
            }
            timer.startOrStopTimer(requireContext(), serviceIntent)
            Log.d("MyTag", "workoutSuccessfullyStarted success")
            binding.startButton.visibility = View.GONE
            binding.setTimer.visibility = View.VISIBLE
//            timer.setTime(viewModel.restTime.value!!.toLong() * 1_000)
//            timer.start()
        }

        viewModel.restTime.observe(requireActivity()) {
            if (!viewModel.activityJustStopped.value!!) {
                timer.setTime(viewModel.restTime.value!!)
                viewModel.activityJustStopped.value = false
                Log.d("MyTag", "restTime")
            }
        }

        //TODO: Удалить эту реализацию
        binding.setTimer.setOnClickListener {
            if (timer.timeIsGoing == true) {
                timer.startOrStopTimer(requireContext(), serviceIntent)
            }
            else {
                it.isEnabled = false
                timer.startOrStopTimer(requireContext(), serviceIntent)
                Handler(requireContext().mainLooper).postDelayed( {
                    it.isEnabled = true
                }, 1000)
            }
        }
        //

        if (viewModel.workoutSuccessfullyStarted.value == true) {
            binding.startButton.visibility = View.GONE
            binding.setTimer.visibility = View.VISIBLE
        }
    }

    private fun incorrectCondition(): Boolean {
        return (!viewModel.workoutSuccessfullyStarted.value!!
                || viewModel.activityJustStopped.value!!)
    }

    override fun onStop() {
        super.onStop()
        viewModel.activityJustStopped.value = true
    }

}