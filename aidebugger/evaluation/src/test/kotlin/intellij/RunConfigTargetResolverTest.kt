package com.intellij.aidebugger.evaluation.intellij

import com.intellij.mock.MockProject
import com.intellij.openapi.util.Disposer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunConfigTargetResolverTest {

    @Test
    fun `toWorkspacePath converts absolute path to workspace relative`() {
        val project = createProject("/home/user/myproject")
        val absolutePath = "/home/user/myproject/src/main.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, absolutePath)

        assertEquals("'/workspace/src/main.py'", result)
    }

    @Test
    fun `toWorkspacePath handles relative path`() {
        val project = createProject("/home/user/myproject")
        val relativePath = "src/main.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, relativePath)

        assertEquals("'/workspace/src/main.py'", result)
    }

    @Test
    fun `toWorkspacePath converts backslashes to forward slashes`() {
        val project = createProject("C:\\Users\\test\\project")
        val windowsPath = "C:\\Users\\test\\project\\src\\main.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, windowsPath)

        assertTrue(result.contains("/src/main.py"))
        assertFalse(result.contains("\\"))
    }

    @Test
    fun `toWorkspacePath handles path outside project`() {
        val project = createProject("/home/user/myproject")
        val outsidePath = "/home/user/other/script.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, outsidePath)

        assertEquals("'/workspace/../other/script.py'", result)
    }

    @Test
    fun `toWorkspacePath handles nested project subdirectories`() {
        val project = createProject("/home/user/myproject")
        val nestedPath = "/home/user/myproject/src/package/module.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, nestedPath)

        assertEquals("'/workspace/src/package/module.py'", result)
    }

    @Test
    fun `toWorkspacePath handles project root path`() {
        val project = createProject("/home/user/myproject")
        val rootPath = "/home/user/myproject/main.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, rootPath)

        assertEquals("'/workspace/main.py'", result)
    }

    @Test
    fun `toWorkspacePath normalizes path with dot segments`() {
        val project = createProject("/home/user/myproject")
        val pathWithDots = "/home/user/myproject/src/../main.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, pathWithDots)

        assertEquals("'/workspace/main.py'", result)
    }

    @Test
    fun `toWorkspacePath handles relative path with dot segments`() {
        val project = createProject("/home/user/myproject")
        val relativePath = "./src/./main.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, relativePath)

        assertEquals("'/workspace/src/main.py'", result)
    }

    @Test
    fun `toWorkspacePath handles path with spaces`() {
        val project = createProject("/home/user/my project")
        val pathWithSpaces = "/home/user/my project/src/main file.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, pathWithSpaces)

        assertEquals("'/workspace/src/main file.py'", result)
    }

    @Test
    fun `toWorkspacePath quotes result for shell safety`() {
        val project = createProject("/home/user/myproject")
        val path = "src/main.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, path)

        assertTrue(result.startsWith("'"))
        assertTrue(result.endsWith("'"))
    }

    @Test
    fun `toWorkspacePath handles null basePath gracefully`() {
        val project = createProject(null)
        val path = "/absolute/path/to/script.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, path)

        assertEquals("'/workspace//absolute/path/to/script.py'", result)
    }

    @Test
    fun `toWorkspacePath handles parent directory traversal`() {
        val project = createProject("/home/user/myproject")
        val path = "/home/user/myproject/src/../../other/script.py"

        val result = RunConfigTargetResolver.toWorkspacePath(project, path)

        assertEquals("'/workspace/../other/script.py'", result)
    }

    private fun createProject(basePath: String?): ProjectStub {
        val disposable = Disposer.newDisposable()
        return ProjectStub(disposable, basePath)
    }

    private class ProjectStub(
        disposable: com.intellij.openapi.Disposable,
        private val projectBasePath: String?
    ) : MockProject(null, disposable) {
        override fun getBasePath() = projectBasePath
    }
}
