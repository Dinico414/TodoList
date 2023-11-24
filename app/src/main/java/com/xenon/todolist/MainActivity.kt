package com.xenon.todolist

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xenon.todolist.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), TaskItemClickListener {
    private lateinit var binding: ActivityMainBinding
    private var taskItems = ArrayList<TaskItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.NewTaskButton.setOnClickListener {
            NewTaskSheet(this, null).show(supportFragmentManager, "newTaskTag")
        }

        setRecyclerView()
    }

    private fun setRecyclerView() {
        val mainActivity = this
        binding.todoListRecycleView.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = TaskItemAdapter(taskItems, mainActivity)
        }

        onTaskItemsMoved()
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
        onTaskItemsMoved()
    }

    fun updateTaskItem(taskItem: TaskItem) {
        val idx = if (taskItem.idx >= 0) taskItem.idx else taskItems.indexOf(taskItem)
        binding.todoListRecycleView.adapter?.notifyItemChanged(idx)
    }

    private fun onTaskItemsMoved() {
        if (taskItems.isEmpty()) {
            binding.noTasks.visibility = View.VISIBLE
        } else {
            binding.noTasks.visibility = View.GONE
        }
    }
}
