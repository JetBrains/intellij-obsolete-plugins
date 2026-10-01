package com.intellij.aidebugger.evaluation.views

import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Container
import java.awt.FontMetrics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.event.ChangeEvent
import javax.swing.event.ListSelectionEvent
import javax.swing.event.TableColumnModelEvent
import javax.swing.event.TableColumnModelListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer
import kotlin.math.max
import kotlin.math.min

internal fun findActionToolbarIn(component: Component): ActionToolbarImpl? {
    if (component is ActionToolbarImpl) return component
    if (component is Container) {
        for (child in component.components) {
            val found = findActionToolbarIn(child)
            if (found != null) return found
        }
    }
    return null
}

// Adjust table auto-resize mode based on whether columns fit in viewport
internal fun adjustTableAutoResizeMode(table: JTable, scrollPane: JScrollPane) {
    try {
        val cm = table.columnModel ?: return
        val viewportWidth = scrollPane.viewport?.width ?: scrollPane.width

        // Calculate total preferred width of all columns
        var totalPreferredWidth = 0
        for (i in 0 until cm.columnCount) {
            totalPreferredWidth += cm.getColumn(i).preferredWidth
        }

        // Add some margin for intercell spacing
        totalPreferredWidth += table.intercellSpacing.width * (cm.columnCount + 1)

        // If columns fit comfortably in viewport (with 95% threshold), use auto-resize
        // Otherwise use no auto-resize to enable horizontal scrolling
        if (totalPreferredWidth <= viewportWidth * 0.95) {
            table.autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
        } else {
            table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        }
    } catch (_: Throwable) { /* ignore */ }
}

// Auto-size the evaluation results table columns based on content, keeping ID narrow
internal fun adjustEvalResultColumnWidths(table: JTable) {
    try {
        val cm = table.columnModel ?: return
        val header = table.tableHeader
        val rowCount = table.rowCount
        val colCount = cm.columnCount
        if (colCount == 0) return
        val sampleRows = min(rowCount, 300)
        val margin = 16

        for (colIdx in 0 until colCount) {
            val column = cm.getColumn(colIdx)
            val colName = try { table.model.getColumnName(colIdx) } catch (_: Throwable) { "" }

            // Start with header width
            var headerWidth = try {
                val hdrR = header?.defaultRenderer
                val hdrComp = hdrR?.getTableCellRendererComponent(table, column.headerValue, false, false, -1, colIdx)
                hdrComp?.preferredSize?.width ?: 0
            } catch (_: Throwable) { 0 }
            headerWidth += margin

            // Calculate content width - sample cells to find max single-line width
            var maxContentWidth = 0
            var hasLongContent = false
            var row = 0
            while (row < sampleRows) {
                try {
                    val value = table.getValueAt(row, colIdx)?.toString() ?: ""
                    val singleLineWidth = if (value.isNotEmpty()) {
                        // Measure text width as if it were on a single line
                        val fm = table.getFontMetrics(table.font)
                        // Take first line or first N chars to estimate width
                        val testText = value.lines().firstOrNull() ?: value
                        val truncated = if (testText.length > 200) testText.take(200) else testText
                        fm.stringWidth(truncated) + margin
                    } else 0

                    if (singleLineWidth > maxContentWidth) {
                        maxContentWidth = singleLineWidth
                    }

                    // Check if content is long and would need wrapping
                    if (value.length > 100 || value.lines().size > 1) {
                        hasLongContent = true
                    }
                } catch (_: Throwable) { /* ignore bad row */ }
                row++
            }

            // Determine sizing based on column type and content
            val (minWidth, maxWidth, preferredWidth) = when {
                colIdx == 0 -> { // ID column: keep narrow
                    val min = 40
                    val max = 80
                    val pref = maxOf(min, minOf(maxContentWidth, max))
                    Triple(min, max, pref)
                }
                colName.endsWith(" Score") -> { // Score columns: compact
                    val min = 65
                    val max = 120
                    val pref = maxOf(min, minOf(maxOf(headerWidth, maxContentWidth), max))
                    Triple(min, max, pref)
                }
                colName.endsWith(" Extra") -> { // Extra columns: wider for explanations
                    val min = 180
                    val max = 600
                    // If content is short, use actual width; if long, use reasonable max for wrapping
                    val pref = if (hasLongContent) {
                        minOf(maxContentWidth, max)
                    } else {
                        maxOf(min, minOf(maxContentWidth, max))
                    }
                    Triple(min, max, pref)
                }
                colName == "Input" -> { // Input column: medium width
                    val min = 150
                    val max = 500
                    val pref = if (hasLongContent) {
                        minOf(maxContentWidth, max)
                    } else {
                        maxOf(min, minOf(maxContentWidth, max))
                    }
                    Triple(min, max, pref)
                }
                colName == "Output" -> { // Output column: wider
                    val min = 180
                    val max = 600
                    val pref = if (hasLongContent) {
                        minOf(maxContentWidth, max)
                    } else {
                        maxOf(min, minOf(maxContentWidth, max))
                    }
                    Triple(min, max, pref)
                }
                else -> { // Other columns: default behavior
                    val min = 100
                    val max = 400
                    val pref = maxOf(min, minOf(maxContentWidth, max))
                    Triple(min, max, pref)
                }
            }

            column.minWidth = minWidth
            column.maxWidth = maxWidth
            column.preferredWidth = preferredWidth
        }
        table.doLayout()
    } catch (_: Throwable) { /* ignore and keep defaults */ }
}

// Dynamic column detection: Input (col 1), Output (col 2), and any "Extra" columns
internal fun getTextColumnIndices(table: JTable): IntArray {
    val indices = mutableListOf<Int>()
    val cm = table.columnModel ?: return intArrayOf(1, 2)
    for (i in 0 until cm.columnCount) {
        val colName = try { table.model.getColumnName(i) } catch (_: Throwable) { "" }
        // Input, Output, or any column ending with " Extra"
        if (colName == "Input" || colName == "Output" || colName.endsWith(" Extra")) {
            indices.add(i)
        }
    }
    return indices.toIntArray()
}

internal fun getExtraColumnIndices(table: JTable): IntArray {
    val indices = mutableListOf<Int>()
    val cm = table.columnModel ?: return intArrayOf()
    for (i in 0 until cm.columnCount) {
        val colName = try { table.model.getColumnName(i) } catch (_: Throwable) { "" }
        if (colName.endsWith(" Extra")) {
            indices.add(i)
        }
    }
    return indices.toIntArray()
}

private class MultiLineTableCellRenderer : JTextArea(), TableCellRenderer {
    init {
        lineWrap = true
        wrapStyleWord = true
        isOpaque = true
        border = null
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        @NlsSafe val full = value?.toString() ?: ""
        text = full
        font = table.font
        if (isSelected) {
            background = table.selectionBackground
            foreground = table.selectionForeground
        } else {
            background = table.background
            foreground = table.foreground
        }
        // Multiline tooltip only when content is clipped
        val colWidth = try { table.columnModel.getColumn(column).width } catch (_: Throwable) { table.width }
        val pad = max(0, table.intercellSpacing.width - 1)
        val tipW = (colWidth - pad).coerceIn(180, 900)
        val desiredH = try { measureWrappedHeight(table, row, column, full) } catch (_: Throwable) { table.getRowHeight(row) }
        val fits = desiredH <= table.getRowHeight(row) + 1
        toolTipText = if (fits) null else makeMultilineTooltip(full, tipW)
        setSize(colWidth - pad, Short.MAX_VALUE.toInt())
        return this
    }
}

internal fun enableEvalResultsMultiline(table: JTable, scrollPane: JScrollPane) {
    try {
        // Define a compact baseline row height based on font metrics, not the current row height
        val fm = table.getFontMetrics(table.font)
        val compactBaseline = (fm.height + JBUI.scale(6)).coerceAtLeast(fm.height)
        table.putClientProperty("eval.baseline.rowHeight", compactBaseline)
        table.rowHeight = compactBaseline

        val cm = table.columnModel

        // Apply renderers dynamically based on column names
        for (i in 0 until cm.columnCount) {
            val colName = try { table.model.getColumnName(i) } catch (_: Throwable) { "" }
            when {
                colName == "Input" || colName == "Output" -> {
                    // Input and Output use ellipsizing renderer constrained by row height
                    cm.getColumn(i).cellRenderer = EllipsizingWrapCellRenderer()
                }
                colName.endsWith(" Extra") -> {
                    // Extra columns show full wrapped content (clipped by row height cap)
                    cm.getColumn(i).cellRenderer = MultiLineTableCellRenderer()
                }
            }
        }
    } catch (_: Throwable) { }

    val updateHeights = {
        val textCols = getTextColumnIndices(table)
        updateVisibleRowHeights(table, textCols)
    }

    // Recalculate heights on column width changes
    table.columnModel.addColumnModelListener(object : TableColumnModelListener {
        override fun columnMarginChanged(e: ChangeEvent?) { updateHeights() }
        override fun columnMoved(e: TableColumnModelEvent?) { updateHeights() }
        override fun columnAdded(e: TableColumnModelEvent?) { updateHeights() }
        override fun columnRemoved(e: TableColumnModelEvent?) { updateHeights() }
        override fun columnSelectionChanged(e: ListSelectionEvent?) { }
    })

    // Table resized (e.g., splitter drag)
    table.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) { updateHeights() }
    })

    // Scroll events: update only visible rows on scroll
    scrollPane.verticalScrollBar?.addAdjustmentListener { updateHeights() }
    scrollPane.horizontalScrollBar?.addAdjustmentListener { updateHeights() }

    // Data/model changes
    table.model.addTableModelListener { updateHeights() }

    // Sorting changes
    try { table.rowSorter?.addRowSorterListener { updateHeights() } } catch (_: Throwable) { }
    table.addPropertyChangeListener("rowSorter") { _ ->
        try { table.rowSorter?.addRowSorterListener { updateHeights() } } catch (_: Throwable) { }
        updateHeights()
    }

    // Initial pass
    updateHeights()
}

//internal fun updateEvalResultRowHeights(table: JTable) {
//    val textCols = getTextColumnIndices(table)
//    updateVisibleRowHeights(table, textCols)
//}

internal fun updateVisibleRowHeights(table: JTable, columns: IntArray) {
    val app = ApplicationManager.getApplication()
    val runnable = Runnable {
        val rect = table.visibleRect ?: Rectangle(0, 0, table.width, table.height)
        var first = table.rowAtPoint(rect.location)
        if (first < 0) first = 0
        var last = table.rowAtPoint(Point(rect.x, rect.y + rect.height - 1))
        if (last < 0) last = table.rowCount - 1
        if (last < first) return@Runnable
        for (row in first..last) {
            val wanted = computeRowPreferredHeight(table, row, columns)
            if (wanted > 0 && table.getRowHeight(row) != wanted) {
                table.setRowHeight(row, wanted)
            }
        }
    }
    if (app.isDispatchThread) runnable.run() else app.invokeLater(runnable)
}

internal fun computeRowPreferredHeight(table: JTable, row: Int, columns: IntArray): Int {
    // Unified height rule for evaluation rows:
    // - Look at all "text" columns (Input, Output, any "* Extra").
    // - Compute wrapped height per column for this row.
    // - Take the maximum height among them.
    // - Convert to line count and cap to a small number of lines.
    // - Return height for that capped line count.
    val baseline = (table.getClientProperty("eval.baseline.rowHeight") as? Int) ?: table.rowHeight
    if (columns.isEmpty()) return baseline

    fun isEvalPlaceholder(s: String?): Boolean {
        if (s.isNullOrBlank()) return false
        val t = s.trim()
        return t.equals("evaluating...", ignoreCase = true) ||
                t.equals("evaluating…", ignoreCase = true) ||
                t.equals("running...", ignoreCase = true) ||
                t.equals("running…", ignoreCase = true)
    }

    fun ceilLines(px: Int): Int {
        return max(1, (px + baseline - 1) / baseline)
    }

    // Hard cap for visible lines per row
    val maxLinesPerRow = 4

    var maxHeightPx = baseline

    for (col in columns) {
        if (col !in 0 until table.columnCount) continue
        val text = try {
            table.getValueAt(row, col)?.toString()?.trim()
        } catch (_: Throwable) {
            null
        }

        if (text.isNullOrEmpty() || isEvalPlaceholder(text)) continue

        val h = try {
            measureWrappedHeight(table, row, col, text)
        } catch (_: Throwable) {
            baseline
        }
        if (h > maxHeightPx) {
            maxHeightPx = h
        }
    }

    // If everything was empty/placeholders, keep baseline height
    if (maxHeightPx <= baseline) return baseline

    val lines = ceilLines(maxHeightPx).coerceAtMost(maxLinesPerRow)
    return heightForLines(table, lines)
}

// === Top-left alignment for non-multiline columns in results table ===
private class TopLeftLabelRenderer : DefaultTableCellRenderer() {
    init {
        horizontalAlignment = LEFT
        verticalAlignment = TOP
        isOpaque = true
    }
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        // Ensure alignment stays enforced even if LAF resets it
        horizontalAlignment = LEFT
        verticalAlignment = TOP
        return comp
    }
}

internal fun enableEvalResultsTopLeftAlignment(table: JTable) {
    try {
        val cm = table.columnModel ?: return
        val textCols = getTextColumnIndices(table).toSet()
        for (i in 0 until cm.columnCount) {
            if (!textCols.contains(i)) {
                cm.getColumn(i).cellRenderer = TopLeftLabelRenderer()
            }
        }
    } catch (_: Throwable) { /* ignore */ }
}

// === Ellipsizing wrap renderer for constrained-height columns (Input/Output) ===
private class EllipsizingWrapCellRenderer : JTextArea(), TableCellRenderer {
    init {
        lineWrap = true
        wrapStyleWord = true
        isOpaque = true
        border = null
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        @NlsSafe val full = value?.toString() ?: ""
        font = table.font
        if (isSelected) {
            background = table.selectionBackground
            foreground = table.selectionForeground
        } else {
            background = table.background
            foreground = table.foreground
        }
        val colWidth = try { table.columnModel.getColumn(column).width } catch (_: Throwable) { table.width }
        val pad = max(0, table.intercellSpacing.width - 1)
        val width = (colWidth - pad).coerceAtLeast(10)

        // Determine allowed lines from current row height vs baseline
        val baseline = (table.getClientProperty("eval.baseline.rowHeight") as? Int) ?: table.rowHeight
        val rh = table.getRowHeight(row).coerceAtLeast(baseline)
        // Use ceiling division so slightly-shorter row heights still allow the intended lines
        val maxLines = max(1, (rh + baseline - 1) / baseline)

        val fm = getFontMetrics(font)
        // Decide whether the full text actually fits within the current cell (row height and column width)
        val allowedH = try { heightForLines(table, maxLines) } catch (_: Throwable) { rh }
        val desiredH = try { measureWrappedHeight(table, row, column, full) } catch (_: Throwable) { allowedH }
        val isClipped = desiredH > allowedH + 1

        val clipped = if (isClipped) clipTextToWidthAndLines(full, fm, width, maxLines) else full
        text = clipped

        // Show tooltip only if ellipsized/truncated (content doesn't fit)
        @NlsSafe val tooltip = full.replace("\n", " ").replace("\r", " ")
        toolTipText = if (isClipped) tooltip else null

        setSize(width, Short.MAX_VALUE.toInt())
        return this
    }
}

internal fun clipTextToWidthAndLines(text: String, fm: FontMetrics, width: Int, maxLines: Int): String {
    if (text.isEmpty() || width <= 0 || maxLines <= 0) return text
    val ellipsis = "…"
    val ellW = fm.stringWidth(ellipsis)
    val tokens = tokenizeForWrap(text)

    val lines = mutableListOf<StringBuilder>()
    var current = StringBuilder()

    fun flushLine() {
        lines.add(current)
        current = StringBuilder()
    }

    var i = 0
    while (i < tokens.size) {
        val t = tokens[i]
        if (t == "\n") {
            flushLine()
            i++; continue
        }
        val add = if (current.isEmpty()) t.trimStart() else t
        val trial = current.toString() + add
        val w = fm.stringWidth(trial)
        if (w <= width) {
            current.append(add)
            i++
        } else {
            // If token itself doesn't fit on empty line, hard-cut it
            if (current.isEmpty()) {
                var cut = t
                while (cut.isNotEmpty() && fm.stringWidth(cut) + ellW > width) {
                    cut = cut.dropLast(1)
                }
                current.append(if (cut.isEmpty()) ellipsis else cut + ellipsis)
                i++
            }
            flushLine()
        }
        if (lines.size + 1 > maxLines) break
    }
    // Add last line
    if (lines.size < maxLines) {
        lines.add(current)
    }

    // If tokens remain, ensure we end with ellipsis and fit
    if (i < tokens.size) {
        var last = lines.last().toString()
        // Trim if needed to append ellipsis
        while (last.isNotEmpty() && fm.stringWidth(last) + ellW > width) {
            last = last.dropLast(1)
        }
        last = if (last.isEmpty()) ellipsis else last + ellipsis
        lines[lines.lastIndex] = StringBuilder(last)
    }

    // Cleanup trailing spaces per line
    return lines.joinToString("\n") { it.toString().trimEnd() }
}

internal fun tokenizeForWrap(text: String): List<String> {
    val list = mutableListOf<String>()
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        if (ch == '\n') {
            list.add("\n"); i++; continue
        }
        if (ch.isWhitespace() && ch != '\r') {
            // group consecutive spaces/tabs as one token
            var j = i
            while (j < text.length && text[j].isWhitespace() && text[j] != '\n' && text[j] != '\r') j++
            list.add(text.substring(i, j))
            i = j; continue
        }
        // word token (non-whitespace)
        var j = i
        while (j < text.length) {
            val c = text[j]
            if (c.isWhitespace() || c == '\n' || c == '\r') break
            j++
        }
        list.add(text.substring(i, j))
        i = j
    }
    return list
}

internal fun measureWrappedHeight(table: JTable, row: Int, column: Int, text: String): Int {
    val cm = table.columnModel ?: return table.rowHeight
    val rawWidth = try { cm.getColumn(column).width } catch (_: Throwable) { 200 }
    // Use the same effective width as renderers to avoid reflow/jitter on hover
    val pad = max(0, table.intercellSpacing.width - 1)
    val width = (rawWidth - pad).coerceAtLeast(10)
    val area = JTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        font = table.font
        this.text = text
        setSize(width, Short.MAX_VALUE.toInt())
    }
    val spacing = table.intercellSpacing.height
    return area.preferredSize.height + spacing
}

internal fun heightForLines(table: JTable, lines: Int): Int {
    val baseline = (table.getClientProperty("eval.baseline.rowHeight") as? Int) ?: table.rowHeight
    return (baseline * lines).coerceAtLeast(baseline)
}

@NlsSafe
internal fun escapeHtmlForTooltip(text: String): String {
    if (text.isEmpty()) return ""
    return escapeHtml(text)
}

@NlsSafe
internal fun makeMultilineTooltip(text: String, widthPx: Int): String {
    val width = widthPx.coerceIn(100, 1200)
    val escaped = escapeHtmlForTooltip(text)
        .replace("\r", "")
        .replace("\n", "<br>")
    return "<html><body style='width: ${width}px;'>$escaped</body></html>"
}

internal fun setTwoLineHeaderHeight(table: JTable) {
    val header = table.tableHeader ?: return
    val size = header.preferredSize
    val fm = header.getFontMetrics(header.font)
    val minForTwoLines = fm.height * 2 + JBUI.scale(4)
    val newHeight = minForTwoLines.coerceAtLeast(size.height + JBUI.scale(2))
    if (newHeight != size.height) {
        header.preferredSize = java.awt.Dimension(size.width, newHeight)
        header.revalidate()
        header.repaint()
    }
}

internal fun installTwoLineEllipsizingHeader(table: JTable) {
    val header = table.tableHeader ?: return
    header.defaultRenderer = object : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            tableObj: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val label = super.getTableCellRendererComponent(
                tableObj,
                value,
                isSelected,
                hasFocus,
                row,
                column
            ) as javax.swing.JLabel

            @NlsSafe val baseText = value?.toString()?.trim().orEmpty()
            val col = runCatching { tableObj.columnModel.getColumn(column) }.getOrNull()
            val width = ((col?.width ?: 80) - 12).coerceAtLeast(24)
            val fm = label.getFontMetrics(label.font)
            val (line1, line2) = splitTitleToTwoLines(baseText, fm, width)

            label.horizontalAlignment = CENTER
            label.border = JBUI.Borders.empty(0, JBUI.scale(4), 0, JBUI.scale(4))
            if (line2 == null) {
                label.text = escapeHtmlIfNeeded(baseText)
            } else {
                @NlsSafe val html = "<html><div style='text-align:center;margin:0;padding:0;line-height:1.1'>" +
                        escapeHtml(line1) + "<br>" + escapeHtml(line2) +
                        "</div></html>"
                label.text = html
            }
            label.toolTipText = baseText
            return label
        }
    }
    header.revalidate()
    header.repaint()
}

internal fun splitTitleToTwoLines(text: String, fm: FontMetrics, width: Int): Pair<String, String?> {
    if (text.isEmpty()) return "" to null
    if (fm.stringWidth(text) <= width) return text to null

    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.size == 1) {
        return fitWithEllipsis(words[0], fm, width) to null
    }

    val firstLine = StringBuilder()
    var i = 0
    while (i < words.size) {
        val candidate = if (firstLine.isEmpty()) words[i] else firstLine.toString() + " " + words[i]
        if (fm.stringWidth(candidate) <= width) {
            firstLine.clear()
            firstLine.append(candidate)
            i++
        } else {
            if (firstLine.isEmpty()) {
                val ell = fitWithEllipsis(words[i], fm, width)
                return ell to null
            }
            break
        }
    }

    val remaining = words.drop(i).joinToString(" ")
    if (remaining.isBlank()) return firstLine.toString() to null

    val secondLine = if (fm.stringWidth(remaining) <= width) {
        remaining
    } else {
        fitWithEllipsis(remaining, fm, width)
    }

    return firstLine.toString() to secondLine
}

internal fun fitWithEllipsis(text: String, fm: FontMetrics, width: Int): String {
    if (fm.stringWidth(text) <= width) return text
    val ell = "…"
    if (fm.stringWidth(ell) > width) return ell

    var end = text.length
    while (end > 0 && fm.stringWidth(text.substring(0, end) + ell) > width) {
        end--
    }
    return if (end <= 0) ell else text.substring(0, end) + ell
}

@NlsSafe
internal fun escapeHtmlIfNeeded(text: String): String {
    val plain = text
        .replace(Regex("<[^>]*>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    return plain
}

@NlsSafe
private fun escapeHtml(s: String): String {
    return s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}