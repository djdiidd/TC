package com.companion.android.trainingcompanion.viewmodels

import android.content.Context
import android.util.Log
import androidx.annotation.IntRange
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.companion.android.trainingcompanion.R

/**
 * ViewModel для сохранения необходимых данных, выбранных пользователем
 */
class WorkoutViewModel: ViewModel() {

    // Хранение массива выбранных объектов из доступного списка (boolean)
    private var whichBodyPartSelected = Array(5) { false }
    private var whichMuscleSelected: Array<Boolean> = arrayOf()
    // Хранение пользовательских настроек
    private var timerIsOnlyVibrate: Boolean = false
    @IntRange(from = 15, to = 240)
    private var restTime: Int? = null
    private var trainingPlace = TRAINING_AT_HOME
    // Хранение данных для синхронизации данных
    val workoutSuccessfullyStarted = MutableLiveData<Boolean>()


    /**
     * Сохранение переданных частей тела
     */
    fun saveSelectedBodyParts(array: Array<Boolean>) {
        Log.d("MyTag", "In saveSelectedBodyParts passed array (${array.contentToString()})")
        whichBodyPartSelected = array.also {
            initBooleanMuscleArray(it)
        }
    }

    /**
     * Сохранение переданных мышц
     */
    fun saveSelectedMuscles(array: Array<Boolean>) {
        whichMuscleSelected = array
    }

    /**
     * Переопределение массива с мышцами на false значение
     */
    fun resetSelectedMuscles() {
        for (index in whichMuscleSelected.indices) {
            if (whichMuscleSelected[index])
                whichMuscleSelected[index] = false
        }

    }

    /**
     * Очистка всех сохраненных значений ViewModel
     */
    fun clearAllData() {
        whichBodyPartSelected = emptyArray()
        whichMuscleSelected = emptyArray()
        timerIsOnlyVibrate = false
        trainingPlace = TRAINING_AT_HOME
        restTime = 15
    }

    /**
     * Получение массива с мышцами, соответствуюми выбранным частям тела
     */
    fun getMusclesForSelectedBP(context: Context): Array<String> {
        if (whichBodyPartSelected.isNullOrEmpty())
            throw NullPointerException(
                "DataViewModel -> getMusclesStringList -> isBodyPartSelected.value is null"
            )
        val appropriateMuscle = mapOf(
            0 to R.array.arms_muscles,  // массив с мышцами рук
            1 to R.array.legs_muscles,  // массив с мышцами ног
            2 to R.array.core_muscles,  // массив с мышцами кора
            3 to R.array.back_muscles,  // массив с мышцами спины
            4 to R.array.chest_muscles, // массив с мышцами груди
        )
        val muscleArray = arrayListOf<String>()
        // Добавляем соответствующий массив со строками из ресурсов
        whichBodyPartSelected.forEachIndexed { i, isSelected ->
            if (isSelected) {
                muscleArray.addAll(context.resources.getStringArray(appropriateMuscle[i]!!))
            }
        }
        // Убираем одинаковые элементы списка (которые относятся к разным частям тела)
        if (whichBodyPartSelected[2]) {
            if (whichBodyPartSelected[3])
                muscleArray.remove(context.resources.getString(R.string.array_item_lower_back))
            if (whichBodyPartSelected[1])
                muscleArray.remove(context.resources.getString(R.string.array_item_glutes))
        }
        return muscleArray.toTypedArray()
    }

    /**
     * Получение полного списка с частями тела (из ресурсов)
     */
    fun getAllBP(context: Context): Array<String>{
        return context.resources.getStringArray(R.array.dialog_body_part)
    }

    /**
     * Получение массива строк с выбранными частям тела
     */
    fun getSelectedBP(context: Context): Array<String> {
        if (whichBodyPartSelected.isNullOrEmpty())
            throw java.lang.NullPointerException(
                "DataViewModel -> getStringListOfSelectedBP -> isBodyPartSelected is null or empty"
            )
        val tempList = arrayListOf<String>()
        val fullList = getAllBP(context)
        for (i in whichBodyPartSelected.indices) {
            if (whichBodyPartSelected[i])
                tempList.add(fullList[i])
        }
        return tempList.toTypedArray()
    }

    fun getWhichBPsAreSelected(): Array<Boolean> { return whichBodyPartSelected }

    fun setWhichBPsAreSelected(bodyParts: Array<Boolean>) { whichBodyPartSelected = bodyParts }

    fun getWhichMusclesAreSelected(): Array<Boolean> { return whichMuscleSelected }

    fun setWhichMusclesAreSelected(muscles: Array<Boolean>) { whichBodyPartSelected = muscles }

    fun isTimerOnlyVibrate(): Boolean { return timerIsOnlyVibrate }

    fun isTimerOnlyVibrate(value: Boolean) { timerIsOnlyVibrate = value }

    @IntRange(from = 15, to = 240)
    fun getRestTime(): Int? { return restTime }

    fun setRestTime(@IntRange(from = 15, to = 240) time: Int) { restTime = time }

    fun getTrainingPlace(): Short { return trainingPlace }

    fun setTrainingPlace(place: Short) { trainingPlace = place }
    /**
     * Получение информации: содержит ли массив с частями тела хотя бы 1 выбранный элемент
     */
    fun someBPSelected(): Boolean {
        if (whichBodyPartSelected.isNullOrEmpty()) {
            throw java.lang.NullPointerException(
                "DataViewModel -> someBPSelected -> isBodyPartSelected is null or empty"
            )
        }
        return whichBodyPartSelected.contains(true)
    }

    /**
     * Получение информации: содержит ли массив с мышцами хотя бы 1 выбранный элемент
     */
    fun someMuscleSelected(): Boolean {
        if (whichMuscleSelected.isNullOrEmpty()) {
            throw java.lang.NullPointerException(
                "DataViewModel -> someBPSelected -> whichMuscleSelected is null or empty"
            )
        }
        return whichMuscleSelected.contains(true)
    }

    /**
     * Инициализация массива мышц с необходимым размером
     */
    private fun initBooleanMuscleArray(whichBPSelected: Array<Boolean>) {
        val bodyPartsSizes = arrayOf(4,4,5,4,2)
        var finalSize = 0
        whichBPSelected.forEachIndexed { i, v ->
            if (v) {
                finalSize += bodyPartsSizes[i]
            }
        }              // Изначально задаем значениями false
        whichMuscleSelected = Array(finalSize) { false }
    }

    companion object {
        const val TRAINING_AT_HOME: Short  = 101
        const val TRAINING_IN_GYM: Short   = 202
        const val TRAINING_OUTDOORS: Short = 303
    }
}