package com.companion.android.trainingcompanion.startdialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import com.companion.android.trainingcompanion.databinding.FragmentDialogBinding
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import java.util.*


const val SELECT_BODY_PART_DIALOG = "select-body-part-dialog" // Ключ для слушателя получения результата
const val SELECT_MUSCLE_DIALOG = "select-muscle-dialog" // Ключ для слушателя получения результата
const val LIST_BUNDLE_TAG = "list-bundle-tag" // Тег для передачи списка выбранных объектов из диалога

/**
 * Окно для выбора параметров перед тренировкой
 * Будет запускать необходимые диалоговые окна
 */
class WorkoutStartDialog
    : DialogFragment(), View.OnClickListener, AdapterView.OnItemSelectedListener{

    // ViewModel для сохранения необходимых данных, выбранных пользователем
    private val viewModel: WorkoutViewModel by activityViewModels()

    // Инициализация объекта класса привязки данных
    private lateinit var binding: FragmentDialogBinding
    // Инициализация Слушателя Нажатий Для Слайдера (времени)
    private lateinit var sliderTouchListener: Slider.OnSliderTouchListener
    // Инициализация слушателей
    private lateinit var muscleLongTouchListener: View.OnLongClickListener
    private lateinit var onSwitcherCheckedListener: CompoundButton.OnCheckedChangeListener
    private lateinit var timeLabelFormatter: LabelFormatter


    /**
     * Этап создания диалогового окна
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MyTag", "WorkoutStartDialog onCreate")


        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ СЛАЙДЕРА С ВРЕМЕНЕМ ОТДЫХА
        sliderTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Уберем предупреждение, если пользователь пытался начать
                // тренировку без выбора времени тренировки
                removeError(binding.timeSliderDescription)
            }
            override fun onStopTrackingTouch(slider: Slider) {
                // Сохранить выбранное значение
                viewModel.setRestTime(slider.value.toInt())
                binding.timeSliderDescription.text =
                    getString(R.string.description_time_slider,
                        getString(R.string.to_time_slider, viewModel.getRestTime().toString()))
            }
        }
        // ОПРЕДЕЛЕНИЕ ВИДА ОТОБРАЖЕНИЯ ТЕКСТА НА СЛАЙДЕРЕ ВЫБОРА ВРЕМЕНИ
        timeLabelFormatter = LabelFormatter { value ->
            if (value % 60f == 0f)  "${value.toInt() / 60} min"
            else  "${value.toInt()} sec"
        }
        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ View С ВЫБОРОМ МЫШЦ
        muscleLongTouchListener = View.OnLongClickListener {
            if (!viewModel.getMusclesForSelectedBP(requireContext()).isNullOrEmpty()) {
                Toast.makeText(requireContext(),
                    "You selected:\n" +
                            viewModel.getMusclesForSelectedBP(requireContext()).toString().drop(1).dropLast(1),
                    Toast.LENGTH_LONG).show()
                true
            }
            else false
        }
        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ ПЕРЕКЛЮЧАТЕЛЯ ТАЙМЕРА (СО ЗВУКОМ - БЕЗ)
        onSwitcherCheckedListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            // Устанавливаем в viewModel выбранный пользователем режим (со звуком или без)
            viewModel.isTimerOnlyVibrate(isChecked)
            // Обновляем переменную привязки данных
            binding.viewModel = viewModel
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
        Log.d("MyTag", "WorkoutStartDialog onCreateView")
        binding = DataBindingUtil  // определяем привязку данных
            .inflate(layoutInflater, R.layout.fragment_dialog, container, false)
        binding.viewModel = viewModel // Определение viewModel 'и

        // Скругление углов (отображение прозрачного фона за диалоговым окном)
        if (dialog != null && dialog?.window != null) {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
            dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE);
        }

        binding.timeSliderDescription.text = getString(R.string.description_time_slider, "")

        // Переменная из файла разметки для работы с видимостью
        binding.musclesIsVisible = binding.bodyPartSelector.text.isNotEmpty()

        // Устанавливаем спиннер с выбором места тренировок
        setUpTrainingPlaceSpinner()

        binding.apply {
            buttonAccept.setOnClickListener(this@WorkoutStartDialog)     // Слушатель нажатий для кнопки начала тр-ки
            buttonCancel.setOnClickListener(this@WorkoutStartDialog)     // Слушатель нажатий для кнопки отмены тр-ки
            bodyPartSelector.setOnClickListener(this@WorkoutStartDialog) // Слушатель диалога с выбором части тела
            musclesSelector.setOnClickListener(this@WorkoutStartDialog)  // Слушатель диалога с выбором мышц
            musclesSelector.setOnLongClickListener(muscleLongTouchListener)
            timeSlider.addOnSliderTouchListener(sliderTouchListener)
            timeSlider.setLabelFormatter(timeLabelFormatter)
            switchMute.setOnCheckedChangeListener(onSwitcherCheckedListener)
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
        Log.d("MyTag", "WorkoutStartDialog onStart")
        super.onStart()
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        dialog?.window?.setLayout(6 * width / 7, 4 * height / 5)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("1", true)
    }
    /**
     * Определение действий по нажатию на View, находящихся на
     * диалоговом окне с выбором предтренировочных характеристик.
     */
    override fun onClick(v: View?) {
        when(v?.id) {
            /** Кнопка подтверждения -- проверка и сохранение данных*/
            binding.buttonAccept.id -> {
                // Если обязательные поля заполнены:
                if (requiredFieldsCompleted()) {
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
                         || binding.bodyPartSelector.text.isEmpty()) {

                        viewModel.saveSelectedBodyParts(whichBPIsSelected!!.toTypedArray()) // сохранение индексов

                        binding.bodyPartSelector.text =
                            getString(
                                R.string.train_on,
                                viewModel.getSelectedBP(requireContext())
                                    .contentToString().dropLast(1).drop(1)
                            )
                        // очищаем данные мышц, так как пользователь обновил части тела
                        viewModel.resetSelectedMuscles()
                        binding.musclesSelector.text = null
                        // После выбора списка тренеруемых частей тела
                        // станет доступен выбор мышц
                        binding.musclesIsVisible = true
                        // Переприкрепим переключатель к появившемуся View
                        bottomOfSwitcherToTopOfMusclesSelector()
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
                    val numbersOfSelectedItems = bundle.getBooleanArray(LIST_BUNDLE_TAG)
                    viewModel.saveSelectedMuscles(numbersOfSelectedItems!!.toTypedArray())

                    var count: Short = 0
                    numbersOfSelectedItems.forEach { if (it) count++ }

                    if (viewModel.someMuscleSelected()) {
                        binding.musclesSelector.text = getString(
                            R.string.number_of_selected_el,
                            count
                        )
                    }
                    else binding.musclesSelector.text = null
                }
                if (viewModel.getAllBP(requireContext()).isEmpty())
                    throw Exception("allBodyParts is empty now")
                if (viewModel.getWhichBPsAreSelected().isNullOrEmpty())
                    throw Exception("viewModel.boolBodyPartSelected.value is null or empty now")
                // Запуск диалогового окна с выбором мышц
                MultiChoiceDialog(
                    viewModel.getMusclesForSelectedBP(requireContext()),
                    viewModel.getWhichMusclesAreSelected().toBooleanArray()
                ).show(parentFragmentManager, SELECT_MUSCLE_DIALOG)
            }
        }
    }

    /**
     * Метод для создания спиннера, с загруженными данными из ресурсов;
     * Данный массив из ресурсов содержит места для тренировок
     */
    private fun setUpTrainingPlaceSpinner() {
        // Создаем массив из ресурса с разметкой simple_spinner_item
        ArrayAdapter.createFromResource(
            requireActivity(),
            R.array.training_place,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Устанавливаем разметку для появления списка спиннера
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Применяем данный адаптер к нашему спиннеру
            binding.placeSpinner.adapter = adapter
        }
        // Устанавливаем слушатель для нажатий
        binding.placeSpinner.onItemSelectedListener = this

    }

    /** ИНТЕРФЕЙС (spinner)
     * Определеяем действия по выбору объекта из списка spinner
     */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        viewModel.setTrainingPlace(
            when (pos) {
                0 -> WorkoutViewModel.TRAINING_AT_HOME
                1 -> WorkoutViewModel.TRAINING_IN_GYM
                else -> WorkoutViewModel.TRAINING_OUTDOORS
            }
        )
    }
    /** ИНТЕРФЕЙС (spinner)
     * Определеляем действия по отстутствию выбора какого-либо объекта в spinner
     */
    override fun onNothingSelected(parent: AdapterView<*>) {}
    /**
     * При появлении View с выбором мышц, переключатель switchMute
     * присоединиться к нему.
     */
    private fun bottomOfSwitcherToTopOfMusclesSelector() {
        binding.switchMute.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topToBottom = if (binding.musclesIsVisible!!)
                binding.musclesSelector.id
            else
                binding.bodyPartSelector.id
        }
    }
    /**
     * Метод предназначен для проверки на заполненность обязательных полей
     * В случае удачи (все нужные поля заполнены) вернется true
     * В случае если не все обязательные поля заполнены, вернется false
     */
    private fun requiredFieldsCompleted(): Boolean {
        return !(viewModel.getRestTime() == null || viewModel.getSelectedBP(requireContext()).isEmpty())
    }
    /**
     * Метод вызовет error у не заполненных объектов TextView
     */
    private fun requireAllFieldsCompleted() {
        if (viewModel.getRestTime() == null)
                binding.timeSliderDescription.error = getString(R.string.error_time_slider)
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
        if (viewModel.getWhichBPsAreSelected().contains(true)) {
            var resultString: String = ""
            viewModel.getWhichBPsAreSelected().forEachIndexed { index, value ->
                // Конкатенируем строки с пробелами
                if (value)
                    resultString += (viewModel.getAllBP(requireContext())[index] + ",")
            }
            binding.bodyPartSelector.text = resultString.dropLast(1)
            resultString = ""
        }
        // Если список с мышцами не пуст, то восстановим
        if (viewModel.getWhichMusclesAreSelected().contains(true) && viewModel.getWhichBPsAreSelected().contains(true)) {
            val arr = arrayListOf<Int>()
            viewModel.getWhichBPsAreSelected().forEachIndexed { index, value ->
                if (value) {
                    arr.add(index)
                }
            }

            arr.clear() // Удаляем уже не нужный массив
            if (viewModel.getMusclesForSelectedBP(requireContext()).isNotEmpty())
                binding.musclesSelector.text = getString(
                R.string.number_of_selected_el,
                viewModel.getMusclesForSelectedBP(requireContext()).size
            )
            else
                binding.musclesSelector.text = null
            bottomOfSwitcherToTopOfMusclesSelector()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("MyTag", "WorkoutStartDialog onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MyTag", "WorkoutStartDialog onDestroy")

    }
}
