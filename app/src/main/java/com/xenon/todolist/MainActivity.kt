package com.xenon.todolist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xenon.todolist.activities.BaseActivity
import com.xenon.todolist.activities.SettingsActivity
import com.xenon.todolist.databinding.ActivityMainBinding
import com.xenon.todolist.fragments.NewTaskSheetFragment
import com.xenon.todolist.fragments.TaskItemFragment
import com.xenon.todolist.fragments.TaskListFragment
import com.xenon.todolist.viewmodel.LiveListViewModel
import com.xenon.todolist.viewmodel.TaskItemViewModel
import com.xenon.todolist.viewmodel.TaskListViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPref: SharedPreferences
    private lateinit var taskItemsModel: TaskItemViewModel
    private lateinit var taskListModel: TaskListViewModel

    private var newTaskSheet: NewTaskSheetFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pInfo = packageManager.getPackageInfo(packageName, 0)
        if (pInfo.packageName.endsWith(".debug")) {
            title = "$title (DEBUG)"
        }

        sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        setupTaskItemFragment()
        setupTaskListFragment()

        adjustBottomMargin(binding.mainLinearLayout ?: binding.CoordinatorLayoutMain, binding.NewTaskButton)

        binding.NewTaskButton.setOnClickListener {
            if (newTaskSheet == null || !newTaskSheet!!.isAdded) {
                newTaskSheet = NewTaskSheetFragment.getInstance(taskItemsModel, null)
                newTaskSheet?.showNow(supportFragmentManager, newTaskSheet!!.tag)
            }
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            binding.appbar.setExpanded(false, false)

        setupToolbar()
        loadTaskList()

        var selectedIdx = sharedPref.getInt("selectedTaskList", 0)
        if (selectedIdx >= taskListModel.getList().size)
            selectedIdx = 0
        val taskList = taskListModel.getList()[selectedIdx]
        taskItemsModel.setList(taskList.items)
    }

    override fun onResume() {
        super.onResume()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            binding.appbar.setExpanded(false, false)
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {}
                R.id.sort -> openSortDialog()
                R.id.settings -> openSettingsActivity()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout?.openDrawer(binding.navView!!)
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
            .setPositiveButton(R.string.ok) { _, _ ->
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
            }
            .setNegativeButton(R.string.cancel, null)
            .setTitle(R.string.sort_by)
            .setView(view)
            .show()
    }

    private fun openSettingsActivity() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun loadTaskList() {
        val json = sharedPref.getString("taskList", "[]")
        try {
            val list = Json.decodeFromString<ArrayList<TaskList>>(json!!)
            if (list.size == 0) {
                loadDefaultTaskList()
                return
            }
            taskListModel.setList(list)
        } catch (e: Exception) {
            loadDefaultTaskList()
        }
    }

    private fun loadDefaultTaskList() {
        val defaultList = ArrayList<TaskList>()
        defaultList.add(TaskList(0, getString(R.string.default_task_list), ArrayList(), System.currentTimeMillis()))
        taskListModel.setList(defaultList)
        saveTaskList()
    }

    private fun saveTaskList() {
        val json = Json.encodeToString(taskListModel.getList())
        with(sharedPref.edit()) {
            putString("taskList", json)
            apply()
        }
    }

    private fun setupTaskItemFragment() {
        val fragment = binding.taskItemFragment.getFragment<TaskItemFragment>()
        taskItemsModel = fragment.getViewModel()
        taskItemsModel.listStatus.observe(this) { change ->
            if (change.type == LiveListViewModel.ListChangedType.REMOVE) {
                Snackbar.make(
                    binding.NewTaskButton,
                    getString(R.string.task_deleted),
                    Snackbar.LENGTH_SHORT
                )
                    .setAction(getString(R.string.undo)) {
                        taskItemsModel.add(change.item!!, change.idx)
                    }
                    .setTextColor(ContextCompat.getColor(this, com.xenon.commons.accesspoint.R.color.onSurface))
                    .setActionTextColor(ContextCompat.getColor(this, com.xenon.commons.accesspoint.R.color.primary))
                    .setBackgroundTint(ContextCompat.getColor(this, com.xenon.commons.accesspoint.R.color.surface))
                    .show()
            }

            saveTaskList()
        }
        fragment.setClickListener(object : TaskItemClickListener {
            override fun editTaskItem(taskItem: TaskItem) {
                if (newTaskSheet == null || !newTaskSheet!!.isAdded) {
                    newTaskSheet = NewTaskSheetFragment.getInstance(taskItemsModel, taskItem)
                    newTaskSheet?.showNow(supportFragmentManager, newTaskSheet!!.tag)
                }
            }

            override fun completeTaskItem(taskItem: TaskItem) {
                taskItem.toggleCompleted()
                taskItemsModel.update(taskItem, true)
            }
        })
    }

    private fun setupTaskListFragment() {
//        class MyDrawerListener : DrawerLayout.SimpleDrawerListener() {
//            override fun onDrawerClosed(drawerView: View) {
//            }
//        }
//        binding.drawerLayout.addDrawerListener(MyDrawerListener())
        val fragment = binding.taskListFragment.getFragment<TaskListFragment>()
        taskListModel = fragment.getViewModel()
        taskListModel.listStatus.observe(this) {change ->
            if (change.type == LiveListViewModel.ListChangedType.ADD) {
                selectTaskList(change.idx)
//                binding.drawerLayout.closeDrawers()
            }
            saveTaskList()
        }
        fragment.setClickListener(object : TaskListAdapter.TaskListClickListener {
            override fun editTaskList(taskList: TaskList, position: Int) {
            }

            override fun selectTaskList(taskList: TaskList, position: Int) {
                selectTaskList(position)
                binding.drawerLayout?.closeDrawers()
            }
        })

//        for (i in 0..20) {
//            val item = binding.navView?.menu?.add(0, i, 0, "item $i")
//            item?.actionView = View.inflate(this, R.layout.nav_drawer_button, null)
//            item?.icon = AppCompatResources.getDrawable(this, R.drawable.ic_arrow_left_vector)
//        }
//        binding.navView?.menu?.setGroupCheckable(0, true, false)
//        binding.navView?.menu?.getItem(0)?.isChecked = true
//        binding.navView?.menu?.getItem(3)?.isChecked = true
//
//        binding.navView?.setNavigationItemSelectedListener { menuItem ->
//            val position = taskListModel.getList().indexOfFirst { it.id == menuItem.itemId }
//            selectTaskList(position)
//            // Slight delay to prevent stutter when closing
////            Handler(Looper.getMainLooper()).postDelayed({
////                binding.drawerLayout?.closeDrawers()
////            }, 0)
//            true
//        }


        binding.addListButton?.setOnClickListener {
            showAddListDialog()
        }
    }

    private fun showAddListDialog() {
        val addTaskView = layoutInflater.inflate(R.layout.alert_add_task_list, null)
        val titleEditText = addTaskView.findViewById<EditText>(R.id.listNameEditText)
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.create_task_list_dialog)
            .setPositiveButton(R.string.save) { _, _ ->
                val taskListName = titleEditText.text.toString()
//                Toast.makeText(requireContext(), "Empty field", Toast.LENGTH_LONG).show()
                if (taskListName.isNotBlank())
                    taskListModel.add(TaskList(-1, taskListName, ArrayList(), System.currentTimeMillis()))
            }
            .setNegativeButton(R.string.cancel, null)
            .setView(addTaskView)

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            titleEditText.requestFocus()
        }
        titleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = p0?.isNotBlank() ?: false
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })
        dialog.show()
    }

    private fun selectTaskList(position: Int) {
        val taskList = taskListModel.getList()[position]
        taskItemsModel.setList(taskList.items)
        val taskListFragment = binding.taskListFragment.getFragment<TaskListFragment>()
        taskListFragment.selectTaskList(position)
        sharedPref.edit().putInt("selectedTaskList", position).apply()
    }
}
