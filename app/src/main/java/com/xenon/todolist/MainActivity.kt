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
import android.os.Bundle
import android.util.Log
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
        val yourView = findViewById<View>(com.xenon.todolist.R.id.CoordinatorLayoutMain)
        adjustBottomMargin(yourView, this)

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

    private fun setRecyclerView() {
        val mainActivity = this
        binding.todoListRecycleView.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = TaskItemAdapter(this@MainActivity, taskItemsModel.getList(), mainActivity)
        }

        taskItemsModel.taskStatus.observe(this) {change ->
            Log.d("Alla", "${change.type} ${change.idx} ${change.taskItem}")
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
//                            addTaskItem(change.taskItem!!, change.idx)
                            taskItemsModel.add(change.taskItem!!, change.idx)
                        }
                        .show()
                }
                TaskItemViewModel.TaskChangedType.UPDATE -> {
                    binding.todoListRecycleView.adapter?.notifyItemChanged(change.idx)
                }
                TaskItemViewModel.TaskChangedType.OVERWRITTEN -> {
                    binding.todoListRecycleView.adapter?.notifyDataSetChanged()
                }
            }
            onTaskItemsChanged()
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
                    com.xenon.todolist.R.drawable.deletebackground
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
                    ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_light),
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
                    .addSwipeLeftActionIcon(com.xenon.todolist.R.drawable.baseline_auto_delete_24)
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
        taskItemsModel.update(taskItem)
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
