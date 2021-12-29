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
import com.companion.android.trainingcompanion.viewmodels.StartViewModel
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import java.util.*
import kotlin.collections.ArrayList


const val SELECT_BODY_PART_DIALOG = "select-body-part-dialog" // Ключ для слушателя получения результата
const val SELECT_MUSCLE_DIALOG = "select-muscle-dialog" // Ключ для слушателя получения результата
const val LIST_BUNDLE_TAG = "list-bundle-tag" // Тег для передачи списка выбранных объектов из диалога

/**
 * Окно для выбора параметров перед тренировкой
 * Будет запускать необходимые диалоговые окна
 */
class WorkoutStartDialog
    : DialogFragment(), View.OnClickListener, AdapterView.OnItemSelectedListener{
//    private val viewModel: StartViewModel by lazy {
//        ViewModelProvider(this).get(StartViewModel::class.java)
//    }
    // ViewModel для сохранения необходимых данных, выбранных пользователем
    private val viewModel: StartViewModel by activityViewModels()

    // Инициализация объекта класса привязки данных
    private lateinit var binding: FragmentDialogBinding
    // Инициализация Слушателя Нажатий Для Слайдера (времени)
    private lateinit var sliderTouchListener: Slider.OnSliderTouchListener
    // Инициализация массивов (для выбора мышц и частей тела)
    private lateinit var allBodyParts: Array<String> // массив для выбора частей тела
    private lateinit var musclesToBodyParts: Array<String> // массив для выбора мышц
    // Инициализация слушателей
    private lateinit var muscleLongTouchListener: View.OnLongClickListener
    private lateinit var onSwitcherCheckedListener: CompoundButton.OnCheckedChangeListener
    private lateinit var timeLabelFormatter: LabelFormatter
    // Определение массива, который будет хранить список выбранных частей тела
    private var numbersOfSelectedItems: ArrayList<Int>? = null

    /**
     * Этап создания диалогового окна
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MyTag", "WorkoutStartDialog onCreate")

        defineBodyPartsArrays()


        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ СЛАЙДЕРА С ВРЕМЕНЕМ ОТДЫХА
        sliderTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Уберем предупреждение, если пользователь пытался начать
                // тренировку без выбора времени тренировки
                removeError(binding.timeSliderDescription)
            }
            override fun onStopTrackingTouch(slider: Slider) {
                // Сохранить выбранное значение
                viewModel.restTime = slider.value.toInt()
                binding.timeSliderDescription.text =
                    getString(R.string.description_time_slider,
                        getString(R.string.to_time_slider, viewModel.restTime.toString()))
            }
        }
        // ОПРЕДЕЛЕНИЕ ВИДА ОТОБРАЖЕНИЯ ТЕКСТА НА СЛАЙДЕРЕ ВЫБОРА ВРЕМЕНИ
        timeLabelFormatter = LabelFormatter { value ->
            if (value % 60f == 0f)  "${value.toInt() / 60} min"
            else  "${value.toInt()} sec"
        }
        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ View С ВЫБОРОМ МЫШЦ
        muscleLongTouchListener = View.OnLongClickListener {
            if (!viewModel.selectedMuscles.value.isNullOrEmpty()) {
                Toast.makeText(requireContext(),
                    "You selected:\n" +
                            viewModel.selectedMuscles.value.toString().drop(1).dropLast(1),
                    Toast.LENGTH_LONG).show()
                true
            }
            else false
        }
        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ ПЕРЕКЛЮЧАТЕЛЯ ТАЙМЕРА (СО ЗВУКОМ - БЕЗ)
        onSwitcherCheckedListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            // Устанавливаем в viewModel выбранный пользователем режим (со звуком или без)
            viewModel.timerOnlyVibrate = isChecked
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

        // Переменная из файла разметки для работы с Visible и Gone
        binding.musclesIsVisible = !viewModel.boolMusclesSelected.value.isNullOrEmpty()
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

        if (allBodyParts.isNullOrEmpty()) {
            defineBodyPartsArrays()
        }
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
                    numbersOfSelectedItems = bundle.getIntegerArrayList(LIST_BUNDLE_TAG)
                    val numbersOfBodyParts = numbersOfBodyPartsToList(numbersOfSelectedItems!!)
                    // Если полученный список не изменился, то перезаписывать данные не будем
                    if (viewModel.selectedBodyParts.value  !=  numbersOfBodyParts
                         || binding.bodyPartSelector.text  ==  "") {
                        binding.bodyPartSelector.text =
                            getString(
                                R.string.train_on,
                                numbersOfPartsToString(numbersOfSelectedItems!!)
                            )
                        defineMuscleArrays(numbersOfSelectedItems!!)

                        viewModel.selectedBodyParts.value = numbersOfBodyParts // сохранение списка
                        viewModel.saveSelectedBodyParts(numbersOfSelectedItems!!) // сохранение индексов
                        // После выбора списка тренеруемых частей тела
                        // станет доступен выбор мышц
                        binding.musclesIsVisible = true
                        // Переприкрепим переключатель к появившемуся View
                        bottomOfSwitcherToTopOfMusclesSelector()
                    }
                }
                if (allBodyParts.isEmpty())
                    throw Exception("allBodyParts is empty")
                if (viewModel.boolBodyPartSelected.value == null)
                    throw Exception("viewModel.boolBodyPartSelected.value is null")
                else if (viewModel.boolBodyPartSelected.value!!.isEmpty())
                    throw Exception("viewModel.boolBodyPartSelected.value is empty")
                // Запуск диалогового окна с выбором частей тела
                MultiChoiceDialog(
                    allBodyParts,
                    viewModel.boolBodyPartSelected.value!!.toBooleanArray()
                ).show(parentFragmentManager, SELECT_BODY_PART_DIALOG)
            }
            /** Выбор мышц для выбранных частей тела -- вызов соответствующего окна и обработка */
            binding.musclesSelector.id -> {
                // Устанавливаем слушатель для получения списка мышц по ключу SELECT_MUSCLE_DIALOG.
                // Список мышц определяется в зависимости от выбранных частей тела
                setFragmentResultListener(SELECT_MUSCLE_DIALOG) { _, bundle ->
                    numbersOfSelectedItems = bundle.getIntegerArrayList(LIST_BUNDLE_TAG)
                    viewModel.selectedMuscles.value = numbersOfMusclesToList(numbersOfSelectedItems!!)
                    viewModel.saveSelectedMuscles(numbersOfSelectedItems!!)
                    if (viewModel.selectedMuscles.value!!.isNotEmpty()) {
                        binding.musclesSelector.text = getString(
                            R.string.number_of_selected_el,
                            numbersOfSelectedItems?.size
                        )
                    }
                    else binding.musclesSelector.text = null
                }
                if (allBodyParts.isEmpty())
                    throw Exception("allBodyParts is empty now")
                if (viewModel.boolBodyPartSelected.value.isNullOrEmpty())
                    throw Exception("viewModel.boolBodyPartSelected.value is null or empty now")
                // Запуск диалогового окна с выбором мышц
                MultiChoiceDialog(
                    musclesToBodyParts,
                    viewModel.boolMusclesSelected.value!!.toBooleanArray()
                ).show(parentFragmentManager, SELECT_MUSCLE_DIALOG)
            }
        }
    }

    /**
     * Метод для преобразования полученных номеров частей тела
     * в строку. Соответственно (0 -> Arms,...)
     */
    private fun numbersOfPartsToString(array: ArrayList<Int>): String {
        var resultString: String = ""
        for (i in array.indices) { // Конкатенируем строки с пробелами
            resultString += (allBodyParts[array[i]] + ",")
        }
        return resultString.dropLast(1)
    }
    /**
     * Метод для преобразования полученных номеров мускулов в строку.
     */
    private fun numbersOfMusclesToString(array: ArrayList<Int>): String? {
        if (array.isEmpty()) return null
        var resultString: String = ""
        for (i in array.indices) { // Конкатенируем строки с пробелами
            resultString += (musclesToBodyParts[array[i]] + ", ")
        }
        resultString.dropLast(2)
        return resultString
    }
    /**
     * Метод для преобразования полученных номеров частей тела в список.
     */
    private fun numbersOfBodyPartsToList(array: ArrayList<Int>): List<String> {
        val mutableList: MutableList<String> = mutableListOf()
        for (i in array.indices) {
            mutableList.add(allBodyParts[array[i]])
        }
        return mutableList.toList()
    }
    /**
     * Метод для преобразования полученных номеров мускулов в список.
     */
    private fun numbersOfMusclesToList(array: ArrayList<Int>): List<String> {
        if (array.isEmpty()) return emptyList()
        val mutableList: MutableList<String> = mutableListOf()
        for (i in array.indices) {
            mutableList.add(musclesToBodyParts[array[i]])
        }
        return mutableList.toList()
    }

    /**
     * Метод для установки значений массивам,
     * хранящим мышцы для соответствующей части тела,
     * переданной в параметре после получения результата диалогового окна
     */
    private fun defineMuscleArrays(array: ArrayList<Int>) {
        // соответствие для каждого номера выбранных частей тела
        val appropriateMuscle = mapOf<Int, Int>(
            0 to R.array.arms_muscles, // массив с мышцами рук
            1 to R.array.legs_muscles, // массив с мышцами ног
            2 to R.array.core_muscles, // массив с мышцами кора
            3 to R.array.back_muscles, // массив с мышцами спины
            4 to R.array.chest_muscles, // массив с мышцами груди
        )
        // Создаем временный массив для заполнения данными из ресурсов (arrays.xml)
        val tempArray = arrayListOf<String>()
        // Добавляем соответствующий массив со строками из ресурсов
        for (i in array.indices) {
            tempArray.addAll(resources.getStringArray(appropriateMuscle[array[i]]!!))
        }
        // Убираем одинаковые элементы списка
        if (array.contains(2)) {
            if (array.contains(3))
                tempArray.remove(resources.getString(R.string.array_item_lower_back))
            if (array.contains(1))
                tempArray.remove(resources.getString(R.string.array_item_glutes))
        }
        // Инициализируем массив с загруженными данными из ресурсов (пока пуст)
        musclesToBodyParts = Array(tempArray.size) { "" }

        // Инициализируем массив с boolean, хранящий true (под данным индексом элемент выбран)
        // и false (если под данным индексом элемент не выбран)
        // setMultiChoiceItems внутри диалогового окна
        if (viewModel.boolMusclesSelected.value != null) { // Если активити было восстановлено,
            // то проверим на сходство размеров списков. Также не забываем проверить то,
                // что список с мускулами после восстановлением соответствует настоящему

            if (viewModel.selectedMuscles.value == null || tempArray.size != viewModel.boolMusclesSelected.value!!.size
                || !tempArray.containsAll(viewModel.selectedMuscles.value!!)) {
                // Если список нужно загрузить (данные отличаются) то
                viewModel.nullifyMuscleData() // обнуляем списки
                viewModel.boolMusclesSelected.value = Array<Boolean>(tempArray.size) { false }
                binding.musclesSelector.text = null // Устанавливаем вместо текста подсказку
            }
        }
        else { // В случае, если в модели список с мышцами пуст, то загружаем
            viewModel.boolMusclesSelected.value = Array<Boolean>(tempArray.size) { false }
        }
        // Поэлементно записываем значения из временного массива в основной
        tempArray.forEachIndexed { i, string ->
            musclesToBodyParts[i] = string
        }
        // Очищаем временный массив
        tempArray.clear()
    }
    /**
     * Метод для установки значений массивам,
     * хранящим части тела для отображения в диалоговых окнах
     */
    private fun defineBodyPartsArrays() {
        // Массив хранит полученные из ресурсов объекты (части тела)
        allBodyParts = resources.getStringArray(R.array.dialog_body_part)
        // Массив хранит выбранные элементы из списка (SelectDialog)
        if (viewModel.boolBodyPartSelected.value.isNullOrEmpty())
            viewModel.boolBodyPartSelected.value = Array(allBodyParts.size) { false }
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
        //binding.placeSpinner.isSelected = false
    }

    /** ИНТЕРФЕЙС (spinner)
     * Определеяем действия по выбору объекта из списка spinner
     */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        viewModel.trainingPlace = parent.getItemAtPosition(pos).toString()
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
        return !(viewModel.restTime == 0 || viewModel.selectedBodyParts.value == null)
    }
    /**
     * Метод вызовет error у не заполненных объектов TextView
     */
    private fun requireAllFieldsCompleted() {
        if (viewModel.restTime == 0)
                binding.timeSliderDescription.error = getString(R.string.error_time_slider)
        if (viewModel.selectedBodyParts.value == null)
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
        if (viewModel.boolBodyPartSelected.value != null) {
            var resultString: String = ""
            viewModel.boolBodyPartSelected.value!!.forEachIndexed { index, value ->
                // Конкатенируем строки с пробелами
                if (value)
                    resultString += (allBodyParts[index] + ",")
            }
            binding.bodyPartSelector.text = resultString.dropLast(1)
            resultString = ""
        }
        // Если список с мышцами не пуст, то восстановим
        if (viewModel.boolMusclesSelected.value != null && viewModel.boolBodyPartSelected.value != null) {
            val arr = arrayListOf<Int>()
            viewModel.boolBodyPartSelected.value!!.forEachIndexed { index, value ->
                if (value) {
                    arr.add(index)
                }
            }
            // Определим список с мышцами, который отобразится в диалоговом окне с выбором мышц
            defineMuscleArrays(arr)
            arr.clear() // Удаляем уже не нужный массив
            if (viewModel.selectedMuscles.value != null && viewModel.selectedMuscles.value?.size != 0)
                binding.musclesSelector.text = getString(
                R.string.number_of_selected_el,
                viewModel.selectedMuscles.value?.size
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
