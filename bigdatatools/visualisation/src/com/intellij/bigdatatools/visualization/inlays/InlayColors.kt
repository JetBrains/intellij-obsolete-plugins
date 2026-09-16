package com.intellij.bigdatatools.visualization.inlays

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.ui.JBColor
import java.awt.Color

private val CELL_UNDER_CARET_COMMAND_MODE_STRIPE_COLOR = ColorKey.createColorKey("JUPYTER.CELL_UNDER_CARET_COMMAND_MODE_STRIPE_COLOR")
private val CODE_CELL_BACKGROUND = ColorKey.createColorKey("JUPYTER.CODE_CELL_BACKGROUND")
private val BOOKMARK_ICON_BACKGROUND = ColorKey.createColorKey("BookmarkIcon.background", JBColor(0xF7C777, 0xAA8542))

fun getCodeCellBackground(scheme: EditorColorsScheme): Color? = scheme.getColor(CODE_CELL_BACKGROUND)
fun getSelectedCellStripeColor(scheme: EditorColorsScheme): Color? = scheme.getColor(CELL_UNDER_CARET_COMMAND_MODE_STRIPE_COLOR)
fun getUnsyncCellStripeColor(scheme: EditorColorsScheme): Color? = scheme.getColor(BOOKMARK_ICON_BACKGROUND)