package com.companion.android.trainingcompanion.startdialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.databinding.DialogRestTimeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider

class ChangeRestTimeDialog(private val currentRestTime: Int)
    : DialogFragment(), DialogInterface.OnClickListener {

    private lateinit var binding: DialogRestTimeBinding

    private var callback: Callback? = null

    private lateinit var sliderTouchListener: Slider.OnSliderTouchListener
    private lateinit var timeLabelFormatter: LabelFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MyTag", "onCreate of dialog")
        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ СЛАЙДЕРА С ВРЕМЕНЕМ ОТДЫХА
        sliderTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                binding.selectedTime.text = requireContext()
                    .getString(R.string.selected_rest_time, slider.value.toInt())
            }
        }
        // ОПРЕДЕЛЕНИЕ ВИДА ОТОБРАЖЕНИЯ ТЕКСТА НА СЛАЙДЕРЕ ВЫБОРА ВРЕМЕНИ
        timeLabelFormatter = LabelFormatter { value ->
            if (value % 60f == 0f)  "${value.toInt() / 60} min"
            else  "${value.toInt()} sec"
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d("MyTag", "onCreateDialog of dialog")
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.dialog_rest_time, null, false)

        binding.slider.addOnSliderTouchListener(sliderTouchListener)
        binding.slider.setLabelFormatter(timeLabelFormatter)

        binding.selectedTime.text = requireContext()
            .getString(R.string.selected_rest_time, currentRestTime)

        binding.slider.value = currentRestTime.toFloat()

        val adb: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_change_rest_time)
            .setPositiveButton(R.string.button_dialog_accept, this)
            .setView(binding.root)
            .setNegativeButton(R.string.button_dialog_cancel, this)
        return adb.create()
    }

    override fun onStart() {
        super.onStart()
        binding.add5s.setOnClickListener {
            binding.slider.value += 5f
            binding.selectedTime.text =
                requireContext().getString(R.string.selected_rest_time, binding.slider.value.toInt())
        }
        binding.sub5s.setOnClickListener {
            binding.slider.value -= 5f
            binding.selectedTime.text =
                requireContext().getString(R.string.selected_rest_time, binding.slider.value.toInt())
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as Callback
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onClick(p0: DialogInterface?, which: Int) {
        when (which) {
            Dialog.BUTTON_POSITIVE -> {
                callback?.restTimeSelected(binding.slider.value.toInt())
                dismiss()
            }
            Dialog.BUTTON_NEGATIVE -> dismiss()
        }
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }
    interface Callback {
        fun restTimeSelected(time: Int)
    }
}