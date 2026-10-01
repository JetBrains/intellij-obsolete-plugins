package com.intellij.aidebugger.common.utility

object FileNameSanitizer {
    private val INVALID_FILENAME_CHARS_REGEX = Regex("[\\\\/:*?\"<>|]")

    /**
     * Sanitizes a string to be used as a file name by replacing invalid characters with underscores.
     * Invalid characters are: \ / : * ? " < > |
     */
    fun sanitize(name: String, replacement: String = "_"): String {
        return name.replace(INVALID_FILENAME_CHARS_REGEX, replacement)
    }
}