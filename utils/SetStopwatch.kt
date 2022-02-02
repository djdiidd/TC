package com.companion.android.trainingcompanion.utils

import android.app.Activity
import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import com.companion.android.trainingcompanion.R

class SetStopwatch(context: Context) {
    val setTimer: TextView = (context as Activity).findViewById(R.id.set_timer)
    val setProgressBar: ProgressBar = (context as Activity).findViewById(R.id.progress_circular)
}