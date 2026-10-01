package com.intellij.aidebugger.common.views.utility


import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses HTML string into an AnnotatedString for display in Compose UI.
 *
 * Supported HTML tags:
 * - Text formatting: b, strong, i, em, u, code
 * - Headers: h1, h2, h3, h4, h5, h6
 * - Structure: p, br, pre
 * - Links: a (with href attribute)
 * - Lists: ul, ol, li
 * - Colors: span with style tag for colors
 *
 * @param html The HTML string to parse
 * @return An AnnotatedString with appropriate styling
 */
fun parseHtmlToAnnotatedString(html: String, links: Map<String, () -> Unit> = emptyMap()): AnnotatedString {
    if (html.isBlank()) return AnnotatedString("")

    val document = try {
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(InputSource(StringReader("<body>$html</body>")))
    } catch (_: SAXException) {
        // If parsing fails, return the original text
        return AnnotatedString(html)
    } catch (_: Exception) {
        // Handle any other exceptions
        return AnnotatedString(html)
    }

    val builder = AnnotatedString.Builder()
    var listLevel = 0
    val listItemNumber = mutableMapOf<Int, Int>() // Track numbered list items at each level

    fun parseNode(node: Node, style: SpanStyle = SpanStyle(), isListItem: Boolean = false) {
        when (node.nodeType) {
            Node.TEXT_NODE -> {
                val text = node.textContent
                if (text.isNotBlank() || isListItem) {
                    builder.append(text)
                }
            }
            Node.ELEMENT_NODE -> {
                val tag = node.nodeName.lowercase()
                val newStyle = when (tag) {
                    "b", "strong" -> style.merge(SpanStyle(fontWeight = FontWeight.Bold))
                    "i", "em" -> style.merge(SpanStyle(fontStyle = FontStyle.Italic))
                    "u" -> style.merge(SpanStyle(textDecoration = TextDecoration.Underline))
                    "code" -> style.merge(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.LightGray.copy(alpha = 0.2f)))
                    "h1" -> style.merge(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp))
                    "h2" -> style.merge(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp))
                    "h3" -> style.merge(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
                    "h4" -> style.merge(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                    "h5" -> style.merge(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                    "h6" -> style.merge(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp))
                    "br" -> {
                        builder.append("\n")
                        return
                    }
                    "a" -> {
                        val href = node.attributes?.getNamedItem("href")?.nodeValue ?: ""
                        val start = builder.length
                        node.childNodes.let { for (i in 0 until it.length) parseNode(it.item(i), style) }
                        val end = builder.length
                        builder.addStyle(style.merge(SpanStyle(textDecoration = TextDecoration.Underline)), start, end)
                        val link = links[href] ?: {}
                        builder.addLink(LinkAnnotation.Clickable(
                            tag = href,
                            linkInteractionListener = { link() }
                        ), start, end)
                        return
                    }
                    "p" -> {
                        if (builder.length > 0 && !builder.toString().endsWith("\n\n")) {
                            builder.append("\n")
                        }
                        val start = builder.length
                        node.childNodes.let { for (i in 0 until it.length) parseNode(it.item(i), style) }
                        val end = builder.length
                        if (end > start && !builder.toString().endsWith("\n")) {
                            builder.append("\n")
                        }
                        return
                    }
                    "pre" -> {
                        if (builder.length > 0 && !builder.toString().endsWith("\n")) {
                            builder.append("\n")
                        }
                        val preStyle = style.merge(SpanStyle(fontFamily = FontFamily.Monospace))
                        val start = builder.length
                        node.childNodes.let { for (i in 0 until it.length) parseNode(it.item(i), preStyle) }
                        val end = builder.length
                        if (end > start && !builder.toString().endsWith("\n")) {
                            builder.append("\n")
                        }
                        return
                    }
                    "ul" -> {
                        if (builder.length > 0 && !builder.toString().endsWith("\n")) {
                            builder.append("\n")
                        }
                        listLevel++
                        node.childNodes.let { for (i in 0 until it.length) parseNode(it.item(i), style) }
                        listLevel--
                        if (!builder.toString().endsWith("\n")) {
                            builder.append("\n")
                        }
                        return
                    }
                    "ol" -> {
                        if (builder.length > 0 && !builder.toString().endsWith("\n")) {
                            builder.append("\n")
                        }
                        listLevel++
                        listItemNumber[listLevel] = 1
                        node.childNodes.let { for (i in 0 until it.length) parseNode(it.item(i), style) }
                        listLevel--
                        listItemNumber.remove(listLevel + 1)
                        if (!builder.toString().endsWith("\n")) {
                            builder.append("\n")
                        }
                        return
                    }
                    "li" -> {
                        val indent = "  ".repeat(listLevel - 1)
                        val bullet = if (listItemNumber.containsKey(listLevel)) {
                            val number = listItemNumber[listLevel] ?: 1
                            listItemNumber[listLevel] = number + 1
                            "$number. "
                        } else {
                            "• "
                        }
                        builder.append("$indent$bullet")
                        val start = builder.length
                        node.childNodes.let { for (i in 0 until it.length) parseNode(it.item(i), style, true) }
                        val end = builder.length
                        if (end > start && !builder.toString().endsWith("\n")) {
                            builder.append("\n")
                        }
                        return
                    }
                    "span" -> {
                        // TODO not tested
                        val color = extractColorFromStyle(node.attributes?.getNamedItem("style")?.nodeValue)
                        if (color != null) style.merge(SpanStyle(color = color)) else style
                    }
                    else -> style
                }

                if (tag == "inline_content_element") {
                    if (node.hasChildNodes()) {
                        val firstChild = node.childNodes.item(0)
                        if (firstChild.nodeType == Node.TEXT_NODE) {
                            val text = firstChild.textContent
                            if (text.isNotBlank()) {
                                builder.appendInlineContent(text, text)
                            }
                        }
                    }
                    return
                }

                val start = builder.length
                node.childNodes.let { for (i in 0 until it.length) parseNode(it.item(i), newStyle) }
                val end = builder.length

                // Apply style to the content if it's not empty
                if (end > start && tag !in listOf("p", "br", "ul", "ol", "li", "pre", "a")) {
                    builder.addStyle(newStyle, start, end)
                }

                // Add spacing after headers
                if (tag.startsWith("h") && tag.length == 2 && tag[1] in '1'..'6') {
                    if (!builder.toString().endsWith("\n\n")) {
                        builder.append("\n")
                    }
                }
            }
        }
    }

    document.documentElement?.childNodes?.let {
        for (i in 0 until it.length) {
            parseNode(it.item(i))
        }
    }

    // Trim leading/trailing whitespace but preserve internal formatting
    val result = builder.toAnnotatedString()
    val trimmedText = result.text.trim()
    if (trimmedText.isEmpty()) return AnnotatedString("")

    val startIndex = result.text.indexOf(trimmedText.first())
    val endIndex = result.text.lastIndexOf(trimmedText.last()) + 1

    return result.subSequence(startIndex, endIndex)
}


fun parseCssColorValue(raw: String): Color? {
    val s = raw.trim().lowercase()

    // #rgb, #rgba (4), #rrggbb (6), #rrggbbaa (8)
    if (s.startsWith("#")) {
        val hex = s.removePrefix("#")
        fun hexToInt(h: String) = h.toInt(16)
        when (hex.length) {
            3 -> { // #rgb
                val r = hexToInt("${hex[0]}${hex[0]}")
                val g = hexToInt("${hex[1]}${hex[1]}")
                val b = hexToInt("${hex[2]}${hex[2]}")
                return Color(r / 255f, g / 255f, b / 255f, 1f)
            }
            4 -> { // #rgba
                val r = hexToInt("${hex[0]}${hex[0]}")
                val g = hexToInt("${hex[1]}${hex[1]}")
                val b = hexToInt("${hex[2]}${hex[2]}")
                val a = hexToInt("${hex[3]}${hex[3]}")
                return Color(r / 255f, g / 255f, b / 255f, a / 255f)
            }
            6 -> { // #rrggbb
                val r = hexToInt(hex.substring(0, 2))
                val g = hexToInt(hex.substring(2, 4))
                val b = hexToInt(hex.substring(4, 6))
                return Color(r / 255f, g / 255f, b / 255f, 1f)
            }
            8 -> { // #rrggbbaa (CSS 8-digit hex)
                val r = hexToInt(hex.substring(0, 2))
                val g = hexToInt(hex.substring(2, 4))
                val b = hexToInt(hex.substring(4, 6))
                val a = hexToInt(hex.substring(6, 8))
                return Color(r / 255f, g / 255f, b / 255f, a / 255f)
            }
        }
        return null
    }

    // rgb(r,g,b) | rgba(r,g,b,a)
    val rgbRegex = Regex("""rgba?\(\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})(?:\s*,\s*([0-9]*\.?[0-9]+))?\s*\)""")
    rgbRegex.matchEntire(s)?.let { m ->
        val r = m.groupValues[1].toInt().coerceIn(0, 255)
        val g = m.groupValues[2].toInt().coerceIn(0, 255)
        val b = m.groupValues[3].toInt().coerceIn(0, 255)
        val a = m.groupValues.getOrNull(4)?.takeIf { it.isNotEmpty() }?.toFloat()?.coerceIn(0f, 1f) ?: 1f
        return Color(r / 255f, g / 255f, b / 255f, a)
    }

    // Common named colors (extend if needed)
    val named = mapOf(
        "black" to 0x000000, "white" to 0xFFFFFF, "red" to 0xFF0000, "blue" to 0x0000FF,
        "green" to 0x008000, "lime" to 0x00FF00, "yellow" to 0xFFFF00, "cyan" to 0x00FFFF,
        "aqua" to 0x00FFFF, "magenta" to 0xFF00FF, "fuchsia" to 0xFF00FF, "gray" to 0x808080,
        "grey" to 0x808080, "silver" to 0xC0C0C0, "maroon" to 0x800000, "olive" to 0x808000,
        "purple" to 0x800080, "teal" to 0x008080, "navy" to 0x000080, "orange" to 0xFFA500,
        "pink" to 0xFFC0CB, "brown" to 0xA52A2A
    )
    named[s]?.let { rgb ->
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return Color(r / 255f, g / 255f, b / 255f, 1f)
    }

    return null
}

fun extractColorFromStyle(styleAttr: String?): Color? {
    if (styleAttr.isNullOrBlank()) return null
    // Split "prop: val; prop2: val2"
    val decls = styleAttr.split(';')
    for (decl in decls) {
        val parts = decl.split(':', limit = 2)
        if (parts.size == 2) {
            val prop = parts[0].trim().lowercase()
            if (prop == "color") {
                val value = parts[1].trim()
                return parseCssColorValue(value)
            }
        }
    }
    return null
}
