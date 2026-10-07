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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.andrew.jpad.ui.theme.JPadTheme

fun calculateCursorPosition(text: String, selectionOffset: Int): Pair<Int, Int> {
    val safeOffset = selectionOffset.coerceIn(0, text.length)
    val textBeforeCursor = text.substring(0, safeOffset)
    val line = textBeforeCursor.count { it == '\n' } + 1
    val lastNewline = textBeforeCursor.lastIndexOf('\n')
    val col = if (lastNewline == -1) safeOffset + 1 else safeOffset - lastNewline
    return Pair(line, col)
}

@Composable
fun JPadCodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    onCursorPositionChange: ((line: Int, col: Int) -> Unit)? = null,
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
        modifier = modifier,
        readOnly = readOnly
    )
}

@Composable
fun JPadCodeEditor(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
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
    val visualTransformation = remember(colors) { JavaVisualTransformation(colors) }

    Row(
        modifier = modifier
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
            BasicTextField(
                value = textFieldValue,
                onValueChange = onTextFieldValueChange,
                textStyle = JPadTheme.typography.code.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accentPrimary),
                visualTransformation = visualTransformation,
                readOnly = readOnly,
                modifier = Modifier.fillMaxWidth()
            )
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
