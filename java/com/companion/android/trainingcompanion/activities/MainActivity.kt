package com.companion.android.trainingcompanion.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.companion.android.trainingcompanion.databinding.ActivityMainBinding
import com.companion.android.trainingcompanion.fragments.ListFragment
import com.companion.android.trainingcompanion.fragments.MainFragment
import com.companion.android.trainingcompanion.utils.StopwatchService
import com.companion.android.trainingcompanion.utils.Stopwatch
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import com.companion.android.trainingcompanion.objects.Place
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.dialogs.*
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel


// Константы для тегов фрагментов
const val tagMainFragment = "main-fragment"
const val tagListFragment = "list-fragment"

private const val STOPWATCH_IS_GOING = "stopwatch-is-going"
private const val STOPWATCH_REMAINING_TIME = "stopwatch-current-time"

class MainActivity : AppCompatActivity(), ChangeRestTimeDialog.Callback, ChangePlaceDialog.Callback,
    WarningUnusedBPDialog.Callback, BreakNotificationDialog.Callback {

    // Инициализация объекта класса привязки данных
    private lateinit var binding: ActivityMainBinding
    // Инициализация объекта Intent для общего времени
    private lateinit var serviceIntent: Intent
    // ViewModel для инкапсуляции некоторых данных времени
    private lateinit var stopwatch: Stopwatch
    // Основная ViewModel, инкапсулирующая данные тренировки
    private val viewModel: WorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FFF", "onCreate")
        binding = DataBindingUtil // определяем привязку данных
            .setContentView(this, R.layout.activity_main)

        // Установка собственного toolbar 'а
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null // и удаление заголовка
        if (savedInstanceState == null)
            hideTrainToolbar() // Скрываем таймер до начала тренировки

        // Инициализация интента для класса сервиса
        serviceIntent = Intent(applicationContext, StopwatchService::class.java)
        stopwatch = Stopwatch(this, serviceIntent)
        // Связывание объекта отправляющего обновленное время и получающего по интенту TIMER_UPDATED
        registerReceiver(stopwatch.newTimeReceiver, IntentFilter(StopwatchService.TIMER_UPDATED))
        // Запускаем стартовый фрагмент
        startMainFragment()

    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()

        /** Слушатель нажатия для элементов бокового меню (с изменениями параметров) */
        binding.sideNavigationView.setNavigationItemSelectedListener {

            when (it.itemId) {
            // Изменение списка выбранных частей тела
                R.id.item_body_parts -> {
                    // По ключу SELECT_BODY_PART_DIALOG_TAG
                    supportFragmentManager.setFragmentResultListener(
                        SELECT_BODY_PART_DIALOG, this) { _, bundle ->
                        // В переменную numbersOfSelectedBodyParts записываем arrayList
                        // полученный из объекта Bundle по ключу BODY_PART_LIST_KEY
                        val whichBPIsSelected = bundle.getBooleanArray(LIST_BUNDLE_TAG)
                        // Если полученный список не изменился, то перезаписывать данные не будем
                        if (!viewModel.getWhichBPsAreSelected().toBooleanArray()
                                .contentEquals(whichBPIsSelected)) {
                            viewModel.updateData(this, whichBPIsSelected!!.toTypedArray())
                            // Запуск диалогового окна с выбором мышц
                            MultiChoiceDialog(
                                viewModel.getAvailableMuscles(this),
                                viewModel.getWhichMusclesAreSelected().toBooleanArray()
                            ).show(supportFragmentManager, SELECT_MUSCLE_DIALOG)
                        }
                    }
                    // Запуск диалогового окна с выбором частей тела
                    MultiChoiceDialog(
                        viewModel.getAllBP(this),
                        viewModel.getWhichBPsAreSelected().toBooleanArray()
                    ).show(supportFragmentManager, SELECT_BODY_PART_DIALOG)
                }
            // Изменение списка выбранных мышц
                R.id.item_muscles -> {
                    supportFragmentManager
                        .setFragmentResultListener(SELECT_MUSCLE_DIALOG, this) {
                                _, bundle ->
                        val numbersOfSelectedItems = bundle.getBooleanArray(LIST_BUNDLE_TAG)
                        viewModel.saveSelectedMuscles(numbersOfSelectedItems!!.toTypedArray())
                    }
                    // Запуск диалогового окна с выбором мышц
                    MultiChoiceDialog(
                        viewModel.getAvailableMuscles(this),
                        viewModel.getWhichMusclesAreSelected().toBooleanArray()
                    ).show(supportFragmentManager, SELECT_MUSCLE_DIALOG)
                }
            // Изменение времени отдыха между подходами
                R.id.item_rest_time -> {
                    ChangeRestTimeDialog(
                        viewModel.restTime.value!!
                    ).show(supportFragmentManager, "")
                }
                R.id.item_place -> {
                    ChangePlaceDialog(viewModel.getTrainingPlace())
                        .show(supportFragmentManager, "")
                }
                R.id.item_switch_mute -> {
                    BreakNotificationDialog(viewModel.getBreakNotificationMode())
                        .show(supportFragmentManager, "")
                }
            }
            true
        }

        // Анимации для нажатий
        val animPressed = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        val animUnpressed = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)


        /** Слушатель для Bottom Navigation View */
        binding.bottomNavigationView.setOnItemSelectedListener { item->
            when (item.itemId) {
                // Если выбираем кнопку для стартовой страницы,
                    // то запускаем соответствующий объект
                R.id.bottom_main -> {  // Передаем bundle со списком
                    val mainFragment = MainFragment()
                    launchFragment(mainFragment, tagMainFragment) // запускаем фрагмент
                    true
                }
                // Если выбираем кнопку со списком,
                    // то запускаем соответствующий объект
                R.id.bottom_list -> {
                    val listFragment = ListFragment()
                    launchFragment(listFragment, tagListFragment) // запускаем фрагмент
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigationView.setOnItemReselectedListener { item->
            when (item.itemId) {
                R.id.bottom_main -> {
                    // do if reselected
                }
                R.id.bottom_list -> {
                    // do if reselected
                }
            }
        }
        /** Слушатель _нажатия_ для кнопки паузы/продолжения общего времени на Toolbar*/
        binding.pauseResumeButton.setOnClickListener {
            it.isClickable = false
            stopwatch.startOrStop()
            Handler(mainLooper).postDelayed( {
                it.isClickable = true
            }, 1000)


        }
        /** Слушатель _касания_ для кнопки паузы/продолжения общего времени на Toolbar*/
        binding.pauseResumeButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> view.startAnimation(animPressed)
                MotionEvent.ACTION_UP   -> view.startAnimation(animUnpressed)
            }
            false
        }
        /** Слушатель _нажатия_ для кнопки опций/настроек на Toolbar*/
        binding.optionsButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
        /** Слушатель _касания_ для кнопки опций/настроек на Toolbar*/
        binding.optionsButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> view.startAnimation(animPressed)
                MotionEvent.ACTION_UP   -> view.startAnimation(animUnpressed)
            }
            false
        }

        /** Отслеживание начала тренировки */
        viewModel.workoutSuccessfullyStarted.observe(this, { workoutStarted ->
            if (!workoutStarted || viewModel.settingsAlreadyApplied) { return@observe }

            Log.d("MyTag", "Started from main")
            showTrainToolbar()
            stopwatch.startOrStop()
            // Устанавливаем соответствующую иконку
            updateWorkoutPlaceIcon()

        })
    }

    /**
     * Функция для запуска переданного фрагмента по тегу:
     * тег для стартового фрагмента, и тег для фрагмента со списком
     */
    private fun launchFragment(fragment: Fragment, tag: String) {
        // Находим фрагмент по заданному тегу
        val currentFragment = supportFragmentManager
            .findFragmentByTag(tag)
        // Если фрагмент с данным тегом не установлен, то создаем
        if (currentFragment == null) {
            supportFragmentManager
                .beginTransaction() // Начинаем транзакцию
                .replace(R.id.fragment_container, fragment, tag) // Заменяем фрагмент
                .commit() // Закрепляем процесс
        }
    }

    /**
     * Функция для запуска стартового фрагмента
     */
    private fun startMainFragment() {
        val fragment = MainFragment() // Запускаем фрагмент передав его
        launchFragment(fragment, tagMainFragment)
    }

    private fun hideTrainToolbar() {
        supportActionBar?.hide()
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    private fun showTrainToolbar() {
        supportActionBar?.show()
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
//        /** Слушатель _нажатия_ для закрытия NavigationView DrawerLayout*/
//        findViewById<ImageView>(R.id.close_nv_button).setOnClickListener {
//            binding.drawerLayout.closeDrawer(GravityCompat.END)
//        }
    }
    private fun updateWorkoutPlaceIcon() {

        binding.sideNavigationView.menu[0].subMenu[0].setIcon(
            when (viewModel.getTrainingPlace()) {
                Place.TRAINING_AT_HOME -> R.drawable.ic_home
                Place.TRAINING_IN_GYM  -> R.drawable.ic_gym
                else -> R.drawable.ic_outdoors
            }
        )
    }

    override fun onStop() {
        super.onStop()
        Log.d("FFF", "stopped from activity")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stopwatch.newTimeReceiver)
        Log.d("MyTag", "Activity destroyed")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putBoolean(STOPWATCH_IS_GOING, stopwatch.isGoing())
            putInt(STOPWATCH_REMAINING_TIME, stopwatch.getRemaining())
        }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.also { b->
            stopwatch.setGoing(b.getBoolean(STOPWATCH_IS_GOING))
            stopwatch.setTime(b.getInt(STOPWATCH_REMAINING_TIME))
            binding.generalClock.text = stopwatch.getTimeInFormatHMMSS(stopwatch.getRemaining())
        }
        if (stopwatch.isGoing()) {
            binding.pauseResumeButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.pauseResumeButton.setImageResource(R.drawable.ic_play)
        }
        super.onRestoreInstanceState(savedInstanceState)
    }
    /* Интерфейс */
    /** Сохранение обновленного значения времени */
    override fun newRestTimeSelected(time: Int) {
        viewModel.restTime.value = time
    }

    /* Интерфейс */
    /** Сохранение обновленного места тренировки */
    override fun newWorkoutPlaceSelected(place: Int) {
        viewModel.setTrainingPlace(place)
        updateWorkoutPlaceIcon()
    }

    /* Интерфейс */
    /** Установка false на неиспользуемыех значениях частей тела */
    override fun unusedBodyPartsRemoved(whichAreUnusedBP: Array<Boolean>) {
        val current = viewModel.getWhichBPsAreSelected()
        for (i in 0 until 5) {
            if (whichAreUnusedBP[i]) {
                current[i] = false
            }
        }
        viewModel.updateData(this, current)
    }

    override fun newBreakNotificationModeSelected(mode: Int) {
        viewModel.setBreakNotificationMode(mode)
    }


}

