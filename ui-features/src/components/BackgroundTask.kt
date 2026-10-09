package org.andrew.jpad.ui.components

/**
 * Represents an active background task (e.g. Maven dependency downloading, compilation, indexing).
 *
 * @property id Unique identifier for the task.
 * @property title Human-readable task name or current step (e.g. "Downloading gson-2.10.1.jar").
 * @property details Optional supplemental text or subtitle.
 * @property progress Null for indeterminate tasks, or 0.0f..1.0f for determinate progress.
 */
data class BackgroundTask(
    val id: String,
    val title: String,
    val details: String? = null,
    val progress: Float? = null
) {
    val isIndeterminate: Boolean get() = progress == null
}
