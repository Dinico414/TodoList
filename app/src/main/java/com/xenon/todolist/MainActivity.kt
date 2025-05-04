package com.xenon.todolist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xenon.commons.accesspoint.R.color
import com.xenon.commons.accesspoint.R.drawable
import com.xenon.todolist.activities.BaseActivity
import com.xenon.todolist.activities.SettingsActivity
import com.xenon.todolist.databinding.ActivityMainBinding
import com.xenon.todolist.fragments.TaskDialogFragment
import com.xenon.todolist.fragments.TaskItemFragment
import com.xenon.todolist.fragments.TodoListFragment
import com.xenon.todolist.viewmodel.LiveListViewModel
import com.xenon.todolist.viewmodel.TaskItemViewModel
import com.xenon.todolist.viewmodel.TodoListViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var taskItemsModel: TaskItemViewModel
    private lateinit var todoListModel: TodoListViewModel
    private var currentTheme: Int = 0

    private var newTaskSheet: TaskDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pInfo = packageManager.getPackageInfo(packageName, 0)
        if (pInfo.packageName.endsWith(".debug")) {
            title = "$title (DEBUG)"
        }


        sharedPreferences = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        setupTaskItemFragment()
        setupTodoListFragment()

        adjustBottomMargin(
            binding.mainLinearLayout ?: binding.coordinatorLayoutMain,
            binding.NewTaskButton
        )

        binding.NewTaskButton.setOnClickListener {
            if (newTaskSheet == null || !newTaskSheet!!.isAdded) {
                newTaskSheet = TaskDialogFragment.getInstance(taskItemsModel, null)
                newTaskSheet?.showNow(supportFragmentManager, newTaskSheet!!.tag)
            }
        }

        setupToolbar()
        loadTodoList()

        var selectedIdx = sharedPreferences.getInt("selectedTodoList", 0)
        if (selectedIdx >= todoListModel.getList().size)
            selectedIdx = 0
        todoListModel.selectedIdx.postValue(selectedIdx)

//        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
//            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
//            binding.listActionButton.visibility = if(imeVisible) View.GONE else View.VISIBLE
//            insets
//        }
    }

    override fun onResume() {
        super.onResume()
        applyTheme(true)
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            Log.d("bbb", "CLICK 4 ${menuItem.title}")
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

        val searchItem = binding.toolbar.menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView?
        searchView?.apply {
            this.setOnSearchClickListener {
                binding.collapsingToolbar?.setCollapsedTitleTextColor(Color.TRANSPARENT)
            }
            this.setOnCloseListener {
                binding.collapsingToolbar?.setCollapsedTitleTextColor(resources.getColor(color.textOnPrimary))
                false
            }
            this.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }
                override fun onQueryTextChange(newText: String?): Boolean {
                    return false
                }
            })
        }

        onBackPressedDispatcher.addCallback {
            if (searchView?.isIconified == false) {
                searchView.isIconified = true
            }
        }
    }

    private fun openSortDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_set_sorting, null)
        val radioView = view.findViewById<RadioGroup>(R.id.sorting_dialog_radio_sorting)
        radioView.check(
            when (taskItemsModel.getSortType()) {
                TaskItemViewModel.SortType.BY_COMPLETENESS -> R.id.sorting_dialog_radio_by_completeness
                TaskItemViewModel.SortType.BY_CREATION_DATE -> R.id.sorting_dialog_radio_by_creation_date
                TaskItemViewModel.SortType.BY_DUE_DATE -> R.id.sorting_dialog_radio_by_due_date
                else -> R.id.sorting_dialog_radio_by_none
            }
        )

        MaterialAlertDialogBuilder(this)
            .setPositiveButton(R.string.ok) { _, _ ->
                val sortType = when (radioView.checkedRadioButtonId) {
                    R.id.sorting_dialog_radio_by_creation_date -> TaskItemViewModel.SortType.BY_CREATION_DATE
                    R.id.sorting_dialog_radio_by_completeness -> TaskItemViewModel.SortType.BY_COMPLETENESS
                    R.id.sorting_dialog_radio_by_due_date -> TaskItemViewModel.SortType.BY_DUE_DATE
                    else -> TaskItemViewModel.SortType.NONE
                }
                with(sharedPreferences.edit()) {
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

    private fun loadTodoList() {
        if (todoListModel.getList().isNotEmpty())
            return

        val json = sharedPreferences.getString("todoList", "[]")
        try {
            val list = Json.decodeFromString<ArrayList<TodoList>>(json!!)
            if (list.size == 0) {
                loadDefaultTodoList()
                return
            }
            todoListModel.setList(list)
        } catch (e: Exception) {
            loadDefaultTodoList()
        }
    }

    private fun loadDefaultTodoList() {
        val defaultList = ArrayList<TodoList>()
        defaultList.add(
            TodoList(
                getString(R.string.default_todo_list),
                ArrayList(),
                Instant.now().toEpochMilli()
            )
        )
        todoListModel.setList(defaultList)
    }

    private fun saveTaskList() {
        val json = Json.encodeToString(todoListModel.getList())
        with(sharedPreferences.edit()) {
            putString("todoList", json)
            apply()
        }
    }

    private fun setupTaskItemFragment() {
        val fragment = binding.taskItemFragment.getFragment<TaskItemFragment>()
        taskItemsModel = fragment.getViewModel()
        taskItemsModel.liveListEvent.observe(this) { change ->
            if (change.type == LiveListViewModel.ListChangedType.REMOVE) {
                val snackbar = Snackbar.make(
                    binding.NewTaskButton,
                    getString(R.string.task_deleted),
                    Snackbar.LENGTH_SHORT
                )
                snackbar.setAction(getString(R.string.undo)) {
                    taskItemsModel.add(change.item!!, change.idx)
                }
                val snackbarView = snackbar.view
                snackbarView.background = ContextCompat.getDrawable(
                    this,
                    drawable.tile_popup
                )
                val textColor = ContextCompat.getColor(
                    this,
                    color.inverseOnSurface
                )
                val actionTextColor = ContextCompat.getColor(
                    this,
                    color.inversePrimary
                )
                val backgroundTint = ContextCompat.getColor(
                    this,
                    color.inverseSurface
                )
                snackbar.setTextColor(textColor)
                    .setActionTextColor(actionTextColor)
                    .setBackgroundTint(backgroundTint)
                    .show()
            }

            saveTaskList()
        }

        fragment.setClickListener(object : TaskItemClickListener {
            override fun editTaskItem(taskItem: TaskItem) {
                if (newTaskSheet == null || !newTaskSheet!!.isAdded) {
                    newTaskSheet = TaskDialogFragment.getInstance(taskItemsModel, taskItem)
                    newTaskSheet?.showNow(supportFragmentManager, newTaskSheet!!.tag)
                }
            }

            override fun completeTaskItem(taskItem: TaskItem) {
                taskItem.toggleCompleted()
                taskItemsModel.update(taskItem)
            }
        })
    }

    private fun setupTodoListFragment() {
        val fragment = binding.todoListFragment!!.getFragment<TodoListFragment>()
        todoListModel = fragment.getViewModel()
        todoListModel.liveListEvent.observe(this) { change ->
            if (change.type == LiveListViewModel.ListChangedType.ADD) {
                todoListModel.selectedIdx.value = change.idx
            }
            saveTaskList()
        }
        todoListModel.selectedIdx.observe(this) { change ->
            if (change >= todoListModel.getList().size)
                return@observe
            val list = todoListModel.getList()[change].items
            taskItemsModel.setList(list)
            sharedPreferences.edit() { putInt("selectedTodoList", change) }
        }
        fragment.setClickListener(object : TodoListAdapter.TodoListClickListener {
            override fun onItemEdited(taskList: TodoList, position: Int) {
                showEditListDialog(taskList) {  }
            }

            override fun onItemSelected(taskList: TodoList, position: Int) {
                todoListModel.selectedIdx.value = position
                binding.drawerLayout?.closeDrawers()
            }

            override fun onItemChecked(taskList: TodoList, position: Int, checkedItems: List<TodoList>) {
                if (checkedItems.isNotEmpty()) {
                    setButtonToDeleteStyle(binding.listActionButton)
                    binding.listActionButton.text = getString(R.string.delete_list)
                    binding.listActionButton.setOnClickListener {
                        showDeleteListsDialog(checkedItems) {
                            setButtonToAddListStyle(binding.listActionButton)
                            binding.listActionButton.text = getString(R.string.add_list)
                            binding.listActionButton.setOnClickListener { showAddListDialog() }
                        }
                    }
                } else {
                    setButtonToAddListStyle(binding.listActionButton)
                    binding.listActionButton.text = getString(R.string.add_list)
                    binding.listActionButton.setOnClickListener { showAddListDialog() }
                }
            }
        })
        binding.listActionButton.setOnClickListener {
            showAddListDialog()
        }
        binding.drawerLayout?.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {
                Log.d("aaa", "closed")
                todoListModel.uncheckAll()
            }
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    private fun setButtonToDeleteStyle(button: Button) {
        button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(button.context, color.delete_red))
        button.setTextColor(ContextCompat.getColor(button.context, color.delete))
    }

    private fun setButtonToAddListStyle(button: Button) {
        button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(button.context, color.primary))
        button.setTextColor(ContextCompat.getColor(button.context, color.textOnPrimaryInvert))
    }

    private fun showAddListDialog() {
        val addTaskView = layoutInflater.inflate(R.layout.dialog_add_todo_list, null)
        val titleEditText = addTaskView.findViewById<EditText>(R.id.listNameEditText)
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.create_task_list_dialog)
            .setPositiveButton(R.string.save) { _, _ ->
                val taskListName = titleEditText.text.toString()
                if (taskListName.isNotBlank())
                    todoListModel.add(
                        TodoList(
                            taskListName,
                            ArrayList(),
                            System.currentTimeMillis()
                        )
                    )
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

    private fun showDeleteListsDialog(checkedItems: List<TodoList>, onComplete: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_list)
            .setPositiveButton(R.string.yes) { _, _ ->
                if (checkedItems.size == todoListModel.getList().size) {
                    loadDefaultTodoList()
                    todoListModel.selectedIdx.postValue(0)
                }
                else {
                    checkedItems.forEach {
                        todoListModel.remove(it)
                    }
                }
                onComplete()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditListDialog(item: TodoList, onComplete: () -> Unit) {
        val addTaskView = layoutInflater.inflate(R.layout.dialog_add_todo_list, null)
        val titleEditText = addTaskView.findViewById<EditText>(R.id.listNameEditText)
        titleEditText.setText(item.name)
        titleEditText.setSelection(item.name.length)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_list)
            .setPositiveButton(R.string.yes) { _, _ ->
                item.name = titleEditText.text.toString()
                todoListModel.update(item, true)
                onComplete()
            }
            .setNegativeButton(R.string.cancel, null)
            .setView(addTaskView)
            .show()
    }

    private fun applyTheme(recreateActivity: Boolean = false) {
        val preferenceManager = SharedPreferenceManager(this)

        AppCompatDelegate.setDefaultNightMode(preferenceManager.themeFlag[preferenceManager.theme])

        if (currentTheme == 0) currentTheme = preferenceManager.theme
        val newTheme = if (preferenceManager.amoledDark) R.style.Theme_Xenon_Amoled else preferenceManager.theme

        if (currentTheme != newTheme) {
            currentTheme = newTheme
            setTheme(newTheme)
            if (recreateActivity) recreate()
        }
    }
}
