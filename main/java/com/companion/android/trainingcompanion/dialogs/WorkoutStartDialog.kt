package com.companion.android.trainingcompanion.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.adapters.PlaceSpinnerAdapter
import com.companion.android.trainingcompanion.databinding.FragmentWorkoutStartDialogBinding
import com.companion.android.trainingcompanion.objects.BreakNotificationMode
import com.companion.android.trainingcompanion.objects.Place
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import java.util.*

// Ключ для слушателя получения результата
const val SELECT_BODY_PART_DIALOG = "select-body-part-dialog"

// Ключ для слушателя получения результата
const val SELECT_MUSCLE_DIALOG = "select-muscle-dialog"

// Тег для передачи списка выбранных объектов из диалога
const val LIST_BUNDLE_TAG = "list-bundle-tag"

/**
 * Окно для выбора параметров перед тренировкой
 * Будет запускать необходимые диалоговые окна
 */
class WorkoutStartDialog
    : DialogFragment(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    // ViewModel для сохранения необходимых данных, выбранных пользователем
    private val viewModel: WorkoutViewModel by activityViewModels()

    // Инициализация объекта класса привязки данных
    private lateinit var binding: FragmentWorkoutStartDialogBinding

    // Инициализация Слушателя Нажатий Для Слайдера (времени)
    private lateinit var sliderTouchListener: Slider.OnSliderTouchListener

    // Инициализация слушателей
    private lateinit var muscleLongTouchListener: View.OnLongClickListener
    private lateinit var timeLabelFormatter: LabelFormatter


    /**
     * Этап создания диалогового окна
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ СЛАЙДЕРА С ВРЕМЕНЕМ ОТДЫХА
        sliderTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Уберем предупреждение, если пользователь пытался начать
                // тренировку без выбора времени тренировки
                removeError(binding.timeSliderDescription)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // Сохранить выбранное значение
                viewModel.restTime.value = slider.value.toInt()
                binding.timeSliderValue.text =
                    getString(R.string.selected_seconds, viewModel.restTime.value!!)
            }
        }
        // ОПРЕДЕЛЕНИЕ ВИДА ОТОБРАЖЕНИЯ ТЕКСТА НА СЛАЙДЕРЕ ВЫБОРА ВРЕМЕНИ
        timeLabelFormatter = LabelFormatter { value ->
            if (value % 60f == 0f) "${value.toInt() / 60} min"
            else "${value.toInt()} sec"
        }
        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ View С ВЫБОРОМ МЫШЦ
        muscleLongTouchListener = View.OnLongClickListener {
            if (viewModel.getWhichMusclesAreSelected().contains(true)) {
                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.toast_amount_of_selected_mscls,
                        viewModel.getSelectedMuscles(requireContext())
                            .contentToString()
                            .drop(1)
                            .dropLast(1)),
                    Toast.LENGTH_LONG
                ).show()
                true
            } else false
        }
    }

    /**
     * Этап создания View во фрагменте
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil  // определяем привязку данных
            .inflate(layoutInflater, R.layout.fragment_workout_start_dialog, container, false)
        binding.viewModel = viewModel // Определение viewModel 'и

        // Скругление углов (отображение прозрачного фона за диалоговым окном)
        if (dialog != null && dialog?.window != null) {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
        binding.timeSliderDescription.text = getString(R.string.description_time_slider)

        // Переменная из файла разметки для работы с видимостью
        binding.musclesIsVisible = binding.bodyPartSelector.text.isNotEmpty()

        // Устанавливаем спиннер с выбором места тренировок
        setUpSpinners()

        binding.apply {
            buttonAccept.setOnClickListener(this@WorkoutStartDialog)     // Слушатель нажатий для кнопки начала тр-ки
            buttonCancel.setOnClickListener(this@WorkoutStartDialog)     // Слушатель нажатий для кнопки отмены тр-ки
            bodyPartSelector.setOnClickListener(this@WorkoutStartDialog) // Слушатель диалога с выбором части тела
            musclesSelector.setOnClickListener(this@WorkoutStartDialog)  // Слушатель диалога с выбором мышц
            musclesSelector.setOnLongClickListener(muscleLongTouchListener)
            timeSlider.addOnSliderTouchListener(sliderTouchListener)
            timeSlider.setLabelFormatter(timeLabelFormatter)
        }
        // Если конфигурация менялась, то восстанавливаем данные
        if (savedInstanceState != null) {
            recoverData()
        }
        return binding.root
    }

    /**
     * Метод onStart жизненного цикла фрагмента
     */
    override fun onStart() {
        super.onStart()

        setDialogSize()

        setWindowAnimation(R.style.SlideLeftToRightDialogAnimation)

//        binding.timeSliderValue.setOnClickListener {
//            // Создать анимацию для thumb слайдера
//        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("1", true)
    }

    /* Интерфейс */
    /**
     * Определение действий по нажатию на View, находящихся на
     * диалоговом окне с выбором предтренировочных характеристик.
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            /** Кнопка подтверждения -- проверка и сохранение данных*/
            binding.buttonAccept.id -> {
                // Если обязательные поля заполнены:
                if (requiredFieldsCompleted()) {
                    if (viewModel.getNumberOfSelectedMuscles() != 0.toShort()) {
                        val unused = viewModel.getWhichBPAreUnused()
                        if (unused.contains(true)) {
                            WarningUnusedBPDialog(unused).show(parentFragmentManager, "")
                        }
                    }
                    viewModel.workoutSuccessfullyStarted.value = true
                    // Сохранить все значения в бд
//------------------НЕОБХОДИМО ПРОВЕРИТЬ НА НАЧАЛО ТРЕНИРОВКИ (после кнопки accept) ИНАЧЕ ПОЯВИТСЯ ВЫБОР МЫШЦ
                    dismiss()
                }
                // Если не все обязательные поля заполнены
                else {
                    requireAllFieldsCompleted()
                }
            }
            /** Кнопка отмены -- закрытие окна и удаление данных*/
            binding.buttonCancel.id -> {
                viewModel.clearAllData()
                dismiss()
            }
            /** Выбор тренеруемых частей тела -- вызов соответствующего окна и обработка данных */
            binding.bodyPartSelector.id -> {
                // Устанавливаем слушатель для получения списка выбранных частей тела
                // По ключу SELECT_BODY_PART_DIALOG_TAG
                setFragmentResultListener(SELECT_BODY_PART_DIALOG) { _, bundle ->
                    // Уберем предупреждение, если пользователь пытался начать
                    // тренировку без выбора тренируемой части тела
                    removeError(binding.bodyPartSelector)
                    // В переменную numbersOfSelectedBodyParts записываем arrayList
                    // полученный из объекта Bundle по ключу BODY_PART_LIST_KEY
                    val whichBPIsSelected = bundle.getBooleanArray(LIST_BUNDLE_TAG)

                    // Если полученный список не изменился, то перезаписывать данные не будем
                    if (!viewModel.getWhichBPsAreSelected().toBooleanArray()
                            .contentEquals(whichBPIsSelected)
                        || binding.bodyPartSelector.text.isEmpty()
                    ) {
                        viewModel.updateData(requireContext(), whichBPIsSelected!!.toTypedArray())
                        binding.bodyPartSelector.text =
                            getString(
                                R.string.train_on,
                                viewModel.getSelectedBP(requireContext())
                                    .contentToString().dropLast(1).drop(1)
                            )
                        var count: Short = 0
                        viewModel.getWhichMusclesAreSelected().forEach { if (it) count++ }

                        if (viewModel.isSomeMuscleSelected())
                            binding.musclesSelector.text = getString(
                                R.string.number_of_selected_el,
                                count
                            )
                        else binding.musclesSelector.text = null
                        // После выбора списка тренеруемых частей тела
                        // станет доступен выбор мышц
                        binding.musclesIsVisible = true
                    }
                }
                // Запуск диалогового окна с выбором частей тела
                MultiChoiceDialog(
                    viewModel.getAllBP(requireContext()),
                    viewModel.getWhichBPsAreSelected().toBooleanArray()
                ).show(parentFragmentManager, SELECT_BODY_PART_DIALOG)
            }
            /** Выбор мышц для выбранных частей тела -- вызов соответствующего окна и обработка */
            binding.musclesSelector.id -> {
                // Устанавливаем слушатель для получения списка мышц по ключу SELECT_MUSCLE_DIALOG.
                // Список мышц определяется в зависимости от выбранных частей тела
                setFragmentResultListener(SELECT_MUSCLE_DIALOG) { _, bundle ->
                    val whichMuscleIsSelected = bundle.getBooleanArray(LIST_BUNDLE_TAG)
                    viewModel.saveSelectedMuscles(whichMuscleIsSelected!!.toTypedArray())

                    var count: Short = 0
                    whichMuscleIsSelected.forEach { if (it) count++ }

                    if (viewModel.isSomeMuscleSelected())
                        binding.musclesSelector.text = getString(
                            R.string.number_of_selected_el,
                            count
                        )
                    else binding.musclesSelector.text = null
                }
                if (viewModel.getAllBP(requireContext()).isEmpty())
                    throw Exception("allBodyParts is empty now")
                if (viewModel.getWhichBPsAreSelected().isNullOrEmpty())
                    throw Exception("viewModel.boolBodyPartSelected.value is null or empty now")
                // Запуск диалогового окна с выбором мышц
                MultiChoiceDialog(
                    viewModel.getAvailableMuscles(requireContext()),
                    viewModel.getWhichMusclesAreSelected().toBooleanArray()
                ).show(parentFragmentManager, SELECT_MUSCLE_DIALOG)
            }
        }
    }

    private fun setDialogSize() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        dialog?.window?.setLayout(6 * width / 7, 4 * height / 5)
    }

    private fun setWindowAnimation(res: Int) {
        dialog?.window?.setWindowAnimations(res)
    }

    /**
     * Метод для создания спиннера, с загруженными данными из ресурсов;
     * Данный массив из ресурсов содержит места для тренировок
     */
    private fun setUpSpinners() {
        val placeAdapter = PlaceSpinnerAdapter(requireContext(), Place.getList(requireContext()))
        val notificationAdapter =
            PlaceSpinnerAdapter(requireContext(), BreakNotificationMode.getList(requireContext()))
        binding.placeSpinner.adapter = placeAdapter
        binding.placeSpinner.onItemSelectedListener = this
        binding.notificationSpinner.adapter = notificationAdapter
        binding.notificationSpinner.onItemSelectedListener = this
    }

    /** ИНТЕРФЕЙС (spinner)
     * Определеяем действия по выбору объекта из списка spinner
     */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        viewModel.setTrainingPlace(
            when (pos) {
                0 -> Place.TRAINING_AT_HOME
                1 -> Place.TRAINING_IN_GYM
                else -> Place.TRAINING_OUTDOORS
            }
        )
    }

    /** ИНТЕРФЕЙС (spinner)
     * Определеляем действия по отстутствию выбора какого-либо объекта в spinner
     */
    override fun onNothingSelected(parent: AdapterView<*>) {}

    /**
     * Метод предназначен для проверки на заполненность обязательных полей
     * В случае удачи (все нужные поля заполнены) вернется true
     * В случае если не все обязательные поля заполнены, вернется false
     */
    private fun requiredFieldsCompleted(): Boolean {
        return viewModel.getSelectedBP(requireContext()).isNotEmpty()
    }

    /**
     * Метод вызовет error у не заполненных объектов TextView
     */
    private fun requireAllFieldsCompleted() {
        if (viewModel.getSelectedBP(requireContext()).isEmpty())
            binding.bodyPartSelector.error = getString(R.string.error_body_parts)
    }

    /**
     * Если пользователь нажав кнопку подтверждения не заполнил все обязательные поля,
     * то на них появится предупреждение (error). Данный метод позволяет отменить предупреждение.
     */
    private fun removeError(textView: TextView) {
        if (textView.error != null) textView.error = null
    }

    /**
     * После изменения конфигурации данный метод позволит восстановить информацию на всех View
     */
    private fun recoverData() {
        // Если список с частями тела не пуст, то восстановим
        if (viewModel.isSomeBPSelected()) {
            binding.bodyPartSelector.text =
                getString(
                    R.string.train_on,
                    viewModel.getSelectedBP(requireContext())
                        .contentToString().dropLast(1).drop(1)
                )
            // Если список с мышцами не пуст, то восстановим
            if (viewModel.isSomeMuscleSelected()) {
                binding.musclesSelector.text =
                    getString(
                        R.string.number_of_selected_el,
                        viewModel.getNumberOfSelectedMuscles()
                    )
                binding.musclesIsVisible = true
            }
        }
        binding.timeSliderValue.text =
            getString(R.string.selected_seconds, viewModel.restTime.value!!)

    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
