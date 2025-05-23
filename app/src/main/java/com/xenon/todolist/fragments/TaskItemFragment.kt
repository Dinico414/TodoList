package com.xenon.todolist.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xenon.todolist.R
import com.xenon.todolist.TaskItem
import com.xenon.todolist.adapter.TaskItemAdapter
import com.xenon.todolist.adapter.TaskItemClickListener
import com.xenon.todolist.databinding.FragmentTaskItemsBinding
import com.xenon.todolist.viewmodel.LiveListViewModel
import com.xenon.todolist.viewmodel.TaskItemViewModel
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlin.math.abs
import kotlin.math.tanh

@Suppress("DEPRECATION")
class TaskItemFragment : Fragment(R.layout.fragment_task_items) {

    private lateinit var binding: FragmentTaskItemsBinding
    private lateinit var taskItemsModel: TaskItemViewModel
    private var clickListener: TaskItemClickListener = object : TaskItemClickListener {
        override fun editTaskItem(taskItem: TaskItem) {
            // Handle edit task item click
        }

        override fun completeTaskItem(taskItem: TaskItem) {
            // Handle complete task item click
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = requireContext()
        val sharedPref = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        taskItemsModel = ViewModelProvider(this)[TaskItemViewModel::class.java]

        val currentSortTypeString = sharedPref.getString("sortType", null)
        val currentSortDirectionString = sharedPref.getString("sortDirection", null) // Get the saved direction

        val currentSortType = if (currentSortTypeString != null) {
            TaskItemViewModel.SortType.valueOf(currentSortTypeString)
        } else {
            TaskItemViewModel.SortType.NONE // Default sort type if none is saved
        }

        val currentSortDirection = if (currentSortDirectionString != null) {
            TaskItemViewModel.SortDirection.valueOf(currentSortDirectionString)
        } else {
            TaskItemViewModel.SortDirection.ASCENDING // Default direction if none is saved
        }

        // Call setSortType with both type and direction
        taskItemsModel.setSortType(currentSortType, currentSortDirection)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecyclerView()
        updateRecyclerViewScroll()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTaskItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateRecyclerViewScroll()
    }

    fun setClickListener(listener: TaskItemClickListener) {
        clickListener = listener
    }

    fun getViewModel(): TaskItemViewModel {
        return taskItemsModel
    }

    private fun updateRecyclerViewScroll() {
        // Update RecyclerView scroll position if needed
    }

    @SuppressLint("NotifyDataSetChanged", "ResourceAsColor", "RestrictedApi")
    private fun setRecyclerView() {
        val context = requireContext()
        val adapter = TaskItemAdapter(context, taskItemsModel.getList(), clickListener)
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
                } else if (position == 0) {
                    outRect.top = marginInPx
                }
            }
        })

        taskItemsModel.liveListEvent.observe(viewLifecycleOwner) { _ ->
            while (true) {
                val change = taskItemsModel.listEventQueue.poll() ?: break
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
                        adapter.taskItems = taskItemsModel.getList()
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            updateNoTasksTextview()
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.START
                )
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

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    // Scale sorting_dialog_ascending the item when dragging starts
                    viewHolder.itemView.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200)
                        .start()
                    // Elevate smoothly when dragging starts
                    viewHolder.itemView.animate().z(20f).setDuration(200).start()

                }
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
                var limitedDX = dX

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

                    val swipeThreshold = getSwipeThreshold(viewHolder)
                    val swipeThresholdPx = viewHolder.itemView.width * swipeThreshold
                    limitedDX = -swipeThresholdPx * abs(tanh(0.8f * dX / swipeThresholdPx)).toFloat()

                    val backgroundDrawable = ContextCompat.getDrawable(
                        context,
                        com.xenon.commons.accesspoint.R.drawable.delete_button
                    )

                    val marginInDp =
                        resources.getDimension(com.xenon.commons.accesspoint.R.dimen.floating_margin)
                    val marginInPixels = (marginInDp / resources.displayMetrics.density).toInt()

                    backgroundDrawable?.setBounds(
                        viewHolder.itemView.right + limitedDX.toInt() + marginInPixels,
                        viewHolder.itemView.top + marginInPixels * 2,
                        viewHolder.itemView.right - marginInPixels * 2,
                        viewHolder.itemView.bottom - marginInPixels * 2
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
                        context,
                        c,
                        recyclerView,
                        viewHolder,
                        limitedDX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                        .addSwipeLeftActionIcon(com.xenon.commons.accesspoint.R.drawable.delete)
                        .create()
                        .decorate()
                }


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

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                //revert the elevation when the drag finishes
                viewHolder.itemView.animate().z(7f).setDuration(200).start()

            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.3f
            }
        }).attachToRecyclerView(binding.todoListRecyclerView)

        updateNoTasksTextview()
    }

    private fun updateNoTasksTextview() {
        if (taskItemsModel.getList().isEmpty()) {
            binding.noTasks.visibility = View.VISIBLE
        } else {
            binding.noTasks.visibility = View.GONE
        }
    }
}