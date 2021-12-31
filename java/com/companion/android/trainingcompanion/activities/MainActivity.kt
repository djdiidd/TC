package com.companion.android.trainingcompanion.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.companion.android.trainingcompanion.databinding.ActivityMainBinding
import com.companion.android.trainingcompanion.fragments.ListFragment
import com.companion.android.trainingcompanion.fragments.MainFragment
import com.companion.android.trainingcompanion.utils.TimerService
import com.companion.android.trainingcompanion.viewmodels.TimeViewModel
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.startdialog.*
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel


// Константы для тегов фрагментов
const val tagMainFragment = "main-fragment"
const val tagListFragment = "list-fragment"

class MainActivity : AppCompatActivity(), ChangeRestTimeDialog.Callback {

    // Инициализация объекта класса привязки данных
    private lateinit var binding: ActivityMainBinding
    // Инициализация объекта Intent для общего времени
    private lateinit var serviceIntent: Intent
    // ViewModel для инкапсуляции некоторых данных времени
    private val timeModel: TimeViewModel by lazy {
        ViewModelProvider(this)[TimeViewModel::class.java]
    }
    private val viewModel: WorkoutViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MyTag", "Activity created")
        binding = DataBindingUtil // определяем привязку данных
            .setContentView(this, R.layout.activity_main)

        // Установка собственного toolbar 'а
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null // и удаление заголовка
        if (savedInstanceState == null)
            hideTrainToolbar() // Скрываем таймер до начала тренировки

        // Инициализация интента для класса сервиса
        serviceIntent = Intent(applicationContext, TimerService::class.java)
        // Связывание объекта отправляющего обновленное время и получающего по интенту TIMER_UPDATED
        registerReceiver(timeModel.updateTime, IntentFilter(TimerService.TIMER_UPDATED))
        // Запускаем стартовый фрагмент
        startMainFragment()

    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()
        Log.d("MyTag", "Activity starts")

        binding.sideNavigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.item_body_parts -> {// Устанавливаем слушатель для получения списка выбранных частей тела
                    // По ключу SELECT_BODY_PART_DIALOG_TAG
                    supportFragmentManager.setFragmentResultListener(
                        SELECT_BODY_PART_DIALOG, this) { _, bundle ->

                        // В переменную numbersOfSelectedBodyParts записываем arrayList
                        // полученный из объекта Bundle по ключу BODY_PART_LIST_KEY
                        val whichBPIsSelected = bundle.getBooleanArray(LIST_BUNDLE_TAG)

                        // Если полученный список не изменился, то перезаписывать данные не будем
                        if (!viewModel.getWhichBPsAreSelected().toBooleanArray()
                                .contentEquals(whichBPIsSelected)) {
                            viewModel.saveSelectedBodyParts(whichBPIsSelected!!.toTypedArray()) // сохранение индексов
                            // очищаем данные мышц, так как пользователь обновил части тела
                            viewModel.resetSelectedMuscles()
                            // Запуск диалогового окна с выбором мышц
                            MultiChoiceDialog(
                                viewModel.getMusclesForSelectedBP(this),
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
                R.id.item_muscles -> {
                    supportFragmentManager
                        .setFragmentResultListener(SELECT_MUSCLE_DIALOG, this) {
                                _, bundle ->
                        val numbersOfSelectedItems = bundle.getBooleanArray(LIST_BUNDLE_TAG)
                        viewModel.saveSelectedMuscles(numbersOfSelectedItems!!.toTypedArray())
                    }
                    // Запуск диалогового окна с выбором мышц
                    MultiChoiceDialog(
                        viewModel.getMusclesForSelectedBP(this),
                        viewModel.getWhichMusclesAreSelected().toBooleanArray()
                    ).show(supportFragmentManager, SELECT_MUSCLE_DIALOG)
                }
                R.id.item_rest_time -> {
                    ChangeRestTimeDialog(
                        if (viewModel.getRestTime() == null) 15
                        else viewModel.getRestTime()!!
                    ).show(supportFragmentManager, "")
                }
            }
            true
        }

        // Анимации для нажатий
        val animPressed = AnimationUtils.loadAnimation(this, R.anim.anim_button_pressing)
        val animUnpressed = AnimationUtils.loadAnimation(this, R.anim.anim_button_unpressing)

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
            if (timeModel.generalTimeIsGoing.value == true) {
                timeModel.startOrStopTimer(this, serviceIntent)
            }
            else {
                it.isClickable = false
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    timeModel.startOrStopTimer(this, serviceIntent)
                    it.isClickable = true
                }, 700)
            }
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
            if (workoutStarted && timeModel.generalTimeIsGoing.value == false) {
                showTrainToolbar()
                timeModel.startOrStopTimer(this, serviceIntent)
            }
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
        /** Слушатель _нажатия_ для закрытия NavigationView DrawerLayout*/
        findViewById<ImageView>(R.id.close_nv_button).setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_body_parts -> {
                Toast.makeText(this, "BP", Toast.LENGTH_SHORT).show()
            }
            R.id.item_muscles -> {
                Toast.makeText(this, "M", Toast.LENGTH_SHORT).show()
            }
            R.id.item_rest_time -> {
                Toast.makeText(this, "RT", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onContextItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        Log.d("MyTag", "Activity stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MyTag", "Activity destroyed")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("1", true)
        super.onSaveInstanceState(outState)
    }

    override fun restTimeSelected(time: Int) {
        viewModel.setRestTime(time)
    }
}

