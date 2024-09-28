package com.xenon.todolist.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xenon.todolist.R
import com.xenon.todolist.TaskList
import com.xenon.todolist.TaskListAdapter
import com.xenon.todolist.databinding.FragmentTaskListBinding
import com.xenon.todolist.viewmodel.LiveListViewModel
import com.xenon.todolist.viewmodel.TaskListViewModel

class TaskListFragment : Fragment(R.layout.fragment_task_list) {

    private lateinit var binding: FragmentTaskListBinding
    private lateinit var taskListModel: TaskListViewModel
    private var clickListener: TaskListAdapter.TaskListClickListener = object : TaskListAdapter.TaskListClickListener {
        override fun editTaskList(taskList: TaskList, position: Int) {
        }

        override fun selectTaskList(taskList: TaskList, position: Int) {
        }
    }
    private var selectedTaskListIdx: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        taskListModel = ViewModelProvider(this)[TaskListViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecyclerView()
        updateRecyclerViewScroll()

        val context = requireContext()
        val sharedPref = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        selectTaskList(sharedPref.getInt("selectedTaskList", -1))

        binding.addListButton.setOnClickListener {
            showAddListDialog()
        }
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
        binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun setClickListener(listener: TaskListAdapter.TaskListClickListener) {
        clickListener = listener
    }

    fun selectTaskList(idx: Int) {
        val adapter = binding.todoListRecyclerView.adapter as TaskListAdapter
        adapter.selectedItemPosition = idx
        if (selectedTaskListIdx >= 0) {
//            taskListModel.update(selectedTaskListIdx)
            adapter.notifyItemChanged(selectedTaskListIdx, true)
        }
        selectedTaskListIdx = idx
//        taskListModel.update(idx)
        adapter.notifyItemChanged(selectedTaskListIdx, true)
    }

    fun getViewModel(): TaskListViewModel {
        return taskListModel
    }

    private fun updateRecyclerViewScroll() {
//        val orientation = resources.configuration.orientation
//        binding.todoListRecyclerView.isNestedScrollingEnabled = orientation != Configuration.ORIENTATION_LANDSCAPE
    }

    @SuppressLint("NotifyDataSetChanged", "ResourceAsColor", "RestrictedApi")
    private fun setRecyclerView() {
        val context = requireContext()
        val adapter = TaskListAdapter(context, taskListModel.getList(), clickListener)
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

        taskListModel.listStatus.observe(viewLifecycleOwner) { change ->
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
                    adapter.taskItems = taskListModel.getList()
                    adapter.notifyDataSetChanged()
                }
            }
        }

//        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
//            override fun getMovementFlags(
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder
//            ): Int {
//                return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.START)
//            }
//
//            override fun onMove(
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder,
//                target: RecyclerView.ViewHolder
//            ): Boolean {
//                val fromIdx = viewHolder.bindingAdapterPosition
//                val targetIdx = target.bindingAdapterPosition
//                return taskListModel.move(fromIdx, targetIdx)
//            }
//
//            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                taskListModel.remove(viewHolder.bindingAdapterPosition)
//            }
//
//            override fun onChildDraw(
//                c: Canvas,
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder,
//                dX: Float,
//                dY: Float,
//                actionState: Int,
//                isCurrentlyActive: Boolean
//            ) {
//                val thresholdInDp = 100.0f
//                val thresholdInPixels = (thresholdInDp * resources.displayMetrics.density).toInt()
//                val limitedDX = if (dX < -thresholdInPixels) -thresholdInPixels.toFloat() else dX
//
//                val backgroundDrawable = ContextCompat.getDrawable(
//                    context,
//                    com.xenon.commons.accesspoint.R.drawable.delete_button
//                )
//
//                val marginInDp = resources.getDimension(com.xenon.commons.accesspoint.R.dimen.floating_margin)
//                val marginInPixels = (marginInDp / resources.displayMetrics.density).toInt()
//
//                backgroundDrawable?.setBounds(
//                    (viewHolder.itemView.right + limitedDX + marginInPixels).toInt(),
//                    viewHolder.itemView.top + marginInPixels * 2,
//                    viewHolder.itemView.right - marginInPixels * 2,
//                    viewHolder.itemView.bottom - marginInPixels * 2
//                )
//
//                backgroundDrawable?.colorFilter = PorterDuffColorFilter(
//                    ContextCompat.getColor(context, com.xenon.commons.accesspoint.R.color.delete_red),
//                    PorterDuff.Mode.SRC_IN
//                )
//
//                val clipPath = Path()
//                clipPath.addRect(
//                    viewHolder.itemView.right.toFloat(),
//                    viewHolder.itemView.top.toFloat(),
//                    viewHolder.itemView.right + limitedDX + marginInPixels,
//                    viewHolder.itemView.bottom.toFloat(),
//                    Path.Direction.CW
//                )
//
//                c.clipPath(clipPath)
//
//                backgroundDrawable?.draw(c)
//
//                RecyclerViewSwipeDecorator.Builder(
//                    context,
//                    c,
//                    recyclerView,
//                    viewHolder,
//                    limitedDX,
//                    dY,
//                    actionState,
//                    isCurrentlyActive
//                )
//                    .addSwipeLeftActionIcon(com.xenon.commons.accesspoint.R.drawable.delete)
//                    .create()
//                    .decorate()
//
//                super.onChildDraw(
//                    c,
//                    recyclerView,
//                    viewHolder,
//                    limitedDX,
//                    dY,
//                    actionState,
//                    isCurrentlyActive
//                )
//            }
//        }).attachToRecyclerView(binding.todoListRecyclerView)
    }

    private fun showAddListDialog() {
        val addTaskView = layoutInflater.inflate(R.layout.alert_add_task_list, null)
        val titleEditText = addTaskView.findViewById<EditText>(R.id.listNameEditText)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.create_task_list_dialog)
            .setPositiveButton(R.string.save) { _, _ ->
                val taskListName = titleEditText.text.toString()
                if (taskListName == "")
                // Toast.makeText(requireContext(), "Empty field", Toast.LENGTH_LONG).show()
                else
                    taskListModel.add(TaskList(-1, taskListName, ArrayList(), System.currentTimeMillis()))
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
}