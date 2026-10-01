package com.intellij.aidebugger.common.utility

class JsonPathParser(private val input: String) {
    private var position = 0
    private val tokens = mutableListOf<JsonPathToken>()

    private enum class Expectation {
        PROPERTY_NAME,
        SEPARATOR_OR_END,
        PATH_ELEMENT
    }

    fun parse(): List<JsonPathToken> {
        skipPrefix()
        var expectation = Expectation.PATH_ELEMENT

        while (!isAtEnd()) {
            val currentChar = currentChar()

            when (expectation) {
                Expectation.PROPERTY_NAME -> {
                    when (currentChar) {
                        '.' -> error("Consecutive dots are not allowed")
                        '[' -> error("Array index cannot follow dot without property name")
                        else -> {
                            parsePropertyName()
                            expectation = Expectation.SEPARATOR_OR_END
                        }
                    }
                }

                Expectation.SEPARATOR_OR_END -> {
                    when (currentChar) {
                        '.' -> {
                            advance()
                            expectation = Expectation.PROPERTY_NAME
                        }
                        '[' -> {
                            parseArrayIndex()
                            expectation = Expectation.SEPARATOR_OR_END
                        }
                        else -> error("Expected '.', '[' or end of path")
                    }
                }

                Expectation.PATH_ELEMENT -> {
                    when (currentChar) {
                        '.' -> {
                            advance()
                            expectation = Expectation.PROPERTY_NAME
                        }
                        '[' -> {
                            parseArrayIndex()
                            expectation = Expectation.SEPARATOR_OR_END
                        }
                        else -> {
                            parsePropertyName()
                            expectation = Expectation.SEPARATOR_OR_END
                        }
                    }
                }
            }
        }

        if (expectation == Expectation.PROPERTY_NAME) {
            error("Unexpected end of path after '.'")
        }

        return tokens
    }

    private fun parseArrayIndex() {
        require('[', "Expected '['")

        skipWhitespace()
        val indexStart = position

        while (!isAtEnd() && currentChar() != ']') {
            advance()
        }

        val indexStr = input.substring(indexStart, position).trim()

        if (indexStr.isEmpty()) {
            error("Empty brackets are not allowed")
        }

        val index = indexStr.toIntOrNull()
            ?: error("Invalid array index '$indexStr'")

        require(']', "Expected ']'")

        tokens.add(JsonPathToken.Idx(index))
    }

    private fun parsePropertyName() {
        val start = position

        while (!isAtEnd() && currentChar() != '.' && currentChar() != '[') {
            advance()
        }

        val name = input.substring(start, position)
        if (name.isNotEmpty()) {
            tokens.add(JsonPathToken.Segment(name))
        }
    }

    private fun skipPrefix() {
        if (!isAtEnd() && currentChar() == '$') {
            advance()
        }
        if (!isAtEnd() && currentChar() == '.') {
            advance()
        }
    }

    private fun skipWhitespace() {
        while (!isAtEnd() && currentChar().isWhitespace()) {
            advance()
        }
    }

    private fun currentChar(): Char = input[position]

    private fun advance() {
        position++
    }

    private fun require(expected: Char, message: String) {
        if (isAtEnd() || currentChar() != expected) {
            error("$message at position $position")
        }
        advance()
    }

    private fun isAtEnd(): Boolean = position >= input.length

    private fun error(message: String): Nothing {
        throw IllegalArgumentException("Invalid JSON path: $message at position $position in '$input'")
    }
}