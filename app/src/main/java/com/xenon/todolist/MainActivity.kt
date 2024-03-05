package com.xenon.todolist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xenon.todolist.activities.BaseActivity
import com.xenon.todolist.activities.SettingsActivity
import com.xenon.todolist.databinding.ActivityMainBinding
import com.xenon.todolist.fragments.NewTaskSheetFragment
import com.xenon.todolist.fragments.TaskRecyclerViewFragment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPref: SharedPreferences
    private lateinit var taskItemsModel: TaskItemViewModel
    private lateinit var subTaskItemsModel: TaskItemViewModel

    private var newTaskSheet: NewTaskSheetFragment? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        setupTaskFragment()

        adjustBottomMargin(binding.CoordinatorLayoutMain, binding.NewTaskButton)

        binding.NewTaskButton.setOnClickListener {
            if (newTaskSheet == null || !newTaskSheet!!.isAdded) {
                newTaskSheet = NewTaskSheetFragment.getInstance(taskItemsModel, null)
                newTaskSheet?.showNow(supportFragmentManager, newTaskSheet!!.tag)
            }
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            binding.appbar.setExpanded(false, false)

        setSubItemDrawerView()
        setupToolbar()
        loadTaskItems()
    }

    override fun onResume() {
        super.onResume()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            binding.appbar.setExpanded(false, false)
    }

    private fun setupToolbar() {
        (binding.toolbar as Toolbar).setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {}
                R.id.sort -> openSortDialog()
                R.id.settings -> openSettingsActivity()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun openSortDialog() {
            val view = layoutInflater.inflate(R.layout.dialog_set_sorting, null)
        val radioView = view.findViewById<RadioGroup>(R.id.sorting_dialog_radio_sorting)
        radioView.check(when(taskItemsModel.getSortType()) {
            TaskItemViewModel.SortType.BY_COMPLETENESS ->R.id.sorting_dialog_radio_by_completeness
            TaskItemViewModel.SortType.BY_CREATION_DATE ->R.id.sorting_dialog_radio_by_creation_date
            TaskItemViewModel.SortType.BY_DUE_DATE ->R.id.sorting_dialog_radio_by_due_date
            else -> R.id.sorting_dialog_radio_by_none
        })

        MaterialAlertDialogBuilder(this, R.style.MyAlertDialogTheme)
            .setPositiveButton(R.string.ok) { dialog, which ->
                val sortType = when (radioView.checkedRadioButtonId) {
                    R.id.sorting_dialog_radio_by_creation_date -> TaskItemViewModel.SortType.BY_CREATION_DATE
                    R.id.sorting_dialog_radio_by_completeness -> TaskItemViewModel.SortType.BY_COMPLETENESS
                    R.id.sorting_dialog_radio_by_due_date -> TaskItemViewModel.SortType.BY_DUE_DATE
                    else -> TaskItemViewModel.SortType.NONE
                }
                with (sharedPref.edit()) {
                    putString("sortType", sortType.name)
                    apply()
                }
                taskItemsModel.setSortType(sortType)
                subTaskItemsModel.setSortType(sortType)
            }
            .setNegativeButton(R.string.cancel, null)
            .setTitle(R.string.sort_by)
            .setView(view)
            .show()
    }

    private fun openSettingsActivity() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun loadTaskItems() {
        val json = sharedPref.getString("taskItems", "[]")
        taskItemsModel.setList(try {
            json?.let { Json.decodeFromString(it) }!!
        } catch (e: Exception) {
            ArrayList()
        })
    }

    private fun saveTaskItems() {
        val json = Json.encodeToString(taskItemsModel.getList())
        with(sharedPref.edit()) {
            putString("taskItems", json)
            apply()
        }
    }

    private fun setupTaskFragment() {
        val taskFragment = binding.taskItemFragment.getFragment<TaskRecyclerViewFragment>()
        taskItemsModel = taskFragment.getViewModel()
        taskItemsModel.taskStatus.observe(this) {change ->
            if (change.type == TaskItemViewModel.TaskChangedType.REMOVE) {
                Snackbar.make(
                    binding.NewTaskButton,
                    getString(R.string.task_deleted),
                    Snackbar.LENGTH_SHORT
                )
                    .setAction(getString(R.string.undo)) {
                        taskItemsModel.add(change.taskItem!!, change.idx)
                    }
                    .setTextColor(ContextCompat.getColor(this, com.xenon.commons.accesspoint.R.color.onSurface))
                    .setActionTextColor(ContextCompat.getColor(this, com.xenon.commons.accesspoint.R.color.primary))
                    .setBackgroundTint(ContextCompat.getColor(this, com.xenon.commons.accesspoint.R.color.surface))
                    .show()
            }
            saveTaskItems()
        }
        taskFragment.setClickListener(object : TaskItemClickListener {
            override fun editTaskItem(taskItem: TaskItem) {
                binding.drawerLayout.openDrawer(binding.navView)
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                subTaskItemsModel.setList(taskItem.children)
//                if (newTaskSheet == null || !newTaskSheet!!.isAdded) {
//                    newTaskSheet = NewTaskSheetFragment.getInstance(taskItemsModel, taskItem)
//                    newTaskSheet?.showNow(supportFragmentManager, newTaskSheet!!.tag)
//                }
            }

            override fun completeTaskItem(taskItem: TaskItem) {
                taskItem.toggleCompleted()
                taskItemsModel.moveAndUpdate(taskItem, true)
            }
        })
    }

    private fun setSubItemDrawerView() {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        class MyDrawerListener : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        }
        binding.drawerLayout.addDrawerListener(MyDrawerListener())

        val subTaskFragment = binding.subTaskItemFragment.getFragment<TaskRecyclerViewFragment>()
        subTaskItemsModel = subTaskFragment.getViewModel()
    }
}
