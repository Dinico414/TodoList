package com.xenon.todolist.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.format.DateFormat
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isGone
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.xenon.commons.accesspoint.R.color
import com.xenon.todolist.Importance
import com.xenon.todolist.R
import com.xenon.todolist.TaskItem
import com.xenon.todolist.databinding.DialogNewTaskBinding
import com.xenon.todolist.viewmodel.TaskItemViewModel
import java.time.Instant
import java.util.Calendar

@Suppress("DEPRECATION")
class NewTaskDialogFragment : DialogFragment() {
    companion object {
        private lateinit var taskItemViewModel: TaskItemViewModel
        private lateinit var taskItem: TaskItem
        private var newTask = false

        fun getInstance(taskItemViewModel: TaskItemViewModel, taskItem: TaskItem?): NewTaskDialogFragment {
            Companion.taskItemViewModel = taskItemViewModel
            newTask = taskItem == null
            val curTime = Instant.now().toEpochMilli()
            Companion.taskItem = taskItem ?: TaskItem(0, "", "", -1, -1, curTime, -1, importance = Importance.NO_IMPORTANCE, children = ArrayList(), moreOptionsExpanded = false)
            return NewTaskDialogFragment()
        }
    }

    private lateinit var binding: DialogNewTaskBinding

    private fun updateSaveButtonState(isEnabled: Boolean) {
        binding.saveButton.isEnabled = isEnabled
        binding.saveButton.alpha = if (isEnabled) 1.0f else 0.5f
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogNewTaskBinding.inflate( LayoutInflater.from(requireContext()))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()

        val enabledBackgroundColor = ContextCompat.getColor(requireContext(), color.primary)
        val disabledBackgroundColor = ContextCompat.getColor(requireContext(), color.primary)

        val backgroundTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_enabled)
            ),
            intArrayOf(
                enabledBackgroundColor,
                disabledBackgroundColor
            )
        )
        binding.saveButton.backgroundTintList = backgroundTintList

        val enabledTextColor = ContextCompat.getColor(requireContext(), color.onPrimary)
        val disabledTextColor = ContextCompat.getColor(requireContext(), color.onPrimary)

        val textColorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_enabled)
            ),
            intArrayOf(
                enabledTextColor,
                disabledTextColor
            )
        )
        binding.saveButton.setTextColor(textColorStateList)


        binding.taskDialogScrollview.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.taskDialogScrollview.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val canScroll = binding.taskDialogScrollview.height < binding.taskDialogScrollview.getChildAt(0).height

                if (canScroll) {
                    binding.taskDialogDivider1.visibility = View.VISIBLE
                    binding.taskDialogDivider2.visibility = View.VISIBLE
                } else {
                    binding.taskDialogDivider1.visibility = View.GONE
                    binding.taskDialogDivider2.visibility = View.GONE
                }
            }
        })

        binding.taskDialogDivider1.visibility = View.GONE
        binding.taskDialogDivider2.visibility = View.GONE
        binding.moreOptionsLayout!!.visibility = View.GONE
        binding.moreOptionsButton.text = getString(R.string.more_options)
        binding.moreOptionsButton.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            ContextCompat.getDrawable(requireContext(), R.drawable.arrow_drop_down),
            null
        )

        if (!newTask) {
            binding.taskTitle.text = getString(R.string.edit_task)
            val editable = Editable.Factory.getInstance()
            binding.name.text = editable.newEditable(taskItem.name)
            binding.desc.text = editable.newEditable(taskItem.desc)
            if (taskItem.dueTime > 0)
                updateTimeButtonText()
            if (taskItem.dueDate > 0)
                updateDateButtonText()

            when (taskItem.importance) {
                Importance.HIGHEST_IMPORTANCE -> binding.importanceToggleGroup.check(R.id.highestImportanceButton)
                Importance.HIGH_IMPORTANCE -> binding.importanceToggleGroup.check(R.id.highImportanceButton)
                Importance.NO_IMPORTANCE -> binding.importanceToggleGroup.check(R.id.lowImportanceButton)
            }

            if (taskItem.moreOptionsExpanded) {
                binding.moreOptionsLayout!!.visibility = View.VISIBLE
                binding.moreOptionsButton.text = getString(R.string.less_options)
                binding.moreOptionsButton.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(requireContext(), R.drawable.arrow_drop_up),
                    null
                )
            } else {
                binding.moreOptionsLayout!!.visibility = View.GONE
                binding.moreOptionsButton.text = getString(R.string.more_options)
                binding.moreOptionsButton.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(requireContext(), R.drawable.arrow_drop_down),
                    null
                )
            }

        } else {
            binding.taskTitle.text = getString(R.string.new_task)
            binding.importanceToggleGroup.check(R.id.lowImportanceButton)
            taskItem.moreOptionsExpanded = false
        }

        binding.importanceToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                taskItem.importance = when (checkedId) {
                    R.id.lowImportanceButton -> Importance.NO_IMPORTANCE
                    R.id.highImportanceButton -> Importance.HIGH_IMPORTANCE
                    R.id.highestImportanceButton -> Importance.HIGHEST_IMPORTANCE
                    else -> Importance.NO_IMPORTANCE
                }
            }
        }

        binding.moreOptionsButton.setOnClickListener {
            val arrowDropDown = ContextCompat.getDrawable(requireContext(), R.drawable.arrow_drop_down)
            val arrowDropUp = ContextCompat.getDrawable(requireContext(), R.drawable.arrow_drop_up)
            val primaryColor = ContextCompat.getColor(requireContext(), color.primary)

            arrowDropDown?.let { DrawableCompat.setTint(it, primaryColor) }
            arrowDropUp?.let { DrawableCompat.setTint(it, primaryColor) }

            if (binding.moreOptionsLayout!!.isGone) {
                binding.moreOptionsLayout!!.visibility = View.VISIBLE
                binding.moreOptionsButton.text = getString(R.string.less_options)
                binding.moreOptionsButton.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    arrowDropUp,
                    null
                )
                taskItem.moreOptionsExpanded = true
            } else {
                binding.moreOptionsLayout!!.visibility = View.GONE
                binding.moreOptionsButton.text = getString(R.string.more_options)
                binding.moreOptionsButton.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    arrowDropDown,
                    null
                )
                taskItem.moreOptionsExpanded = false
            }
        }

        binding.timePickerButton.setOnClickListener {
            openTimePicker()
        }
        binding.datePickerButton.setOnClickListener {
            openDatePicker()
        }

        binding.name.addTextChangedListener { text ->
            updateSaveButtonState(text.toString().trim().isNotEmpty())
            taskItem.name = text.toString()
        }

        binding.desc.addTextChangedListener { text ->
            taskItem.desc = text.toString()
        }
        binding.dismissButton.setOnClickListener {
            dialog.dismiss()
        }

        binding.saveButton.setOnClickListener {
            dismiss()
        }

        updateSaveButtonState(binding.name.text.toString().trim().isNotEmpty())


        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        saveAction()
    }

    private fun openTimePicker() {
        val isSystem24Hour = is24HourFormat(requireContext())
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        val dueTime = if(taskItem.dueTime > 0) taskItem.dueTime else Instant.now().toEpochMilli()
        val cal = Calendar.getInstance()
        cal.timeInMillis = dueTime

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(cal.get(Calendar.HOUR_OF_DAY))
            .setMinute(cal.get(Calendar.MINUTE))
            .setTitleText(getString(R.string.task_due))
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .build()

        picker.addOnPositiveButtonClickListener {
            cal.timeInMillis = 0
            cal.set(Calendar.MINUTE, picker.minute)
            cal.set(Calendar.HOUR_OF_DAY, picker.hour)

            taskItem.dueTime = cal.timeInMillis
            updateTimeButtonText()
        }

        picker.show(childFragmentManager, "TAG")
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance()
        val dueDate = if(taskItem.dueDate > 0) taskItem.dueDate else MaterialDatePicker.todayInUtcMilliseconds()
        cal.timeInMillis = dueDate

        val constraintsBuilder = CalendarConstraints.Builder()
        constraintsBuilder.setValidator(DateValidatorPointForward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(constraintsBuilder.build())
            .setTitleText(getString(R.string.select_date))
            .setSelection(cal.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            cal.timeInMillis = selectedDate
            taskItem.dueDate = cal.timeInMillis
            updateDateButtonText()
        }

        datePicker.show(childFragmentManager, "DATE_PICKER_TAG)")
    }

    private fun updateTimeButtonText() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = taskItem.dueTime

        val timeFormat = DateFormat.getTimeFormat(requireContext())
        val formattedTime = timeFormat.format(cal.time)

        binding.timePickerButton.text = formattedTime
    }

    private fun updateDateButtonText() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = taskItem.dueDate

        val dateFormat = DateFormat.getDateFormat(requireContext())
        val formattedDate = dateFormat.format(cal.time)

        binding.datePickerButton.text = formattedDate
    }

    private fun saveAction() {
        val name = binding.name.text.toString().trim()
        val desc = binding.desc.text.toString().trim()

        if (name.isNotEmpty() || !newTask) {
            taskItem.name = name
            taskItem.desc = desc

            if (!newTask) {
                taskItemViewModel.update(taskItem)
            } else {
                taskItemViewModel.add(taskItem)
            }
        }
    }
}