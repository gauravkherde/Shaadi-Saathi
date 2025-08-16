package com.gaurav.shaadisaathi.models

data class WeddingTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val priority: String = "Medium", // High, Medium, Low
    val isCompleted: Boolean = false,
    val dueDate: Long = 0L,
    val completedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getPriorityDisplayName(): String = priority.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }

    fun getCategoryDisplayName(): String = category.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }

    fun isOverdue(): Boolean = !isCompleted && dueDate < System.currentTimeMillis()

    fun getDaysUntilDue(): Int {
        val diff = dueDate - System.currentTimeMillis()
        return (diff / (24 * 60 * 60 * 1000)).toInt()
    }
}
