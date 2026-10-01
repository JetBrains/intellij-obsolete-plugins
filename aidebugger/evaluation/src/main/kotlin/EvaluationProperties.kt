package com.intellij.aidebugger.evaluation

import java.io.IOException
import java.util.Properties

object EvaluationProperties {
    private val props = Properties()

    // Property key for server URL in PyCharm properties
    private const val SERVER_URL_PROPERTY_KEY = "jetbrains.cadence.server.url"

    init {
        try {
            EvaluationProperties::class.java.getClassLoader().getResourceAsStream("config.properties")
                .use { input -> props.load(input) }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    val TOS_URL = props.getProperty("tos_url")
}
