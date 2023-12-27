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
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        taskItemsModel = ViewModelProvider(this)[TaskItemViewModel::class.java]

        adjustBottomMargin(binding.CoordinatorLayoutMain, this)

        binding.NewTaskButton.setOnClickListener {
            if (newTaskSheet == null || !newTaskSheet!!.isAdded) {
                newTaskSheet = NewTaskSheet.getInstance(taskItemsModel, null)
                newTaskSheet?.showNow(supportFragmentManager, newTaskSheet!!.tag)
            }
        }

        sharedPref = getPreferences(Context.MODE_PRIVATE)

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

                    // Calculate the top margin in pixels
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
//                    if (change.idx == 0 || change.idx == taskItemsModel.getList().size - 1) {
//                        binding.todoListRecycleView.scrollToPosition(change.idx)
//                    }
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
                    binding.todoListRecycleView.adapter?.notifyItemMoved(change.idx, change.idx2)
                    binding.todoListRecycleView.adapter?.notifyItemChanged(change.idx2)
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
                if (taskItemsModel.getList()[fromIdx].isCompleted() != taskItemsModel.getList()[targetIdx].isCompleted()) {
                    return false
                }
                taskItemsModel.move(fromIdx, targetIdx)
                return true
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

        if (taskItem.isCompleted()) {
            var targetIdx = 0
            for ((i, item) in taskItemsModel.getList().reversed().withIndex()) {
                if (!item.isCompleted() || item == taskItem) {
                    targetIdx = taskItemsModel.getList().size - i - 1
                    break
                }
            }
            taskItemsModel.moveAndUpdate(taskItem, targetIdx)
        }
        else {
            taskItemsModel.moveAndUpdate(taskItem, 0)
        }
    }

    private fun onTaskItemsChanged() {
        saveTaskItems()
        if (taskItemsModel.getList().isEmpty()) {
            binding.noTasks.visibility = View.VISIBLE
        } else {
            binding.noTasks.visibility = View.GONE
        }
    }

    private fun adjustBottomMargin(view: View, activity: AppCompatActivity) {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)

        rootView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {

                rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val navigationBarHeight = getNavigationBarHeight(activity)

                val desiredMargin =
                    if (navigationBarHeight > 15.dpToPx()) 0 else (15 - navigationBarHeight).dpToPx()

                val layoutParams = view.layoutParams as CoordinatorLayout.LayoutParams
                layoutParams.bottomMargin = desiredMargin
                view.layoutParams = layoutParams
            }
        })
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
