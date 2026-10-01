package com.intellij.aidebugger.evaluation.services

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.python.run.PythonRunConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedList
import kotlin.io.path.Path
import kotlin.io.path.relativeTo

class PythonDependencyManagerDetector(private val project: Project) {

    private val projectRoot = project.guessProjectDir()?.toNioPath()!!

    sealed interface PythonEnvConf {
        val directory: String
    }

    private data class PoetryConf(override val directory: String) : PythonEnvConf
    private data class PipConf(val requirementsPath: String) : PythonEnvConf {
        override val directory: String get() = Path(requirementsPath).parent?.toString() ?: "."
    }
    private data class UvConf(override val directory: String) : PythonEnvConf
    private data class PdmConf(override val directory: String) : PythonEnvConf
    private data class HatchConf(override val directory: String) : PythonEnvConf

    fun resolvePythonVersion(runConfiguration: PythonRunConfiguration): String? {
        try {
            // Try: Python SDK on the run configuration
            val sdk = runCatching { runConfiguration.javaClass.getMethod("getSdk").invoke(runConfiguration) }.getOrNull()
            val v1 = runCatching { sdk?.javaClass?.getMethod("getVersionString")?.invoke(sdk) as? String }.getOrNull()
            extractPythonSemver(v1)?.let { return it }

            // Try: project-level Python SDK via PythonSdkUtil.findPythonSdk(project)
            try {
                val utilCls = Class.forName("com.jetbrains.python.sdk.PythonSdkUtil")
                val findSdk = utilCls.getMethod("findPythonSdk", Class.forName("com.intellij.openapi.project.Project"))
                val sdk2 = findSdk.invoke(null, project)
                val v2 = sdk2?.javaClass?.getMethod("getVersionString")?.invoke(sdk2) as? String
                extractPythonSemver(v2)?.let { return it }
            } catch (_: Throwable) {
                // ignore and fall through
            }

            // Last resort: interpreter path name (may include version e.g., python3.11)
            val interpreterPath = runCatching { runConfiguration.interpreterPath }.getOrNull()
            if (!interpreterPath.isNullOrBlank()) {
                val simple = interpreterPath.substringAfterLast('/')
                val m = Regex("(\\d+\\.\\d+(?:\\.\\d+)?)").find(simple)
                if (m != null) return m.groupValues[1]
            }
        } catch (_: Throwable) { }
        return null
    }

    private fun extractPythonSemver(versionString: String?): String? {
        if (versionString.isNullOrBlank()) return null
        val s = versionString.removePrefix("Python ").trim()
        val full = Regex("(\\d+\\.\\d+\\.\\d+)").find(s)?.groupValues?.getOrNull(1)
        if (full != null) return full
        return Regex("(\\d+\\.\\d+)").find(s)?.groupValues?.getOrNull(1)
    }

    fun walkIntermediateDirs(start: Path, end: Path): List<Path> {
        val pair = if (start.isAbsolute && end.isAbsolute) {
            end.normalize() to start.normalize()
        } else if (start.isAbsolute && !end.isAbsolute) {
            start.resolve(end).normalize() to start
        } else if (!start.isAbsolute && end.isAbsolute) {
            throw IllegalArgumentException("Cannot start from a relative path.")
        } else {
            end.normalize() to start.normalize()
        }
        var current: Path? = pair.first
        val lastPath = pair.second.parent
        val result = LinkedList<Path>()
        while (current != lastPath && current != null) {
            result.addFirst(current)
            current = current.parent
        }
        return result
    }

    fun findPythonDependencyManager(path: Path = projectRoot): PythonEnvConf? =
        findPythonDependencyManager(path, path)

    fun findPythonDependencyManager(from: Path, maxDepthPath: Path): PythonEnvConf? {
        return runReadAction {
            val dirs = walkIntermediateDirs(from, maxDepthPath)
            for (dir in dirs) {
                val requirementsFilePath = dir.resolve("requirements.txt")
                val pyprojectPath = dir.resolve("pyproject.toml")
                val lockFiles = listOf("uv.lock", "pdm.lock", "poetry.lock", "hatch.toml")

                val pyprojectFile = VfsUtil.findFile(pyprojectPath, false)
                val requirementsFile = VfsUtil.findFile(requirementsFilePath, false)

                // prefer pip if exists
                requirementsFile?.let {
                    val rel = it.toNioPath().relativeTo(from).toString()
                    return@runReadAction PipConf(rel)
                }


                pyprojectFile?.let { file ->
                    val content = VfsUtil.loadText(file)
                    when {
                        "[tool.poetry]" in content -> {
                            val parentDirPath = file.parent.toNioPath().relativeTo(from).ensureNotEmpty()
                            return@runReadAction PoetryConf(parentDirPath.toString())
                        }

                        "[tool.uv]" in content || VfsUtil.findFile(dir.resolve("uv.lock"), false) != null -> {
                            val parentDirPath = file.parent.toNioPath().relativeTo(from)
                            return@runReadAction UvConf(parentDirPath.toString())
                        }

                        "[tool.pdm]" in content || VfsUtil.findFile(dir.resolve("pdm.lock"), false) != null -> {
                            val parentDirPath = file.parent.toNioPath().relativeTo(from)
                            return@runReadAction PdmConf(parentDirPath.toString())
                        }

                        "[tool.hatch]" in content -> {
                            val parentDirPath = file.parent.toNioPath().relativeTo(from)
                            return@runReadAction HatchConf(parentDirPath.toString())
                        }
                    }
                }

                for (lockName in lockFiles) {
                    if (Files.exists(dir.resolve(lockName))) {
                        val parentDirPath = dir.relativeTo(from).ensureNotEmpty()
                        return@runReadAction when (lockName) {
                            "uv.lock" -> UvConf(parentDirPath.toString())
                            "pdm.lock" -> PdmConf(parentDirPath.toString())
                            "poetry.lock" -> PoetryConf(parentDirPath.toString())
                            "hatch.toml" -> HatchConf(parentDirPath.toString())
                            else -> null
                        }
                    }
                }
            }
            PoetryConf(DOT_PATH.toString())
        }
    }

    fun toShellVars(conf: PythonEnvConf?): Pair<String, String> = when (conf) {
        is PoetryConf -> "poetry" to "${conf.directory}/pyproject.toml"
        is PipConf -> "pip" to conf.requirementsPath
        is UvConf -> "uv" to "${conf.directory}/pyproject.toml"
        is PdmConf -> "pdm" to "${conf.directory}/pyproject.toml"
        is HatchConf -> "hatch" to "${conf.directory}/pyproject.toml"
        else -> "poetry" to "."
    }

    private val EMPTY_PATH = Path("")
    private val DOT_PATH = Path(".")
    fun Path.ensureNotEmpty() = if (this == EMPTY_PATH) DOT_PATH else this
}
