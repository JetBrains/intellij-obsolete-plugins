package com.intellij.aidebugger.evaluation.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import java.util.LinkedList
import kotlin.io.path.Path

class PythonDependencyManagerDetectorTest {

    @Test
    fun `walkIntermediateDirs with absolute paths`() {
        val detector = TestPythonDependencyManagerDetector()

        val start = Path("/home/user/project")
        val end = Path("/home/user/project/src/main/python")

        val result = detector.testWalkIntermediateDirs(start, end)

        assertEquals(4, result.size)
        assertTrue(result[0].toString().endsWith("project"))
        assertTrue(result[1].toString().endsWith("src"))
        assertTrue(result[2].toString().endsWith("main"))
        assertTrue(result[3].toString().endsWith("python"))
    }

    @Test
    fun `walkIntermediateDirs with relative path from absolute`() {
        val detector = TestPythonDependencyManagerDetector()

        val start = Path("/home/user/project")
        val end = Path("src/main")

        val result = detector.testWalkIntermediateDirs(start, end)

        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.toString().contains("src") })
    }

    @Test
    fun `walkIntermediateDirs with relative paths`() {
        val detector = TestPythonDependencyManagerDetector()

        val start = Path("project")
        val end = Path("project/src/main")

        val result = detector.testWalkIntermediateDirs(start, end)

        assertTrue(result.isNotEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `walkIntermediateDirs throws when start is relative and end is absolute`() {
        val detector = TestPythonDependencyManagerDetector()

        val start = Path("project")
        val end = Path("/home/user/project")

        detector.testWalkIntermediateDirs(start, end)
    }

    @Test
    fun `toShellVars for PoetryConf`() {
        val detector = TestPythonDependencyManagerDetector()

        val conf = detector.createPoetryConf("src/myapp")
        val (tool, path) = detector.testToShellVars(conf)

        assertEquals("poetry", tool)
        assertEquals("src/myapp/pyproject.toml", path)
    }

    @Test
    fun `toShellVars for PipConf`() {
        val detector = TestPythonDependencyManagerDetector()

        val conf = detector.createPipConf("requirements.txt")
        val (tool, path) = detector.testToShellVars(conf)

        assertEquals("pip", tool)
        assertEquals("requirements.txt", path)
    }

    @Test
    fun `toShellVars for UvConf`() {
        val detector = TestPythonDependencyManagerDetector()

        val conf = detector.createUvConf(".")
        val (tool, path) = detector.testToShellVars(conf)

        assertEquals("uv", tool)
        assertEquals("./pyproject.toml", path)
    }

    @Test
    fun `toShellVars for PdmConf`() {
        val detector = TestPythonDependencyManagerDetector()

        val conf = detector.createPdmConf("backend")
        val (tool, path) = detector.testToShellVars(conf)

        assertEquals("pdm", tool)
        assertEquals("backend/pyproject.toml", path)
    }

    @Test
    fun `toShellVars for HatchConf`() {
        val detector = TestPythonDependencyManagerDetector()

        val conf = detector.createHatchConf(".")
        val (tool, path) = detector.testToShellVars(conf)

        assertEquals("hatch", tool)
        assertEquals("./pyproject.toml", path)
    }

    @Test
    fun `toShellVars for null conf returns default poetry`() {
        val detector = TestPythonDependencyManagerDetector()

        val (tool, path) = detector.testToShellVars(null)

        assertEquals("poetry", tool)
        assertEquals(".", path)
    }

    @Test
    fun `directory property works for PipConf with parent path`() {
        val detector = TestPythonDependencyManagerDetector()

        val conf = detector.createPipConf("src/requirements.txt")

        assertEquals("src", conf.directory)
    }

    @Test
    fun `directory property works for PipConf with root path`() {
        val detector = TestPythonDependencyManagerDetector()

        val conf = detector.createPipConf("requirements.txt")

        assertEquals(".", conf.directory)
    }

    class TestPythonDependencyManagerDetector {
        fun testWalkIntermediateDirs(start: Path, end: Path): List<Path> {
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

        fun testToShellVars(conf: PythonEnvConf?): Pair<String, String> = when (conf) {
            is PoetryConf -> "poetry" to "${conf.directory}/pyproject.toml"
            is PipConf -> "pip" to conf.requirementsPath
            is UvConf -> "uv" to "${conf.directory}/pyproject.toml"
            is PdmConf -> "pdm" to "${conf.directory}/pyproject.toml"
            is HatchConf -> "hatch" to "${conf.directory}/pyproject.toml"
            else -> "poetry" to "."
        }

        interface PythonEnvConf {
            val directory: String
        }

        data class PoetryConf(override val directory: String) : PythonEnvConf
        data class PipConf(val requirementsPath: String) : PythonEnvConf {
            override val directory: String get() = Path(requirementsPath).parent?.toString() ?: "."
        }
        data class UvConf(override val directory: String) : PythonEnvConf
        data class PdmConf(override val directory: String) : PythonEnvConf
        data class HatchConf(override val directory: String) : PythonEnvConf

        fun createPoetryConf(directory: String) = PoetryConf(directory)
        fun createPipConf(requirementsPath: String) = PipConf(requirementsPath)
        fun createUvConf(directory: String) = UvConf(directory)
        fun createPdmConf(directory: String) = PdmConf(directory)
        fun createHatchConf(directory: String) = HatchConf(directory)
    }
}
