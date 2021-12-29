package com.companion.android.trainingcompanion.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.ArrayList

/**
 * ViewModel для сохранения необходимых данных, выбранных пользователем
 */
class StartViewModel: ViewModel() {

    // Хранение массива выбранных объектов из доступного списка (boolean)
    val boolBodyPartSelected = MutableLiveData<Array<Boolean>>()
    val boolMusclesSelected = MutableLiveData<Array<Boolean>>()
    // Хранение выбранных значений в виде строк
    val selectedBodyParts = MutableLiveData<List<String>>()
    val selectedMuscles = MutableLiveData<List<String>>()
    // Хранение пользовательских настроек
    var timerOnlyVibrate: Boolean = false
    var restTime: Int = 0
    var trainingPlace: String? = null
    // Хранение данных для синхронизации данных
    var workoutSuccessfullyStarted = MutableLiveData<Boolean>()

    /// Стоит пересмотреть решение, основанное на хранении целого списка из строк,
    /// возможно лучше хранить только номера выбранных объектов

    /**
     * Сохранение выбранных частей тела, для отображения данных элементов
     * при повторном открытии диалогового окна с выбором частей тела
     */
    fun saveSelectedBodyParts(list: ArrayList<Int>) {
        if (boolBodyPartSelected.value != null) {
            for (i in boolBodyPartSelected.value!!.indices) boolBodyPartSelected.value!![i] = false
            list.forEachIndexed { _, num ->
                boolBodyPartSelected.value!![num] = true
            }
        }
        else throw NullPointerException("boolBodyPartSelected.value is null")
    }

    /**
     * Сохранение выбранных мышц, для отображения данных элементов
     * при повторном открытии диалогового окна с выбором мышц
     */
    fun saveSelectedMuscles(list: ArrayList<Int>) {
        if (boolMusclesSelected.value != null) {
            for (i in boolMusclesSelected.value!!.indices) boolMusclesSelected.value!![i] = false
            list.forEachIndexed { _, num ->
                boolMusclesSelected.value!![num] = true
            }
        }
        else throw NullPointerException("boolMusclesSelected.value is null")
    }

    /**
     * Обнуление списков с мышцами
     */
    fun nullifyMuscleData() {
        boolMusclesSelected.value = arrayOf()
        selectedMuscles.value = listOf()
    }

    /**
     * Очистка всех сохраненных значений ViewModel
     */
    fun clearAllData() {
        boolBodyPartSelected.value = emptyArray()
        boolMusclesSelected.value = emptyArray()
        selectedBodyParts.value = emptyList()
        selectedMuscles.value = emptyList()
        timerOnlyVibrate = false
        trainingPlace = null
        restTime = 0
    }
}