package com.companion.android.trainingcompanion.objects

import android.content.Context
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.models.SimpleSpinnerItem

object BreakNotificationMode {

    const val SOUND    : Int = 111
    const val VIBRATION: Int = 112
    const val ANIMATION: Int = 113

    private val images = intArrayOf(
        R.drawable.ic_with_sound,
        R.drawable.ic_vibration,
        R.drawable.ic_screen_glow
    )

    private var list: Array<SimpleSpinnerItem> = arrayOf()

    private fun getTitles(context: Context): Array<String> {
        return context.resources.getStringArray(R.array.break_notification)
    }

    fun getList(context: Context): Array<SimpleSpinnerItem> {
        if (list.isNotEmpty())
            return list
        val result = Array(images.size) {SimpleSpinnerItem("", 0)}
        val titles = getTitles(context)
        for (i in images.indices) {
            val image = images[i]
            val title = titles[i]
            result[i] = SimpleSpinnerItem(title, image)
        }
        list = result
        return list
    }

}