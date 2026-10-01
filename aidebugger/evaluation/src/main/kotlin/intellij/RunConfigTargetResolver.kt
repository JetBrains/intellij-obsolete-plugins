package com.intellij.aidebugger.evaluation.intellij

import com.intellij.openapi.project.Project
import java.nio.file.Path

/**
 * Resolves target flags (script/module) from IntelliJ Run Configurations.
 * Follows MVVM pattern - pure utility function for IntelliJ integration.
 */
object RunConfigTargetResolver {

    /**
     * Build command-line target flags from a Run Configuration.
     * Returns null if the configuration cannot be resolved.
     */
    fun buildTargetFlagsFromRunConfig(project: Project, runConfigName: String?): String? {
        val rm = com.intellij.execution.RunManager.getInstance(project)
        val settings = if (!runConfigName.isNullOrBlank())
            rm.allSettings.firstOrNull { it.name == runConfigName }
        else
            rm.selectedConfiguration

        val cfg = settings?.configuration ?: return null

        val pyCfgClassName = "com.jetbrains.python.run.PythonRunConfiguration"
        val pyCls = runCatching { Class.forName(pyCfgClassName) }.getOrNull()
        if (pyCls != null && pyCls.isInstance(cfg)) {
            fun callBool(name: String): Boolean? = runCatching {
                pyCls.getMethod(name).invoke(cfg) as? Boolean
            }.getOrNull()

            fun callStr(name: String): String? = runCatching {
                (pyCls.getMethod(name).invoke(cfg) as? String)?.trim()?.takeIf { it.isNotEmpty() }
            }.getOrNull()

            val scriptName = callStr("getScriptName")
            if (!scriptName.isNullOrEmpty()) {
                return "--script " + toWorkspacePath(project, scriptName)
            }

            val isModuleMode = callBool("isModuleMode") ?: callBool("getModuleMode") ?: false
            return if (isModuleMode) {
                val moduleName = callStr("getModuleName")
                if (!moduleName.isNullOrEmpty()) "--module $moduleName" else null
            } else {
                val scriptPath = callStr("getScriptName")
                if (!scriptPath.isNullOrEmpty()) "--script " + toWorkspacePath(project, scriptPath) else null
            }
        }
        return null
    }

    /**
     * Convert a file path to workspace-relative path for Docker execution.
     */
    fun toWorkspacePath(project: Project, path: String): String {
        val projectNio = project.basePath?.let { Path.of(it) }
        return try {
            val sp = java.nio.file.Paths.get(path)
            val abs = if (sp.isAbsolute) sp.normalize() else projectNio?.resolve(path)?.normalize()
            val rel = if (abs != null && projectNio != null) projectNio.relativize(abs).toString() else path
            "'/workspace/" + rel.replace('\\', '/') + "'"
        } catch (_: Throwable) {
            "'/workspace/" + path.replace('\\', '/') + "'"
        }
    }
}