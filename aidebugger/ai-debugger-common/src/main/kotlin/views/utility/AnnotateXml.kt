package com.intellij.aidebugger.common.views.utility

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.intellij.aidebugger.common.views.JsonFormatterTheme

fun annotateXml(xmlString: String, theme: JsonFormatterTheme): AnnotatedString? {
    if (xmlString.isBlank()) return null

    return buildAnnotatedString {
        fun punct(s: String) = withStyle(SpanStyle(color = theme.punctColor)) { append(s) }
        fun tagName(s: String) = withStyle(SpanStyle(color = theme.keyColor)) { append(s) }
        fun attributeName(s: String) = withStyle(SpanStyle(color = theme.numberColor)) { append(s) }
        fun attributeValue(s: String) = withStyle(SpanStyle(color = theme.stringColor)) { append(s) }
        fun textContent(s: String) = withStyle(SpanStyle(color = theme.textColor)) { append(s) }
        fun comment(s: String) = withStyle(SpanStyle(color = theme.nullColor)) { append(s) }
        fun nl() = append("\n")
        fun ind(level: Int) = append("  ".repeat(level))

        fun parseAttributes(attributeString: String) {
            var attrIndex = 0
            val attrs = attributeString.trim()

            while (attrIndex < attrs.length) {
                // Skip whitespace
                while (attrIndex < attrs.length && attrs[attrIndex].isWhitespace()) {
                    attrIndex++
                }

                if (attrIndex >= attrs.length) break

                // Find attribute name
                val nameStart = attrIndex
                while (attrIndex < attrs.length && attrs[attrIndex] != '=' && !attrs[attrIndex].isWhitespace()) {
                    attrIndex++
                }

                if (nameStart < attrIndex) {
                    attributeName(attrs.substring(nameStart, attrIndex))
                }

                // Skip whitespace and '='
                while (attrIndex < attrs.length && (attrs[attrIndex].isWhitespace() || attrs[attrIndex] == '=')) {
                    if (attrs[attrIndex] == '=') {
                        punct("=")
                    }
                    attrIndex++
                }

                // Find attribute value
                if (attrIndex < attrs.length && (attrs[attrIndex] == '"' || attrs[attrIndex] == '\'')) {
                    val quote = attrs[attrIndex]
                    val valueStart = attrIndex
                    attrIndex++ // Skip opening quote

                    while (attrIndex < attrs.length && attrs[attrIndex] != quote) {
                        attrIndex++
                    }

                    if (attrIndex < attrs.length) {
                        attrIndex++ // Skip closing quote
                        attributeValue(attrs.substring(valueStart, attrIndex))
                    }
                }

                // Add space between attributes
                if (attrIndex < attrs.length) {
                    append(" ")
                }
            }
        }

        try {
            var i = 0
            var indentLevel = 0
            val text = xmlString.trim()

            while (i < text.length) {
                when {
                    // Handle XML comments
                    text.startsWith("<!--", i) -> {
                        val endComment = text.indexOf("-->", i + 4)
                        if (endComment != -1) {
                            comment(text.substring(i, endComment + 3))
                            i = endComment + 3
                        } else {
                            append(text.substring(i))
                            break
                        }
                    }

                    // Handle XML declaration or processing instructions
                    text.startsWith("<?", i) -> {
                        val endPI = text.indexOf("?>", i + 2)
                        if (endPI != -1) {
                            punct("<?")
                            tagName(text.substring(i + 2, endPI))
                            punct("?>")
                            i = endPI + 2
                        } else {
                            append(text.substring(i))
                            break
                        }
                    }

                    // Handle opening/closing tags
                    text[i] == '<' -> {
                        val endTag = text.indexOf('>', i)
                        if (endTag != -1) {
                            val tagContent = text.substring(i + 1, endTag)

                            when {
                                // Closing tag
                                tagContent.startsWith("/") -> {
                                    indentLevel--
                                    ind(indentLevel)
                                    punct("<")
                                    punct("/")
                                    tagName(tagContent.substring(1).trim())
                                    punct(">")
                                }

                                // Self-closing tag
                                tagContent.endsWith("/") -> {
                                    ind(indentLevel)
                                    punct("<")
                                    val parts = tagContent.dropLast(1).trim().split(" ", limit = 2)
                                    tagName(parts[0])

                                    if (parts.size > 1) {
                                        append(" ")
                                        parseAttributes(parts[1])
                                    }

                                    punct(" />")
                                }

                                // Opening tag
                                else -> {
                                    ind(indentLevel)
                                    punct("<")
                                    val parts = tagContent.split(" ", limit = 2)
                                    tagName(parts[0])

                                    if (parts.size > 1) {
                                        append(" ")
                                        parseAttributes(parts[1])
                                    }

                                    punct(">")
                                    indentLevel++
                                }
                            }

                            i = endTag + 1

                            // Add newline after tags (except for inline content)
                            if (i < text.length && text[i] != '<' && !text.substring(i).trim().startsWith("<")) {
                                // Check if there's text content before next tag
                                val nextTag = text.indexOf('<', i)
                                if (nextTag != -1) {
                                    val content = text.substring(i, nextTag).trim()
                                    if (content.isNotEmpty()) {
                                        textContent(content)
                                        i = nextTag
                                        nl()
                                        continue
                                    }
                                }
                            }
                            nl()
                        } else {
                            append(text.substring(i))
                            break
                        }
                    }

                    // Handle text content between tags
                    else -> {
                        val nextTag = text.indexOf('<', i)
                        if (nextTag != -1) {
                            val content = text.substring(i, nextTag).trim()
                            if (content.isNotEmpty()) {
                                textContent(content)
                            }
                            i = nextTag
                        } else {
                            val content = text.substring(i).trim()
                            if (content.isNotEmpty()) {
                                textContent(content)
                            }
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback: return plain text if parsing fails
            append(xmlString)
        }
    }
}