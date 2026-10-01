package com.intellij.aidebugger.evaluation.intellij

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import java.io.File

object RunConfigResolver {
    fun expandMacros(path: String, projectDir: String): String {
        var p = path
        // Replace common macros that may appear in run config paths
        if (p.contains("\$PROJECT_DIR\$")) {
            p = p.replace("\$PROJECT_DIR\$", projectDir)
        }
        // Expand home directory shorthand if present
        if (p.startsWith("~")) {
            val home = System.getProperty("user.home") ?: ""
            if (home.isNotEmpty()) {
                p = File(home, p.removePrefix("~")).absolutePath
            }
        }
        return p
    }

    fun resolveTargetFromRunConfig(project: Project, rcName: String?): Pair<String, String>? {
        val runManager = RunManager.getInstance(project)
        val configurations = runManager.allConfigurationsList

        fun normalizeScriptPath(path: String): String {
            val expanded = expandMacros(path, project.basePath ?: "")
            val f = File(expanded)
            return if (f.isAbsolute) f.path else File(project.basePath ?: "", expanded).path
        }

        // If name provided, search for that specific config
        if (!rcName.isNullOrBlank()) {
            val config = configurations.find { it.name == rcName }
            config?.let {
                extractScriptOrModuleFromRunConfig(it)?.let { result ->
                    return if (result.first == "script") {
                        "script" to normalizeScriptPath(result.second)
                    } else {
                        result
                    }
                }
            }
            return null
        }

        // No name provided: try to auto-discover a single config with a script/module
        var found: Pair<String, String>? = null
        var count = 0
        for (config in configurations) {
            val result = extractScriptOrModuleFromRunConfig(config)
            if (result != null) {
                found = if (result.first == "script") {
                    "script" to normalizeScriptPath(result.second)
                } else {
                    result
                }
                count++
                if (count > 1) break
            }
        }
        return if (count == 1) found else null
    }

    fun resolveInterpreterFromRunConfig(project: Project, rcName: String): String? {
        val runManager = RunManager.getInstance(project)
        val configurations = runManager.allConfigurationsList

        val config = configurations.find { it.name == rcName }
        config?.let {
            extractInterpreterFromRunConfig(it)?.let { result ->
                return expandMacros(result, project.basePath ?: "")
            }
        }
        return null
    }

    private fun extractScriptOrModuleFromRunConfig(config: RunConfiguration): Pair<String, String>? {
        // Handle Python run configurations
        try {
            // Use reflection to handle Python plugin configurations if available
            val configClass = config.javaClass
            when {
                configClass.name.contains("python", ignoreCase = true) -> {
                    try {
                        // Try to get script path
                        val scriptPath = configClass.getMethod("getScriptName").invoke(config) as? String
                        if (!scriptPath.isNullOrBlank()) {
                            return "script" to scriptPath.trim()
                        }

                        // Try to get module name
                        val moduleName = configClass.getMethod("getModuleName").invoke(config) as? String
                        if (!moduleName.isNullOrBlank()) {
                            return "module" to moduleName.trim()
                        }
                    } catch (e: Exception) {
                        // Method might not exist, try other approaches
                    }
                }
            }

            // Generic approach: try to access common fields via reflection
            try {
                val fields = configClass.declaredFields
                for (field in fields) {
                    field.isAccessible = true
                    when (field.name.lowercase()) {
                        "scriptname", "script_name" -> {
                            val value = field.get(config) as? String
                            if (!value.isNullOrBlank()) return "script" to value.trim()
                        }
                        "modulename", "module_name" -> {
                            val value = field.get(config) as? String
                            if (!value.isNullOrBlank()) return "module" to value.trim()
                        }
                    }
                }
            } catch (e: Exception) {
                // Reflection failed, ignore
            }
        } catch (e: Exception) {
            // Configuration type not supported
        }
        return null
    }

    private fun extractInterpreterFromRunConfig(config: RunConfiguration): String? {
        try {
            val configClass = config.javaClass

            // Try to get SDK/interpreter path
            try {
                val sdkHome = configClass.getMethod("getSdkHome").invoke(config) as? String
                if (!sdkHome.isNullOrBlank()) return sdkHome.trim()
            } catch (e: Exception) {
                // Method might not exist
            }

            // Try via SDK object
            try {
                val sdk = configClass.getMethod("getSdk").invoke(config)
                if (sdk != null) {
                    val homePath = sdk.javaClass.getMethod("getHomePath").invoke(sdk) as? String
                    if (!homePath.isNullOrBlank()) return homePath.trim()
                }
            } catch (e: Exception) {
                // Method might not exist
            }

            // Generic reflection approach
            try {
                val fields = configClass.declaredFields
                for (field in fields) {
                    field.isAccessible = true
                    when (field.name.lowercase()) {
                        "sdkhome", "sdk_home", "interpreterhome", "interpreter_home" -> {
                            val value = field.get(config) as? String
                            if (!value.isNullOrBlank()) return value.trim()
                        }
                    }
                }
            } catch (e: Exception) {
                // Reflection failed
            }
        } catch (e: Exception) {
            // Configuration type not supported
        }
        return null
    }

    fun resolveEnvVarsFromRunConfig(project: Project, rcName: String): EnvVarsAggregator.RunEnvConfig? {
        val runManager = RunManager.getInstance(project)
        val configurations = runManager.allConfigurationsList

        val config = configurations.find { it.name == rcName }
        return config?.let { extractEnvVarsFromRunConfig(it) }
    }

    private fun extractEnvVarsFromRunConfig(config: RunConfiguration): EnvVarsAggregator.RunEnvConfig? {
        val vars = LinkedHashMap<String, String>()
        var passParent = true
        var hasAnySetting = false

        try {
            val configClass = config.javaClass

            // Try to get environment variables via common methods
            try {
                val envs = configClass.getMethod("getEnvs").invoke(config)
                if (envs is Map<*, *>) {
                    envs.forEach { (k, v) ->
                        if (k is String && v != null) {
                            vars[k] = v.toString()
                        }
                    }
                    if (vars.isNotEmpty()) {
                        hasAnySetting = true
                    }
                }
            } catch (e: Exception) {
                // Method might not exist
            }

            // Try to get isPassParentEnvs() setting
            try {
                val passParentEnvs = configClass.getMethod("isPassParentEnvs").invoke(config) as? Boolean
                if (passParentEnvs != null) {
                    passParent = passParentEnvs
                    hasAnySetting = true
                }
            } catch (e: Exception) {
                // Method might not exist
            }

            // Generic reflection approach
            if (!hasAnySetting) {
                try {
                    val fields = configClass.declaredFields
                    for (field in fields) {
                        field.isAccessible = true
                        when (field.name.lowercase()) {
                            "envs", "environmentvariables", "env_vars" -> {
                                val value = field.get(config)
                                if (value is Map<*, *>) {
                                    value.forEach { (k, v) ->
                                        if (k is String && v != null) {
                                            vars[k] = v.toString()
                                        }
                                    }
                                    if (vars.isNotEmpty()) {
                                        hasAnySetting = true
                                    }
                                }
                            }
                            "passparentenvs", "pass_parent_envs" -> {
                                val value = field.get(config) as? Boolean
                                if (value != null) {
                                    passParent = value
                                    hasAnySetting = true
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Reflection failed
                }
            }
        } catch (e: Exception) {
            // Configuration type not supported
        }

        return if (hasAnySetting) EnvVarsAggregator.RunEnvConfig(vars, passParent) else null
    }
}
