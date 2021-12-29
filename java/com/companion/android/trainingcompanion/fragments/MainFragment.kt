package com.companion.android.trainingcompanion.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment.STYLE_NORMAL
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.startdialog.WorkoutStartDialog
import com.companion.android.trainingcompanion.databinding.FragmentMainBinding
import com.companion.android.trainingcompanion.viewmodels.StartViewModel

private const val DIALOG_START = "dialog-start" // Константа для метки фрагмента

/**
 * Данный фрагмент сопровождает пользователя во время тренировки и является основным
 */
class MainFragment: Fragment() {
    // ранняя инициализация объекта класса привязки данных
    private lateinit var binding: FragmentMainBinding

    private val viewModel: StartViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil // определяем привязку данных
            .inflate(layoutInflater, R.layout.fragment_main, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Слушатель нажатий для кнопки начала тренировки
        binding.startButton.setOnClickListener {
            it.isClickable = false
            it.postDelayed( Runnable {
                it.visibility = View.GONE
            }, 150)

            WorkoutStartDialog().apply {
                show(this@MainFragment.parentFragmentManager, DIALOG_START)
            }
        }
    }
}