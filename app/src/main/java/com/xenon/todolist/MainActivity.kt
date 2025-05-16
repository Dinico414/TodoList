package com.xenon.todolist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.WindowInfoTracker
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xenon.todolist.activities.BaseActivity
import com.xenon.todolist.activities.SettingsActivity
import com.xenon.todolist.adapter.TaskItemClickListener
import com.xenon.todolist.adapter.TodoListAdapter
import com.xenon.todolist.databinding.ActivityMainBinding
import com.xenon.todolist.fragments.TaskDialogFragment
import com.xenon.todolist.fragments.TaskItemFragment
import com.xenon.todolist.fragments.TodoListFragment
import com.xenon.todolist.viewmodel.LiveListViewModel
import com.xenon.todolist.viewmodel.TaskItemViewModel
import com.xenon.todolist.viewmodel.TodoListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlin.math.roundToInt

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

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

//        fixMargins()

        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Safely collects from WindowInfoTracker when the lifecycle is
                // STARTED and stops collection when the lifecycle is STOPPED.
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { layoutInfo ->
                        // New posture information.
                        val foldingFeature = layoutInfo.displayFeatures
//                            .filterIsInstance<FoldingFeature>()
                            .firstOrNull()
                        // Use information from the foldingFeature object.
//                        if (foldingFeature is FoldingFeature) Toast.makeText(this@MainActivity, "${foldingFeature?.state.toString()} ${foldingFeature?.isSeparating} ${foldingFeature?.orientation.toString()} ${foldingFeature?.occlusionType.toString()}", Toast.LENGTH_SHORT).show()
//                        else if (foldingFeature is DisplayFeature) Toast.makeText(this@MainActivity, "${foldingFeature.toString()}", Toast.LENGTH_SHORT).show()
                    }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyTheme(true)
    }

    private fun fixMargins() {
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//        )
//        val statusBarHeight = resources.getDimensionPixelSize(
//            Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android")
//        )
//        var navHeight = resources.getDimensionPixelSize(
//            Resources.getSystem().getIdentifier("navigation_bar_height", "dimen", "android")
//        )
//        if (navHeight < 15.dp) navHeight = 15.dp
//
////        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
////        displayManager.registerDisplayListener(object : DisplayManager.DisplayListener{
////            override fun onDisplayAdded(displayId: Int) {}
////            override fun onDisplayRemoved(displayId: Int) {}
////            override fun onDisplayChanged(displayId: Int) {}
////        }, null)
////        val ol = object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
////            override fun onOrientationChanged(orientation: Int) {
////
////            }
////        }
////        if (ol.canDetectOrientation()) ol.enable() else ol.disable()
//
//        // Setting margins of specific elements to avoid navbar giving the statusbar a background
//        binding.coordinatorLayoutMain.updatePadding(
//            top = statusBarHeight + binding.coordinatorLayoutMain.paddingTop,
//            bottom = navHeight + binding.coordinatorLayoutMain.paddingBottom
//        )
//        binding.drawerLinearRoot?.apply {
//            val lp = this.layoutParams as ViewGroup.MarginLayoutParams
//            lp.topMargin = statusBarHeight + lp.topMargin
//            lp.bottomMargin = navHeight + lp.bottomMargin
//            this.layoutParams = lp
//        }

//        // This method doesnt seem to work on some devices
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(insets.left, 0, insets.right, insets.bottom)

            binding.coordinatorLayoutMain.updatePadding(top = insets.top)
            binding.drawerLinearRoot?.apply {
                val lp = this.layoutParams as ViewGroup.MarginLayoutParams
                lp.topMargin = insets.top + lp.topMargin
                this.layoutParams = lp
            }

            Toast.makeText(this, insets.toString(), Toast.LENGTH_LONG).show()

            WindowInsetsCompat.CONSUMED
        }
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

        val searchItem = binding.toolbar.menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView?

        val backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (searchView?.isIconified == false) {
                    searchView.setQuery("", false)
                    searchView.clearFocus()
                    searchView.isIconified = true
                }
            }
        }
        onBackPressedDispatcher.addCallback(backPressedCallback)

        searchView?.apply {
            this.setOnSearchClickListener {
                binding.collapsingToolbar?.setCollapsedTitleTextColor(Color.TRANSPARENT)
                backPressedCallback.isEnabled = true
//                binding.toolbar.navigationIcon = null
            }
            this.setOnCloseListener {
                binding.collapsingToolbar?.setCollapsedTitleTextColor(resources.getColor(com.xenon.commons.accesspoint.R.color.textOnPrimary))
                backPressedCallback.isEnabled = false
//                binding.toolbar.navigationIcon = resources.getDrawable(drawable.ic_navigation_vector)
                false
            }
            this.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }
                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText != null) {
                        val text = newText.lowercase()
                        taskItemsModel.setListFilter { item ->
                            item.name.lowercase().contains(text)
                                    || item.dueTimeString.lowercase().contains(newText)
                                    || item.dueDateString.lowercase().contains(newText)
                                    || item.description.lowercase().contains(newText)
                        }
                    }
                    return false
                }
            })
        }
    }

    private fun openSortDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_set_sorting, null)
        val radioView = view.findViewById<RadioGroup>(R.id.sorting_dialog_radio_sorting)
        val directionToggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.sorting_dialog_toggle_direction) // Assuming you added this
        val scrollView = view.findViewById<ScrollView>(R.id.sorting_dialog_scrollview)
        val dividerTop = view.findViewById<View>(R.id.sorting_dialog_divider_1)
        val dividerBottom = view.findViewById<View>(R.id.sorting_dialog_divider_2)

        val saveBtn = view.findViewById<MaterialButton>(R.id.ok)
        val cancelBtn = view.findViewById<MaterialButton>(R.id.cancel)
        val dismissBtn = view.findViewById<ImageButton>(R.id.dismiss)

        // Set initial sort type selection
        radioView.check(
            when (taskItemsModel.getSortType()) {
                TaskItemViewModel.SortType.BY_COMPLETENESS -> R.id.sorting_dialog_radio_by_completeness
                TaskItemViewModel.SortType.BY_CREATION_DATE -> R.id.sorting_dialog_radio_by_creation_date
                TaskItemViewModel.SortType.BY_DUE_DATE -> R.id.sorting_dialog_radio_by_due_date
                TaskItemViewModel.SortType.BY_NAME -> R.id.sorting_dialog_radio_by_name
                TaskItemViewModel.SortType.BY_IMPORTANCE -> R.id.sorting_dialog_radio_by_importance
                else -> R.id.sorting_dialog_radio_by_none
            }
        )

        // Set initial sort direction selection
        when (taskItemsModel.getSortDirection()) {
            TaskItemViewModel.SortDirection.ASCENDING -> directionToggleGroup.check(R.id.sorting_dialog_ascending) // Assuming you added this ID
            TaskItemViewModel.SortDirection.DESCENDING -> directionToggleGroup.check(R.id.sorting_dialog_descending) // Assuming you added this ID
        }

        scrollView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val canScroll = scrollView.height < scrollView.getChildAt(0).height

                if (canScroll) {
                    dividerTop.visibility = View.VISIBLE
                    dividerBottom.visibility = View.VISIBLE
                } else {
                    dividerTop.visibility = View.GONE
                    dividerBottom.visibility = View.GONE
                }
            }
        })

        val dialog = MaterialAlertDialogBuilder(this)
//            .setTitle(R.string.sort_by)
            .setView(view)
            .create()

        saveBtn.setOnClickListener {
            val sortType = when (radioView.checkedRadioButtonId) {
                R.id.sorting_dialog_radio_by_creation_date -> TaskItemViewModel.SortType.BY_CREATION_DATE
                R.id.sorting_dialog_radio_by_completeness -> TaskItemViewModel.SortType.BY_COMPLETENESS
                R.id.sorting_dialog_radio_by_due_date -> TaskItemViewModel.SortType.BY_DUE_DATE
                R.id.sorting_dialog_radio_by_name -> TaskItemViewModel.SortType.BY_NAME
                R.id.sorting_dialog_radio_by_importance -> TaskItemViewModel.SortType.BY_IMPORTANCE
                else -> TaskItemViewModel.SortType.NONE
            }

            val sortDirection = when (directionToggleGroup.checkedButtonId) { // Get checked button ID
                R.id.sorting_dialog_ascending -> TaskItemViewModel.SortDirection.ASCENDING // Assuming you added this ID
                R.id.sorting_dialog_descending -> TaskItemViewModel.SortDirection.DESCENDING // Assuming you added this ID
                else -> TaskItemViewModel.SortDirection.ASCENDING // Default to ascending
            }

            with(sharedPreferences.edit()) {
                putString("sortType", sortType.name)
                putString("sortDirection", sortDirection.name) // Save the sort direction
                apply()
            }
            taskItemsModel.setSortType(sortType, sortDirection) // Call with both type and direction
            dialog.dismiss()
        }
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        dismissBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
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
            if (list.isEmpty()) {
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

    private fun saveTodoList() {
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
                    com.xenon.commons.accesspoint.R.drawable.tile_popup
                )
                val textColor = ContextCompat.getColor(
                    this,
                    com.xenon.commons.accesspoint.R.color.inverseOnSurface
                )
                val actionTextColor = ContextCompat.getColor(
                    this,
                    com.xenon.commons.accesspoint.R.color.inversePrimary
                )
                val backgroundTint = ContextCompat.getColor(
                    this,
                    com.xenon.commons.accesspoint.R.color.inverseSurface
                )
                snackbar.setTextColor(textColor)
                    .setActionTextColor(actionTextColor)
                    .setBackgroundTint(backgroundTint)
                    .show()
            }

            saveTodoList()
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
        val fragment = binding.todoListFragment.getFragment<TodoListFragment>()
        todoListModel = fragment.getViewModel()
        todoListModel.liveListEvent.observe(this) { change ->
            if (change.type == LiveListViewModel.ListChangedType.ADD) {
                todoListModel.selectedIdx.value = change.idx
            }
            saveTodoList()
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
                todoListModel.uncheckAll()
            }
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    private fun setButtonToDeleteStyle(button: Button) {
        button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(button.context, com.xenon.commons.accesspoint.R.color.delete_red))
        button.setTextColor(ContextCompat.getColor(button.context, com.xenon.commons.accesspoint.R.color.delete))
    }

    private fun setButtonToAddListStyle(button: Button) {
        button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(button.context, com.xenon.commons.accesspoint.R.color.primary))
        button.setTextColor(ContextCompat.getColor(button.context, com.xenon.commons.accesspoint.R.color.textOnPrimaryInvert))
    }

    private fun showAddListDialog() {
        val addTaskView = layoutInflater.inflate(R.layout.dialog_add_todo_list, null)

        val title = addTaskView.findViewById<TextView>(R.id.cardTitle)
        val nameEditText = addTaskView.findViewById<EditText>(R.id.listNameEditText)
        val saveBtn = addTaskView.findViewById<MaterialButton>(R.id.save)
        val cancelBtn = addTaskView.findViewById<MaterialButton>(R.id.cancel)
        val dismissBtn = addTaskView.findViewById<ImageButton>(R.id.dismiss)

        val builder = MaterialAlertDialogBuilder(this)
            .setView(addTaskView)
        val dialog = builder.create()

        title.text = resources.getText(R.string.add_list)
        saveBtn.setOnClickListener {
            val taskListName = nameEditText.text.toString()
            if (taskListName.isNotBlank()) {
                todoListModel.add(
                    TodoList(
                        taskListName,
                        ArrayList(),
                        System.currentTimeMillis()
                    )
                )
            }
            dialog.dismiss()
        }
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        dismissBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            saveBtn.isEnabled = false
            nameEditText.requestFocus()
        }
        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                saveBtn.isEnabled = p0?.isNotBlank() == true
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
                    todoListModel.uncheckAll()
                }
                onComplete()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditListDialog(item: TodoList, onComplete: () -> Unit) {
        val addTaskView = layoutInflater.inflate(R.layout.dialog_add_todo_list, null)

        val title = addTaskView.findViewById<TextView>(R.id.cardTitle)
        val nameEditText = addTaskView.findViewById<EditText>(R.id.listNameEditText)
        val saveBtn = addTaskView.findViewById<MaterialButton>(R.id.save)
        val cancelBtn = addTaskView.findViewById<MaterialButton>(R.id.cancel)
        val dismissBtn = addTaskView.findViewById<ImageButton>(R.id.dismiss)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(addTaskView)
            .create()

        title.text = resources.getText(R.string.edit_list)
        nameEditText.setText(item.name)
        nameEditText.setSelection(item.name.length)
        saveBtn.setOnClickListener {
            item.name = nameEditText.text.toString()
            todoListModel.update(item, true)
            onComplete()
            dialog.dismiss()
        }
        saveBtn.isEnabled = item.name.isNotEmpty()
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        dismissBtn.setOnClickListener {
            dialog.dismiss()
        }
        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                saveBtn.isEnabled = p0?.isNotBlank() == true
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })

        dialog.show()
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
