package com.xenon.todolist.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xenon.todolist.R
import com.xenon.todolist.TodoList
import com.xenon.todolist.TodoListAdapter
import com.xenon.todolist.databinding.FragmentTodoListsBinding
import com.xenon.todolist.viewmodel.LiveListViewModel
import com.xenon.todolist.viewmodel.TodoListViewModel

class TodoListFragment : Fragment(R.layout.fragment_todo_lists) {

    private lateinit var binding: FragmentTodoListsBinding
    private lateinit var todoListModel: TodoListViewModel
    private var clickListener: TodoListAdapter.TodoListClickListener = object : TodoListAdapter.TodoListClickListener {
        override fun editTodoList(taskList: TodoList, position: Int) {
        }

        override fun selectTodoList(taskList: TodoList, position: Int) {
        }
    }
    private var selectedTodoListIdx: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        todoListModel = ViewModelProvider(this)[TodoListViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecyclerView()
        updateRecyclerViewScroll()

        val context = requireContext()
        val sharedPref = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        selectTodoList(sharedPref.getInt("selectedTodoList", 0))
    }

    override fun onResume() {
        super.onResume()
        updateRecyclerViewScroll()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTodoListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun setClickListener(listener: TodoListAdapter.TodoListClickListener) {
        clickListener = listener
    }

    fun selectTodoList(idx: Int) {
        val adapter = binding.todoListRecyclerView.adapter as TodoListAdapter
        adapter.selectedItemPosition = idx
        if (selectedTodoListIdx >= 0) {
            adapter.notifyItemChanged(selectedTodoListIdx, true)
        }
        selectedTodoListIdx = idx
        adapter.notifyItemChanged(selectedTodoListIdx, true)
    }

    fun getViewModel(): TodoListViewModel {
        return todoListModel
    }

    private fun updateRecyclerViewScroll() {
    }

    @SuppressLint("NotifyDataSetChanged", "ResourceAsColor", "RestrictedApi")
    private fun setRecyclerView() {
        val context = requireContext()
        val adapter = TodoListAdapter(context, todoListModel.getList(), clickListener)
        binding.todoListRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.todoListRecyclerView.adapter = adapter
        binding.todoListRecyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
        })

        todoListModel.liveListEvent.observe(viewLifecycleOwner) { change ->
            when (change.type) {
                LiveListViewModel.ListChangedType.ADD -> {
                    adapter.notifyItemInserted(change.idx)
                }
                LiveListViewModel.ListChangedType.REMOVE -> {
                    adapter.notifyItemRemoved(change.idx)
                }
                LiveListViewModel.ListChangedType.MOVED -> {
                    adapter.notifyItemMoved(change.idx, change.idx2)
                }
                LiveListViewModel.ListChangedType.UPDATE -> {
                    Log.d("iip", "UPDATE $change")
                    adapter.notifyItemChanged(change.idx)
                }
                LiveListViewModel.ListChangedType.MOVED_AND_UPDATED -> {
                    adapter.notifyItemChanged(change.idx)
                    adapter.notifyItemMoved(change.idx, change.idx2)
                    if (change.idx == 0) {
                        binding.todoListRecyclerView.scrollToPosition(0)
                    }
                }
                LiveListViewModel.ListChangedType.OVERWRITTEN -> {
                    adapter.taskItems = todoListModel.getList()
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

}