package org.andrew.jpad.ui.components.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.andrew.jpad.core.completion.CompletionResult
import org.andrew.jpad.core.completion.CompletionService
import org.andrew.jpad.core.completion.LookupItem
import org.andrew.jpad.ui.components.editor.completion.JPadLookupPopup
import org.andrew.jpad.ui.theme.JPadTheme

import org.andrew.jpad.core.completion.SqlCompletionService
import org.andrew.jpad.core.database.DatabaseTableMeta
import org.andrew.jpad.ui.components.ScriptLanguageMode

fun calculateCursorPosition(text: String, selectionOffset: Int): Pair<Int, Int> {
    val safeOffset = selectionOffset.coerceIn(0, text.length)
    val textBeforeCursor = text.substring(0, safeOffset)
    val line = textBeforeCursor.count { it == '\n' } + 1
    val lastNewline = textBeforeCursor.lastIndexOf('\n')
    val col = if (lastNewline == -1) safeOffset + 1 else safeOffset - lastNewline
    return Pair(line, col)
}

fun calculateSelectionOffset(text: String, line: Int, col: Int): Int {
    if (text.isEmpty() || (line <= 1 && col <= 1)) return 0
    val lines = text.split('\n')
    val targetLineIdx = (line - 1).coerceIn(0, lines.size - 1)
    var offset = 0
    for (i in 0 until targetLineIdx) {
        offset += lines[i].length + 1
    }
    val targetColIdx = (col - 1).coerceIn(0, lines[targetLineIdx].length)
    return (offset + targetColIdx).coerceIn(0, text.length)
}

@Composable
fun JPadCodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    onCursorPositionChange: ((line: Int, col: Int) -> Unit)? = null,
    targetCursorPosition: Pair<Int, Int>? = null,
    completionService: CompletionService? = null,
    sqlCompletionService: SqlCompletionService? = null,
    languageMode: ScriptLanguageMode = ScriptLanguageMode.JAVA_STATEMENTS,
    activeDatabaseSchema: List<DatabaseTableMeta> = emptyList(),
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    // Sync external changes to internal TextFieldValue
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            val newSelection = textFieldValue.selection.coerceIn(0, value.length)
            textFieldValue = textFieldValue.copy(text = value, selection = newSelection)
        }
    }

    // Move cursor to target position when requested (e.g. from clicking a diagnostic in Problems panel)
    LaunchedEffect(targetCursorPosition) {
        targetCursorPosition?.let { (line, col) ->
            val offset = calculateSelectionOffset(value, line, col)
            textFieldValue = textFieldValue.copy(selection = TextRange(offset))
            onCursorPositionChange?.invoke(line, col)
        }
    }

    JPadCodeEditor(
        textFieldValue = textFieldValue,
        onTextFieldValueChange = { newValue ->
            textFieldValue = newValue
            if (newValue.text != value) {
                onValueChange(newValue.text)
            }
            if (onCursorPositionChange != null) {
                val (ln, col) = calculateCursorPosition(newValue.text, newValue.selection.start)
                onCursorPositionChange(ln, col)
            }
        },
        completionService = completionService,
        sqlCompletionService = sqlCompletionService,
        languageMode = languageMode,
        activeDatabaseSchema = activeDatabaseSchema,
        onExternalValueCommit = onValueChange,
        onCursorMove = onCursorPositionChange,
        modifier = modifier,
        readOnly = readOnly
    )
}

@Composable
fun JPadCodeEditor(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    completionService: CompletionService? = null,
    sqlCompletionService: SqlCompletionService? = null,
    languageMode: ScriptLanguageMode = ScriptLanguageMode.JAVA_STATEMENTS,
    activeDatabaseSchema: List<DatabaseTableMeta> = emptyList(),
    onExternalValueCommit: ((String) -> Unit)? = null,
    onCursorMove: ((line: Int, col: Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    val colors = JPadTheme.colors
    val lineCount = remember(textFieldValue.text) {
        textFieldValue.text.count { it == '\n' } + 1
    }
    val gutterWidth = remember(lineCount) {
        val digits = maxOf(2, lineCount.toString().length)
        (digits * 9 + 20).dp
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val visualTransformation = remember(colors, languageMode) {
        if (languageMode == ScriptLanguageMode.SQL) {
            SqlVisualTransformation(colors)
        } else {
            JavaVisualTransformation(colors)
        }
    }

    var completionResult by remember { mutableStateOf<CompletionResult?>(null) }
    var selectedCompletionIndex by remember { mutableStateOf(0) }

    fun applyCompletion(item: LookupItem, anchor: Int) {
        val currentText = textFieldValue.text
        val cursor = textFieldValue.selection.start
        val prefixBefore = currentText.substring(0, anchor)
        val textAfter = if (cursor <= currentText.length) currentText.substring(cursor) else ""
        val newText = prefixBefore + item.insertText + textAfter
        val newCursorOffset = (anchor + item.insertText.length + item.cursorDelta).coerceIn(0, newText.length)

        val newSelection = TextRange(newCursorOffset)
        val updatedValue = TextFieldValue(text = newText, selection = newSelection)
        onTextFieldValueChange(updatedValue)
        onExternalValueCommit?.invoke(newText)
        completionResult = null
        val (ln, col) = calculateCursorPosition(newText, newCursorOffset)
        onCursorMove?.invoke(ln, col)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundEditor)
                .verticalScroll(verticalScrollState)
        ) {
            // Line numbers Gutter
            Column(
                modifier = Modifier
                    .width(gutterWidth)
                    .background(colors.backgroundSidebar)
                    .padding(top = 8.dp, bottom = 8.dp, end = 6.dp)
            ) {
                for (lineNum in 1..lineCount) {
                    Text(
                        text = lineNum.toString(),
                        style = JPadTheme.typography.code.copy(
                            color = colors.textMuted,
                            textAlign = TextAlign.End
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Gutter Border
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(colors.borderSubtle)
            )

            // Code Editor Input Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .horizontalScroll(horizontalScrollState)
            ) {
                fun requestCompletions(text: String, cursor: Int): CompletionResult? {
                    if (readOnly) return null
                    return if (languageMode == ScriptLanguageMode.SQL) {
                        sqlCompletionService?.getCompletionsForSchema(text, cursor, activeDatabaseSchema)
                            ?: completionService?.getCompletions(text, cursor)
                    } else {
                        completionService?.getCompletions(text, cursor)
                    }
                }

                val hasCompletion = (languageMode == ScriptLanguageMode.SQL && (sqlCompletionService != null || completionService != null)) || completionService != null

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        onTextFieldValueChange(newValue)

                        // Auto-completion trigger check on typing
                        if (hasCompletion && !readOnly) {
                            val cursor = newValue.selection.start
                            if (cursor > 0 && cursor <= newValue.text.length) {
                                val lastChar = newValue.text[cursor - 1]
                                if (lastChar.isLetterOrDigit() || lastChar == '.' || lastChar == '@' || lastChar == '_') {
                                    val res = requestCompletions(newValue.text, cursor)
                                    if (res != null && res.items.isNotEmpty()) {
                                        completionResult = res
                                        selectedCompletionIndex = 0
                                    } else {
                                        completionResult = null
                                    }
                                } else {
                                    completionResult = null
                                }
                            } else {
                                completionResult = null
                            }
                        }
                    },
                    textStyle = JPadTheme.typography.code.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accentPrimary),
                    visualTransformation = visualTransformation,
                    readOnly = readOnly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            val isCtrlOrMeta = event.isCtrlPressed || event.isMetaPressed

                            // Manual completion trigger on Ctrl+Space
                            if (isCtrlOrMeta && event.key == Key.Spacebar) {
                                if (hasCompletion && !readOnly) {
                                    val cursor = textFieldValue.selection.start
                                    val res = requestCompletions(textFieldValue.text, cursor)
                                    if (res != null && res.items.isNotEmpty()) {
                                        completionResult = res
                                        selectedCompletionIndex = 0
                                        return@onPreviewKeyEvent true
                                    }
                                }
                                return@onPreviewKeyEvent false
                            }

                            // Completion Popup Navigation
                            val cur = completionResult
                            if (cur != null && cur.items.isNotEmpty()) {
                                when (event.key) {
                                    Key.DirectionDown -> {
                                        selectedCompletionIndex = (selectedCompletionIndex + 1) % cur.items.size
                                        return@onPreviewKeyEvent true
                                    }
                                    Key.DirectionUp -> {
                                        selectedCompletionIndex = (selectedCompletionIndex - 1 + cur.items.size) % cur.items.size
                                        return@onPreviewKeyEvent true
                                    }
                                    Key.Enter, Key.Tab -> {
                                        val chosen = cur.items.getOrNull(selectedCompletionIndex)
                                        if (chosen != null) {
                                            applyCompletion(chosen, cur.anchor)
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                    Key.Escape -> {
                                        completionResult = null
                                        return@onPreviewKeyEvent true
                                    }
                                }
                            }
                            false
                        }
                )
            }
        }

        // Floating IntelliJ-style Completion Popup
        completionResult?.let { res ->
            if (res.items.isNotEmpty()) {
                val (cursorLine, cursorCol) = calculateCursorPosition(textFieldValue.text, textFieldValue.selection.start)
                val charWidth = 7.8f
                val lineHeight = 18f
                val xOffset = (gutterWidth.value + (cursorCol - 1) * charWidth - horizontalScrollState.value).coerceIn(gutterWidth.value, 600f)
                val yOffset = (cursorLine * lineHeight - verticalScrollState.value + 14f).coerceIn(0f, 800f)

                Box(
                    modifier = Modifier.padding(start = xOffset.dp, top = yOffset.dp)
                ) {
                    JPadLookupPopup(
                        items = res.items,
                        selectedIndex = selectedCompletionIndex,
                        onItemSelected = { item ->
                            applyCompletion(item, res.anchor)
                        }
                    )
                }
            }
        }
    }
}

private fun TextRange.coerceIn(min: Int, max: Int): TextRange {
    val start = this.start.coerceIn(min, max)
    val end = this.end.coerceIn(min, max)
    return TextRange(start, end)
}

@Preview
@Composable
private fun JPadCodeEditorPreview() {
    val sampleJavaCode = """
        // Sample JPad Java Execution
        import static org.andrew.jpad.runtime.JPad.dump;
        import java.util.List;

        public class OrderAnalysis {
            public static void main(String[] args) {
                var items = List.of(
                    new OrderItem(1, "ThinkPad X1 Carbon", 1899.99, true),
                    new OrderItem(2, "Mechanical Keyboard", 149.50, false)
                );

                dump(items, "Order List");
            }
        }
    """.trimIndent()

    var code by remember { mutableStateOf(sampleJavaCode) }
    var cursor by remember { mutableStateOf(Pair(1, 1)) }

    JPadTheme {
        Box(
            modifier = Modifier
                .width(600.dp)
                .fillMaxHeight()
        ) {
            JPadCodeEditor(
                value = code,
                onValueChange = { code = it },
                onCursorPositionChange = { ln, col -> cursor = Pair(ln, col) }
            )
        }
    }
}
