@file:Suppress("DEPRECATION")

package com.xenon.todolist

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
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
    private var taskItems = ArrayList<TaskItem>()
    private lateinit var sharedPref: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val yourView = findViewById<View>(com.xenon.todolist.R.id.CoordinatorLayoutMain)
        adjustBottomMargin(yourView, this)

        binding.NewTaskButton.setOnClickListener {
            NewTaskSheet(this, null).show(supportFragmentManager, "newTaskTag")
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
        taskItems = json?.let { Json.decodeFromString(it) }!!
    }

    private fun saveTaskItems() {
        val json = Json.encodeToString(taskItems)
        with(sharedPref.edit()) {
            putString("taskItems", json)
            apply()
        }
    }

    private fun setRecyclerView() {
        val mainActivity = this
        binding.todoListRecycleView.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = TaskItemAdapter(this@MainActivity, taskItems, mainActivity)

        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val taskItem = taskItems[viewHolder.adapterPosition]
                mainActivity.removeTaskItem(taskItem)
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
                    .addSwipeLeftBackgroundColor(
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.delete_red
                        )
                    )
                    .addSwipeLeftActionIcon(com.xenon.todolist.R.drawable.baseline_auto_delete_24)
                    .addSwipeLeftPadding(1, 15.0f, 10.0f, 15.0f)
                    .addSwipeLeftCornerRadius(1, 20.0f)
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
        NewTaskSheet(this, taskItem).show(supportFragmentManager, "newTaskTag")
    }

    override fun completeTaskItem(taskItem: TaskItem) {
        taskItem.toggleCompleted()
        updateTaskItem(taskItem)
    }

    fun addTaskItem(taskItem: TaskItem) {
        taskItems.add(taskItem)
        binding.todoListRecycleView.adapter?.notifyItemInserted(taskItems.size - 1)
        onTaskItemsChanged()
    }

    fun removeTaskItem(taskItem: TaskItem, showUndo: Boolean = true) {
        taskItems.removeAt(taskItem.idx)
        binding.todoListRecycleView.adapter?.notifyItemRemoved(taskItem.idx)
        onTaskItemsChanged()

        if (showUndo) {
            Snackbar.make(
                binding.NewTaskButton,
                getString(com.xenon.todolist.R.string.task_deleted),
                Snackbar.LENGTH_SHORT
            )
                .setAction(getString(com.xenon.todolist.R.string.undo)) {
                    taskItems.add(taskItem.idx, taskItem)
                    binding.todoListRecycleView.adapter?.notifyItemInserted(taskItem.idx)
                    onTaskItemsChanged()
                }
                .show()
        }
    }


    fun updateTaskItem(taskItem: TaskItem) {
        val idx = if (taskItem.idx >= 0) taskItem.idx else taskItems.indexOf(taskItem)
        binding.todoListRecycleView.adapter?.notifyItemChanged(idx)
        saveTaskItems()
    }

    private fun onTaskItemsChanged() {
        saveTaskItems()
        if (taskItems.isEmpty()) {
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
