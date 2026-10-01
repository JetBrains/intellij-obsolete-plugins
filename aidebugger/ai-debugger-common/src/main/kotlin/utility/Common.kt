package com.intellij.aidebugger.common.utility

fun formatMilliseconds(millis: Long): String {
    val milliseconds = millis % 1000
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60)) % 24
    val days = millis / (1000 * 60 * 60 * 24)

    return buildString {
        if (days > 0) append("$days d ")
        if (hours > 0) append("$hours hr ")
        if (minutes > 0) append("$minutes min ")
        if (seconds > 0) append("$seconds sec ")
        if (milliseconds > 0 || isEmpty()) append("$milliseconds ms")
    }.trim()
}