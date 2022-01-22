package com.companion.android.trainingcompanion.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.databinding.FragmentMainBinding
import com.companion.android.trainingcompanion.dialogs.WorkoutStartDialog
import com.companion.android.trainingcompanion.utils.DynamicTimer
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel
import java.util.*

private const val DIALOG_START = "dialog-start" // Метка фрагмента

/**
 * Данный фрагмент сопровождает пользователя во время тренировки и является основным
 */
class MainFragment : Fragment() {
    // ранняя инициализация объекта класса привязки данных
    private lateinit var binding: FragmentMainBinding

    private val viewModel: WorkoutViewModel by activityViewModels()

    private lateinit var timer: DynamicTimer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil // определяем привязку данных
            .inflate(layoutInflater, R.layout.fragment_main, container, false)

        timer = DynamicTimer(viewModel.restTime.value!!.toLong() * 1_000, binding.setTimer)

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
                viewModel.activityJustStopped.value = false
                return@observe
            }


            binding.startButton.visibility = View.GONE
            binding.setTimer.visibility = View.VISIBLE
            timer.setTime(viewModel.restTime.value!!.toLong() * 1_000)
//            if (timer.isRunning())
            timer.start()
        }

        viewModel.restTime.observe(requireActivity()) {
            timer.setTime(viewModel.restTime.value!!.toLong() * 1_000)
        }
        var a = 0
        binding.setTimer.setOnClickListener {
            if (a % 2 == 0)
                timer.pause()
            else {
                if (!timer.isFinished())
                    timer.`continue`()
                else {
                    timer.start()
                }
            }
            a++
        }

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