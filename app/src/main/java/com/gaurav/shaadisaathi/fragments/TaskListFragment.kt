package com.gaurav.shaadisaathi.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaurav.shaadisaathi.adapters.TaskListAdapter
import com.gaurav.shaadisaathi.databinding.FragmentTaskListBinding
import com.gaurav.shaadisaathi.models.WeddingTask
import kotlinx.coroutines.launch

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskListAdapter
    private val taskList = mutableListOf<WeddingTask>()
    private val allTasks = mutableListOf<WeddingTask>()
    private var currentFilter = "all"
    private val TAG = "TaskListFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadDefaultTasks()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskListAdapter(
            tasks = taskList,
            onTaskToggle = { task: WeddingTask, isCompleted: Boolean ->
                toggleTaskCompletion(task, isCompleted)
            },
            onTaskClick = { task: WeddingTask ->
                showTaskDetails(task)
            }
        )

        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddTask?.setOnClickListener {
            showAddTaskDialog()
        }

        binding.swipeRefreshLayout?.setOnRefreshListener {
            loadDefaultTasks()
        }

        binding.chipFilterAll?.setOnClickListener {
            filterTasks("all")
        }

        binding.chipFilterPending?.setOnClickListener {
            filterTasks("pending")
        }

        binding.chipFilterCompleted?.setOnClickListener {
            filterTasks("completed")
        }
    }

    private fun loadDefaultTasks() {
        binding.progressBar?.visibility = View.VISIBLE
        binding.swipeRefreshLayout?.isRefreshing = true

        lifecycleScope.launch {
            try {
                // Load default wedding planning tasks
                val defaultTasks = getDefaultWeddingTasks()
                allTasks.clear()
                allTasks.addAll(defaultTasks)

                // Apply current filter
                applyCurrentFilter()

                updateEmptyState(taskList.isEmpty())
                updateTaskStatistics()

                Toast.makeText(requireContext(), "Tasks loaded successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading tasks: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar?.visibility = View.GONE
                binding.swipeRefreshLayout?.isRefreshing = false
            }
        }
    }

    private fun getDefaultWeddingTasks(): List<WeddingTask> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            WeddingTask(
                id = "1",
                title = "Book Wedding Venue",
                description = "Research and book the perfect venue for the wedding ceremony and reception",
                category = "Venue",
                priority = "High",
                isCompleted = false,
                dueDate = currentTime + (30 * 24 * 60 * 60 * 1000L), // 30 days
                createdAt = currentTime
            ),
            WeddingTask(
                id = "2",
                title = "Send Save the Dates",
                description = "Design and send save the date cards to all guests",
                category = "Invitations",
                priority = "Medium",
                isCompleted = false,
                dueDate = currentTime + (45 * 24 * 60 * 60 * 1000L), // 45 days
                createdAt = currentTime
            ),
            WeddingTask(
                id = "3",
                title = "Book Photographer",
                description = "Find and book a professional wedding photographer and videographer",
                category = "Photography",
                priority = "High",
                isCompleted = false,
                dueDate = currentTime + (20 * 24 * 60 * 60 * 1000L), // 20 days
                createdAt = currentTime
            ),
            WeddingTask(
                id = "4",
                title = "Choose Wedding Dress",
                description = "Shop for and select the perfect wedding dress and accessories",
                category = "Attire",
                priority = "High",
                isCompleted = false,
                dueDate = currentTime + (60 * 24 * 60 * 60 * 1000L), // 60 days
                createdAt = currentTime
            ),
            WeddingTask(
                id = "5",
                title = "Plan Menu with Caterer",
                description = "Finalize menu options, dietary requirements, and catering arrangements",
                category = "Catering",
                priority = "Medium",
                isCompleted = false,
                dueDate = currentTime + (40 * 24 * 60 * 60 * 1000L), // 40 days
                createdAt = currentTime
            ),
            WeddingTask(
                id = "6",
                title = "Book DJ/Band",
                description = "Hire entertainment for the wedding ceremony and reception",
                category = "Entertainment",
                priority = "Medium",
                isCompleted = false,
                dueDate = currentTime + (35 * 24 * 60 * 60 * 1000L), // 35 days
                createdAt = currentTime
            ),
            WeddingTask(
                id = "7",
                title = "Order Wedding Cake",
                description = "Design and order the perfect wedding cake for the celebration",
                category = "Catering",
                priority = "Medium",
                isCompleted = false,
                dueDate = currentTime + (25 * 24 * 60 * 60 * 1000L), // 25 days
                createdAt = currentTime
            ),
            WeddingTask(
                id = "8",
                title = "Book Transportation",
                description = "Arrange transportation for the wedding party and guests",
                category = "Transportation",
                priority = "Low",
                isCompleted = false,
                dueDate = currentTime + (15 * 24 * 60 * 60 * 1000L), // 15 days
                createdAt = currentTime
            ),
            WeddingTask(
                id = "9",
                title = "Purchase Wedding Rings",
                description = "Select and purchase wedding bands for the ceremony",
                category = "Jewelry",
                priority = "High",
                isCompleted = false,
                dueDate = currentTime + (50 * 24 * 60 * 60 * 1000L), // 50 days
                createdAt = currentTime
            ),
            WeddingTask(
                id = "10",
                title = "Book Honeymoon",
                description = "Plan and book honeymoon destination and accommodations",
                category = "Honeymoon",
                priority = "Low",
                isCompleted = false,
                dueDate = currentTime + (90 * 24 * 60 * 60 * 1000L), // 90 days
                createdAt = currentTime
            ),
            WeddingTask(
                id = "11",
                title = "Send Wedding Invitations",
                description = "Design, print, and send formal wedding invitations to all guests",
                category = "Invitations",
                priority = "Medium",
                isCompleted = false,
                dueDate = currentTime + (21 * 24 * 60 * 60 * 1000L), // 21 days
                createdAt = currentTime
            ),
            WeddingTask(
                id = "12",
                title = "Book Makeup Artist",
                description = "Hire professional makeup artist for the wedding day",
                category = "Beauty",
                priority = "Medium",
                isCompleted = false,
                dueDate = currentTime + (30 * 24 * 60 * 60 * 1000L), // 30 days
                createdAt = currentTime
            )
        )
    }

    private fun filterTasks(filter: String) {
        currentFilter = filter
        applyCurrentFilter()
        updateFilterChips(filter)
    }

    private fun applyCurrentFilter() {
        val filteredTasks = when (currentFilter) {
            "pending" -> allTasks.filter { !it.isCompleted }
            "completed" -> allTasks.filter { it.isCompleted }
            else -> allTasks
        }

        taskList.clear()
        taskList.addAll(filteredTasks.sortedWith(compareBy<WeddingTask> { it.isCompleted }.thenBy { it.dueDate }))
        taskAdapter.notifyDataSetChanged()

        updateEmptyState(taskList.isEmpty())
    }

    private fun updateFilterChips(selectedFilter: String) {
        binding.chipFilterAll?.isChecked = selectedFilter == "all"
        binding.chipFilterPending?.isChecked = selectedFilter == "pending"
        binding.chipFilterCompleted?.isChecked = selectedFilter == "completed"
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState?.visibility = View.VISIBLE
            binding.recyclerViewTasks.visibility = View.GONE
            binding.layoutTaskStats?.visibility = View.GONE
        } else {
            binding.layoutEmptyState?.visibility = View.GONE
            binding.recyclerViewTasks.visibility = View.VISIBLE
            binding.layoutTaskStats?.visibility = View.VISIBLE
        }
    }

    private fun updateTaskStatistics() {
        val totalTasks = allTasks.size
        val completedTasks = allTasks.count { it.isCompleted }
        val pendingTasks = totalTasks - completedTasks
        val completionRate = if (totalTasks > 0) (completedTasks / totalTasks.toFloat() * 100).toInt() else 0

        binding.tvTotalTasks?.text = totalTasks.toString()
        binding.tvCompletedTasks?.text = completedTasks.toString()
        binding.tvPendingTasks?.text = pendingTasks.toString()
        binding.tvCompletionRate?.text = "$completionRate%"

        binding.progressTaskCompletion?.progress = completionRate

        // Update progress bar color based on completion rate
        val progressColor = when {
            completionRate >= 80 -> android.R.color.holo_green_dark
            completionRate >= 50 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }
        binding.progressTaskCompletion?.progressTintList =
            androidx.core.content.ContextCompat.getColorStateList(requireContext(), progressColor)
    }

    private fun toggleTaskCompletion(task: WeddingTask, isCompleted: Boolean) {
        val index = allTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            allTasks[index] = task.copy(
                isCompleted = isCompleted,
                completedAt = if (isCompleted) System.currentTimeMillis() else 0L,
                updatedAt = System.currentTimeMillis()
            )

            // Apply current filter to refresh the display
            applyCurrentFilter()
            updateTaskStatistics()

            val message = if (isCompleted) {
                "✅ Task completed! Great progress!"
            } else {
                "Task marked as pending"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

            // Show celebration for milestone completion
            if (isCompleted) {
                checkMilestones()
            }
        }
    }

    private fun checkMilestones() {
        val completedTasks = allTasks.count { it.isCompleted }
        val totalTasks = allTasks.size
        val completionRate = (completedTasks / totalTasks.toFloat() * 100).toInt()

        when (completionRate) {
            25 -> showMilestoneMessage("🎉 Quarter way there! Keep it up!")
            50 -> showMilestoneMessage("🎊 Halfway done! You're doing amazing!")
            75 -> showMilestoneMessage("🌟 Three-quarters complete! Almost there!")
            100 -> showMilestoneMessage("🎆 Congratulations! All tasks completed! 🎆")
        }
    }

    private fun showMilestoneMessage(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Milestone Reached!")
            .setMessage(message)
            .setPositiveButton("Awesome!") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showTaskDetails(task: WeddingTask) {
        val dueDate = java.text.SimpleDateFormat("EEEE, MMM dd, yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(task.dueDate))

        val daysUntilDue = task.getDaysUntilDue()
        val dueDateText = when {
            task.isCompleted -> "Completed ✅"
            daysUntilDue < 0 -> "Overdue by ${-daysUntilDue} days ⚠️"
            daysUntilDue == 0 -> "Due today! 🔥"
            daysUntilDue <= 7 -> "Due in $daysUntilDue days ⏰"
            else -> "Due: $dueDate"
        }

        val completedText = if (task.isCompleted && task.completedAt > 0) {
            val completedDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(task.completedAt))
            "\n\nCompleted on: $completedDate"
        } else ""

        val message = """
            ${task.description}
            
            Category: ${task.getCategoryDisplayName()}
            Priority: ${task.getPriorityDisplayName()}
            $dueDateText$completedText
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(task.title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Edit") { _, _ ->
                showEditTaskDialog(task)
            }
            .setNegativeButton("Delete") { _, _ ->
                showDeleteConfirmation(task)
            }
            .show()
    }

    private fun showEditTaskDialog(task: WeddingTask) {
        val input = android.widget.EditText(requireContext())
        input.setText(task.title)
        input.hint = "Enter task title"

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Task")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    updateTask(task, newTitle)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTask(task: WeddingTask, newTitle: String) {
        val index = allTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            allTasks[index] = task.copy(
                title = newTitle,
                updatedAt = System.currentTimeMillis()
            )
            applyCurrentFilter()
            Toast.makeText(requireContext(), "Task updated successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation(task: WeddingTask) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete '${task.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(task: WeddingTask) {
        allTasks.removeAll { it.id == task.id }
        applyCurrentFilter()
        updateTaskStatistics()
        Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
    }

    private fun showAddTaskDialog() {
        val dialogView = layoutInflater.inflate(com.gaurav.shaadisaathi.R.layout.dialog_add_task, null)
        val etTitle = dialogView.findViewById<android.widget.EditText>(com.gaurav.shaadisaathi.R.id.etTaskTitle)
        val etDescription = dialogView.findViewById<android.widget.EditText>(com.gaurav.shaadisaathi.R.id.etTaskDescription)
        val spinnerCategory = dialogView.findViewById<android.widget.Spinner>(com.gaurav.shaadisaathi.R.id.spinnerCategory)
        val spinnerPriority = dialogView.findViewById<android.widget.Spinner>(com.gaurav.shaadisaathi.R.id.spinnerPriority)

        // Setup spinners
        val categories = arrayOf("Custom", "Venue", "Invitations", "Photography", "Attire", "Catering", "Entertainment", "Transportation", "Jewelry", "Beauty", "Honeymoon")
        val priorities = arrayOf("Low", "Medium", "High")

        val categoryAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        val priorityAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = priorityAdapter
        spinnerPriority.setSelection(1) // Default to Medium

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add New Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val category = spinnerCategory.selectedItem.toString()
                val priority = spinnerPriority.selectedItem.toString()

                if (title.isNotEmpty()) {
                    addNewTask(title, description, category, priority)
                } else {
                    Toast.makeText(requireContext(), "Please enter a task title", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNewTask(title: String, description: String, category: String, priority: String) {
        val newTask = WeddingTask(
            id = System.currentTimeMillis().toString(),
            title = title,
            description = description.ifEmpty { "Custom task added by user" },
            category = category,
            priority = priority,
            isCompleted = false,
            dueDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L), // 7 days default
            createdAt = System.currentTimeMillis()
        )

        allTasks.add(newTask)
        applyCurrentFilter()
        updateTaskStatistics()

        Toast.makeText(requireContext(), "Task '${title}' added successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showTaskFilters() {
        val filterOptions = arrayOf("All Tasks", "High Priority", "Medium Priority", "Low Priority", "Overdue Tasks", "Due This Week")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Filter Tasks")
            .setItems(filterOptions) { _, which ->
                when (which) {
                    0 -> filterTasks("all")
                    1 -> filterByPriority("High")
                    2 -> filterByPriority("Medium")
                    3 -> filterByPriority("Low")
                    4 -> filterOverdue()
                    5 -> filterDueThisWeek()
                }
            }
            .show()
    }

    private fun filterByPriority(priority: String) {
        val filteredTasks = allTasks.filter { it.priority == priority }
        taskList.clear()
        taskList.addAll(filteredTasks)
        taskAdapter.notifyDataSetChanged()
        updateEmptyState(taskList.isEmpty())
        Toast.makeText(requireContext(), "Showing $priority priority tasks", Toast.LENGTH_SHORT).show()
    }

    private fun filterOverdue() {
        val overdueTask = allTasks.filter { it.isOverdue() }
        taskList.clear()
        taskList.addAll(overdueTask)
        taskAdapter.notifyDataSetChanged()
        updateEmptyState(taskList.isEmpty())
        Toast.makeText(requireContext(), "Showing overdue tasks", Toast.LENGTH_SHORT).show()
    }

    private fun filterDueThisWeek() {
        val oneWeekFromNow = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
        val dueThisWeek = allTasks.filter {
            !it.isCompleted && it.dueDate <= oneWeekFromNow && it.dueDate >= System.currentTimeMillis()
        }
        taskList.clear()
        taskList.addAll(dueThisWeek)
        taskAdapter.notifyDataSetChanged()
        updateEmptyState(taskList.isEmpty())
        Toast.makeText(requireContext(), "Showing tasks due this week", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): TaskListFragment {
            return TaskListFragment()
        }
    }
}
