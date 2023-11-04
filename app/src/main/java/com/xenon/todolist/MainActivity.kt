package com.xenon.todolist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.xenon.todolist.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), TaskItemClickListener
{
    private lateinit var binding: ActivityMainBinding
    private lateinit var taskViewModel: TaskViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        binding.NewTaskButton.setOnClickListener {
            NewTaskSheet(null).show(supportFragmentManager, "newTaskTag")
        }
        setRecycleView()
    }
    private fun setRecycleView()
    {
        val mainActivity = this
        taskViewModel.taskItems.observe(this)
        {
            binding.todoListRecycleView.apply {
                layoutManager = LinearLayoutManager(applicationContext)
                adapter = it?.let { it1 -> TaskItemAdapter(it1, mainActivity) }
            }
        }
    }

    override fun editTaskItem(taskItem: TaskItem)
    {
       NewTaskSheet(taskItem).show(supportFragmentManager,"newTaskTag")
    }

    override fun completeTaskItem(taskItem: TaskItem)
    {

        taskViewModel.toggleCompleted(taskItem)
    }
}