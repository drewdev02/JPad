package org.andrew.jpad.ui.components.palette

enum class CommandCategory(val title: String, val icon: String) {
    ALL("All", "✨"),
    EXECUTION("Execution", "⚡"),
    FILE("Files & Tabs", "📁"),
    LANGUAGE("Language", "⚙️"),
    VIEW("Panels", "👁️"),
    DATABASE("Database", "🗄️"),
    HISTORY("History", "🕒")
}

data class CommandPaletteAction(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val category: CommandCategory,
    val shortcut: String? = null,
    val icon: String? = null,
    val onExecute: () -> Unit
)

object CommandPaletteSearch {
    fun filter(actions: List<CommandPaletteAction>, query: String, selectedCategory: CommandCategory = CommandCategory.ALL): List<CommandPaletteAction> {
        val categoryFiltered = if (selectedCategory == CommandCategory.ALL) {
            actions
        } else {
            actions.filter { it.category == selectedCategory }
        }

        if (query.isBlank()) return categoryFiltered

        val terms = query.trim().lowercase().split("\\s+".toRegex())

        return categoryFiltered.filter { action ->
            val searchHaystack = buildString {
                append(action.title.lowercase())
                append(" ")
                append(action.category.title.lowercase())
                if (action.subtitle != null) {
                    append(" ")
                    append(action.subtitle.lowercase())
                }
                if (action.shortcut != null) {
                    append(" ")
                    append(action.shortcut.lowercase())
                }
            }

            terms.all { term -> searchHaystack.contains(term) }
        }.sortedByDescending { action ->
            // Prioritize title startsWith > title contains > other
            val term = terms.firstOrNull() ?: ""
            when {
                action.title.startsWith(term, ignoreCase = true) -> 3
                action.title.contains(term, ignoreCase = true) -> 2
                else -> 1
            }
        }
    }
}
