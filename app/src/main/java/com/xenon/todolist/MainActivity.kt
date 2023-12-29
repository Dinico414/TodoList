@file:Suppress("DEPRECATION")

package com.xenon.todolist

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xenon.commons.accesspoint.R
import com.xenon.todolist.databinding.ActivityMainBinding
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity(), TaskItemClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var taskItemsModel: TaskItemViewModel
    private lateinit var sharedPref: SharedPreferences

    private var newTaskSheet: NewTaskSheet? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = getPreferences(Context.MODE_PRIVATE)

        taskItemsModel = ViewModelProvider(this)[TaskItemViewModel::class.java]
        taskItemsModel.setSortType(TaskItemViewModel.SortType.valueOf(sharedPref.getString("sortType", "NONE")!!))

        adjustBottomMargin(binding.CoordinatorLayoutMain, this)

        binding.NewTaskButton.setOnClickListener {
            if (newTaskSheet == null || !newTaskSheet!!.isAdded) {
                newTaskSheet = NewTaskSheet.getInstance(taskItemsModel, null)
                newTaskSheet?.showNow(supportFragmentManager, newTaskSheet!!.tag)
            }
        }

        val activity = this
        (binding.toolbar as Toolbar).setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                com.xenon.todolist.R.id.search -> {}
                com.xenon.todolist.R.id.sort -> {
                    val view = layoutInflater.inflate(com.xenon.todolist.R.layout.dialog_set_sorting, null)
                    val radioView = view.findViewById<RadioGroup>(com.xenon.todolist.R.id.sorting_dialog_radio_sorting)
                    radioView.check(when(taskItemsModel.getSortType()) {
                        TaskItemViewModel.SortType.BY_COMPLETENESS ->com.xenon.todolist.R.id.sorting_dialog_radio_by_completeness
                        TaskItemViewModel.SortType.BY_CREATION_DATE ->com.xenon.todolist.R.id.sorting_dialog_radio_by_creation_date
                        TaskItemViewModel.SortType.BY_DUE_DATE ->com.xenon.todolist.R.id.sorting_dialog_radio_by_due_date
                        else -> com.xenon.todolist.R.id.sorting_dialog_radio_by_none
                    })

                    MaterialAlertDialogBuilder(activity)
                        .setPositiveButton(com.xenon.todolist.R.string.ok) { dialog, which ->
                            val sortType = when (radioView.checkedRadioButtonId) {
                                com.xenon.todolist.R.id.sorting_dialog_radio_by_creation_date -> TaskItemViewModel.SortType.BY_CREATION_DATE
                                com.xenon.todolist.R.id.sorting_dialog_radio_by_completeness -> TaskItemViewModel.SortType.BY_COMPLETENESS
                                com.xenon.todolist.R.id.sorting_dialog_radio_by_due_date -> TaskItemViewModel.SortType.BY_DUE_DATE
                                else -> TaskItemViewModel.SortType.NONE
                            }
                            with (sharedPref.edit()) {
                                putString("sortType", sortType.name)
                                apply()
                            }
                            taskItemsModel.setSortType(sortType)
                        }
                        .setNegativeButton(com.xenon.todolist.R.string.cancel, null)
                        .setTitle(com.xenon.todolist.R.string.sort_by)
                        .setView(view)
                        .show()
                }
                com.xenon.todolist.R.id.settings -> {}
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }

        updateAppbar()
        loadTaskItems()
        setRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        updateAppbar()
    }

    private fun updateAppbar() {
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.appbar.setExpanded(false, false)
            binding.todoListRecycleView.isNestedScrollingEnabled = false
        } else {
            binding.todoListRecycleView.isNestedScrollingEnabled = true
        }
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

    @SuppressLint("NotifyDataSetChanged")
    private fun setRecyclerView() {
        val mainActivity = this
        binding.todoListRecycleView.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = TaskItemAdapter(this@MainActivity, taskItemsModel.getList(), mainActivity)
            class TodoListItemDecoration : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    super.getItemOffsets(outRect, view, parent, state)

                    val marginInPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        10.toFloat(),
                        view.context.resources.displayMetrics
                    ).toInt()

                    val position = parent.getChildAdapterPosition(view)
                    if (position == RecyclerView.NO_POSITION) {
                        val oldPosition = parent.getChildViewHolder(view)?.oldPosition
                        if (oldPosition == 0) {
                            outRect.top = marginInPx
                        }
                    }
                    else if (position == 0) {
                        outRect.top = marginInPx
                    }
                }
            }
            addItemDecoration(TodoListItemDecoration())
        }

        taskItemsModel.taskStatus.observe(this) {change ->
            when (change.type) {
                TaskItemViewModel.TaskChangedType.ADD -> {
                    binding.todoListRecycleView.adapter?.notifyItemInserted(change.idx)
                }
                TaskItemViewModel.TaskChangedType.REMOVE -> {
                    binding.todoListRecycleView.adapter?.notifyItemRemoved(change.idx)
                    Snackbar.make(
                        binding.NewTaskButton,
                        getString(com.xenon.todolist.R.string.task_deleted),
                        Snackbar.LENGTH_SHORT
                    )
                        .setAction(getString(com.xenon.todolist.R.string.undo)) {
                            taskItemsModel.add(change.taskItem!!, change.idx)
                        }
                        .show()
                }
                TaskItemViewModel.TaskChangedType.MOVED -> {
                    binding.todoListRecycleView.adapter?.notifyItemMoved(change.idx, change.idx2)
                }
                TaskItemViewModel.TaskChangedType.UPDATE -> {
                    binding.todoListRecycleView.adapter?.notifyItemChanged(change.idx)
                }
                TaskItemViewModel.TaskChangedType.MOVED_AND_UPDATED -> {
                    binding.todoListRecycleView.adapter?.notifyItemChanged(change.idx)
                    binding.todoListRecycleView.adapter?.notifyItemMoved(change.idx, change.idx2)
                    if (change.idx == 0) {
                        binding.todoListRecycleView.scrollToPosition(0)
                    }
                }
                TaskItemViewModel.TaskChangedType.OVERWRITTEN -> {
                    binding.todoListRecycleView.adapter?.notifyDataSetChanged()
                }
            }
            onTaskItemsChanged()
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.START)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromIdx = viewHolder.bindingAdapterPosition
                val targetIdx = target.bindingAdapterPosition
                return taskItemsModel.move(fromIdx, targetIdx)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                taskItemsModel.remove(viewHolder.bindingAdapterPosition)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val thresholdInDp = 100.0f
                val thresholdInPixels = (thresholdInDp * resources.displayMetrics.density).toInt()
                val limitedDX = if (dX < -thresholdInPixels) -thresholdInPixels.toFloat() else dX

                val backgroundDrawable = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.delete_button
                )

                val marginInDp = resources.getDimension(R.dimen.floating_margin)
                val marginInPixels = (marginInDp / resources.displayMetrics.density).toInt()

                backgroundDrawable?.setBounds(
                    (viewHolder.itemView.right + limitedDX + marginInPixels).toInt(),
                    viewHolder.itemView.top + marginInPixels * 2,
                    viewHolder.itemView.right - marginInPixels * 2,
                    viewHolder.itemView.bottom - marginInPixels * 2
                )

                backgroundDrawable?.colorFilter = PorterDuffColorFilter(
                    ContextCompat.getColor(this@MainActivity, R.color.delete_red),
                    PorterDuff.Mode.SRC_IN
                )

                val clipPath = Path()
                clipPath.addRect(
                    viewHolder.itemView.right.toFloat(),
                    viewHolder.itemView.top.toFloat(),
                    viewHolder.itemView.right + limitedDX + marginInPixels,
                    viewHolder.itemView.bottom.toFloat(),
                    Path.Direction.CW
                )

                c.clipPath(clipPath)

                backgroundDrawable?.draw(c)

                RecyclerViewSwipeDecorator.Builder(
                    this@MainActivity,
                    c,
                    recyclerView,
                    viewHolder,
                    limitedDX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
                    .addSwipeLeftActionIcon(R.drawable.delete)
                    .create()
                    .decorate()

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    limitedDX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }


        }).attachToRecyclerView(binding.todoListRecycleView)

        onTaskItemsChanged()
    }

    override fun editTaskItem(taskItem: TaskItem) {
        if (newTaskSheet == null || !newTaskSheet!!.isAdded) {
            newTaskSheet = NewTaskSheet.getInstance(taskItemsModel, taskItem)
            newTaskSheet?.showNow(supportFragmentManager, newTaskSheet!!.tag)
        }
    }

    override fun completeTaskItem(taskItem: TaskItem) {
        taskItem.toggleCompleted()
        taskItemsModel.moveAndUpdate(taskItem)
    }

    private fun onTaskItemsChanged() {
        saveTaskItems()
        if (taskItemsModel.getList().isEmpty()) {
            binding.noTasks.visibility = View.VISIBLE
        } else {
            binding.noTasks.visibility = View.GONE
        }
    }

    private fun adjustBottomMargin(view: View, activity: AppCompatActivity): Int {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)

        var desiredMargin = 15

        rootView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {

                rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val navigationBarHeight = getNavigationBarHeight(activity)

                desiredMargin = if (navigationBarHeight > 15.dpToPx()) {
                    0.dpToPx()
                } else {
                    (15 - navigationBarHeight).dpToPx()
                }

                val layoutParams = view.layoutParams as CoordinatorLayout.LayoutParams
                val desiredMargin2 = desiredMargin +14.dpToPx()
                layoutParams.bottomMargin = desiredMargin
                view.layoutParams = layoutParams


                setNewTaskButtonMargin(desiredMargin2)
            }
        })

        return desiredMargin
    }

    private fun setNewTaskButtonMargin(margin: Int) {
        val layoutParams = binding.NewTaskButton.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.bottomMargin = margin
        binding.NewTaskButton.layoutParams = layoutParams
    }

    fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    fun getNavigationBarHeight(activity: AppCompatActivity): Int {
        val resources = activity.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")

        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }
}
