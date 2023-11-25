package com.xenon.todolist

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.xenon.todolist.databinding.ActivityMainBinding
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity(), TaskItemClickListener {
    private lateinit var binding: ActivityMainBinding
    private var taskItems = ArrayList<TaskItem>()
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.NewTaskButton.setOnClickListener {
            NewTaskSheet(this, null).show(supportFragmentManager, "newTaskTag")
        }

        sharedPref = getPreferences(Context.MODE_PRIVATE)

        loadTaskItems()
        setRecyclerView()
    }

    private fun loadTaskItems() {
        val json = sharedPref.getString("taskItems", "[]")
        taskItems = json?.let { Json.decodeFromString(it) }!!
    }

    private fun saveTaskItems() {
        val json = Json.encodeToString(taskItems)
        with (sharedPref.edit()) {
            putString("taskItems", json)
            apply()
        }
    }

    private fun setRecyclerView() {
        val mainActivity = this
        binding.todoListRecycleView.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = TaskItemAdapter(taskItems, mainActivity)
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
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
        taskItems.remove(taskItem)
        binding.todoListRecycleView.adapter?.notifyItemRemoved(taskItem.idx)
        onTaskItemsChanged()

        if (showUndo) {
            Snackbar.make(binding.NewTaskButton, "Task deleted", Snackbar.LENGTH_SHORT)
                .setAction("Undo") {
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
}
