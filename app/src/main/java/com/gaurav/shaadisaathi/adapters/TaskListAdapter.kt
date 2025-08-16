package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ItemTaskBinding
import com.gaurav.shaadisaathi.models.WeddingTask
import java.text.SimpleDateFormat
import java.util.*

class TaskListAdapter(
    private val tasks: MutableList<WeddingTask>,
    private val onTaskToggle: (WeddingTask, Boolean) -> Unit,
    private val onTaskClick: (WeddingTask) -> Unit
) : RecyclerView.Adapter<TaskListAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<WeddingTask>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: WeddingTask) {
            binding.apply {
                tvTaskTitle.text = task.title
                tvTaskDescription.text = task.description
                tvTaskCategory.text = task.category

                // Priority indicator
                tvTaskPriority.text = task.priority
                val priorityColor = when (task.priority.lowercase()) {
                    "high" -> R.color.status_declined
                    "medium" -> R.color.status_pending
                    "low" -> R.color.status_confirmed
                    else -> R.color.colorSecondary
                }
                tvTaskPriority.setTextColor(ContextCompat.getColor(itemView.context, priorityColor))

                // Due date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvTaskDueDate.text = "Due: ${dateFormat.format(Date(task.dueDate))}"

                // Completion status
                checkboxTaskComplete.isChecked = task.isCompleted
                checkboxTaskComplete.setOnCheckedChangeListener { _, isChecked ->
                    onTaskToggle(task, isChecked)
                }

                // Completed at display
                if (task.isCompleted && task.completedAt > 0) {
                    tvTaskCompletedAt.visibility = View.VISIBLE
                    tvTaskCompletedAt.text = "Completed: ${dateFormat.format(Date(task.completedAt))}"
                } else {
                    tvTaskCompletedAt.visibility = View.GONE
                }

                // Task status styling
                if (task.isCompleted) {
                    tvTaskTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorSecondary))
                    tvTaskDescription.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorSecondary))
                    cardTask.alpha = 0.7f
                } else {
                    tvTaskTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorPrimary))
                    tvTaskDescription.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorSecondary))
                    cardTask.alpha = 1.0f
                }

                // Overdue indicator
                val isOverdue = !task.isCompleted && task.dueDate < System.currentTimeMillis()
                if (isOverdue) {
                    tvTaskDueDate.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_declined))
                    tvTaskDueDate.text = "Overdue: ${dateFormat.format(Date(task.dueDate))}"
                } else {
                    tvTaskDueDate.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorSecondary))
                }

                // Click listener
                root.setOnClickListener {
                    onTaskClick(task)
                }
            }
        }
    }
}
