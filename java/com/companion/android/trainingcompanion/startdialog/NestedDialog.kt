package com.companion.android.trainingcompanion.startdialog


import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.companion.android.trainingcompanion.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Диалоговое окно, которое принимает массив из строк и такой же размерности массива Boolean
 * Массив строк заполнит TextView, а второй будет использоваться конструктором onCreateDialog
 */
class MultiChoiceDialog(private val items: Array<String>, private val itemSelectedList: BooleanArray)
    : DialogFragment(), DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnClickListener{

    // Ранняя инициализация списка с выбранными частями тела
    private var listOfSelectedItems = arrayListOf<Int>()
    /**
     * Процесс создание диалогового окна для выбора с помощью AlertDialog.Builder
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        checkIfSomeSelected()
        val adb: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(  // Название окна
                if (tag == SELECT_BODY_PART_DIALOG) getString(R.string.title_select_body_parts)
                else getString(R.string.title_select_muscles)
            )
            .setMultiChoiceItems(items, itemSelectedList, this)
            .setNegativeButton(R.string.button_dialog_cancel, this)
            .setPositiveButton(R.string.button_dialog_continue, this)

        return adb.create()  // Созданное диалоговое окно
    }

    /** ИНТЕРФЕЙС
     * Слушатель для выбора частей тела (Multi Choice Items)
     */
    override fun onClick(dialog: DialogInterface?, which: Int, isChecked: Boolean) {
        // Если выделяем элемент, то добавляем его
        if (isChecked) listOfSelectedItems.add(which)
        // Если отменяем выбор элемента, то удаляем его
        else listOfSelectedItems.remove(which)
    }
    /** ИНТЕРФЕЙС
     * Слушатель для Negative и Positive Button
     */
    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            // По нажатию кнопки подтвердить: устанавливаем результат
            Dialog.BUTTON_POSITIVE ->  {
                // Если итоговый список не пуст
                if (listOfSelectedItems.isNotEmpty()) {
                    // Сортируем список
                    listOfSelectedItems.sort()
                    // Передаем результат в слушатель по тегу.
                    setFragmentResult(
                        tag!!, // Тег присваивается во время запуска диалогового окна
                        bundleOf(LIST_BUNDLE_TAG to listOfSelectedItems) // Создание bundle
                    )
                }
                // Передать пустой список можем только в том случае, если это передача списка мышц
                else if (tag == SELECT_MUSCLE_DIALOG) {
                    setFragmentResult(  // Передаем результат в слушатель по тегу.
                        tag!!, // Тег присваивается во время запуска диалогового окна
                        bundleOf(LIST_BUNDLE_TAG to listOfSelectedItems) // Создание bundle
                    )
                }
                listOfSelectedItems.clear()
            }
            // По нажатию кнопки отмены: закрываем диалоговое окно
            Dialog.BUTTON_NEGATIVE -> {
                listOfSelectedItems.clear()
            }
        }
    }

    /**
     * Метод для повторного заполнения списка выбранных элементов
     * (После повторного запуска окна, выбранные элементы будут так же отображаться)
     */
    private fun checkIfSomeSelected() {
        itemSelectedList.forEachIndexed { index, value ->
            if (value) {
                listOfSelectedItems.add(index)
            }
        }
    }

    /**
     * Приостановка, которая понадобится для выключения
     * диалогового окна воизбежание ошибок
     */
    override fun onPause() {
        super.onPause()
        this.dismiss()
    }
}