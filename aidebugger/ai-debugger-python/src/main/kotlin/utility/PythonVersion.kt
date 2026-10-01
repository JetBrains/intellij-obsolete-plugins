package com.intellij.aidebugger.python.utility


data class PythonVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<PythonVersion> {
    companion object {
        private val versionRegex = Regex("""Python (\d+)\.(\d+)\.(\d+)""")

        fun parse(versionString: String): PythonVersion? {
            return versionRegex.find(versionString)?.destructured?.let { (major, minor, patch) ->
                PythonVersion(major.toInt(), minor.toInt(), patch.toInt())
            }
        }
    }

    override fun compareTo(other: PythonVersion): Int = compareValuesBy(
        this,
        other,
        PythonVersion::major,
        PythonVersion::minor,
        PythonVersion::patch
    )
}