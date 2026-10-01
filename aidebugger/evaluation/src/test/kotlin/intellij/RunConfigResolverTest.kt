package com.intellij.aidebugger.evaluation.intellij

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunConfigResolverTest {

    @Test
    fun `expandMacros replaces PROJECT_DIR macro`() {
        val projectDir = "/home/user/project"
        val path = "\$PROJECT_DIR\$/src/main.py"

        val result = RunConfigResolver.expandMacros(path, projectDir)

        assertEquals("/home/user/project/src/main.py", result)
    }

    @Test
    fun `expandMacros handles multiple PROJECT_DIR occurrences`() {
        val projectDir = "/project"
        val path = "\$PROJECT_DIR\$/\$PROJECT_DIR\$/file.py"

        val result = RunConfigResolver.expandMacros(path, projectDir)

        assertEquals("/project//project/file.py", result)
    }

    @Test
    fun `expandMacros returns path unchanged when no macros`() {
        val projectDir = "/home/user/project"
        val path = "/absolute/path/to/file.py"

        val result = RunConfigResolver.expandMacros(path, projectDir)

        assertEquals("/absolute/path/to/file.py", result)
    }

    @Test
    fun `expandMacros expands home directory`() {
        val userHome = System.getProperty("user.home")
        val path = "~/scripts/main.py"

        val result = RunConfigResolver.expandMacros(path, "/any/project")

        assertTrue(result.startsWith(userHome))
        assertTrue(result.endsWith("scripts/main.py"))
    }

    @Test
    fun `expandMacros handles tilde without trailing slash`() {
        val userHome = System.getProperty("user.home")
        val path = "~"

        val result = RunConfigResolver.expandMacros(path, "/any/project")

        assertEquals(userHome, result)
    }

    @Test
    fun `expandMacros combines PROJECT_DIR and tilde in path`() {
        val projectDir = "/home/user/project"
        val path = "\$PROJECT_DIR\$/~/file.py"

        val result = RunConfigResolver.expandMacros(path, projectDir)

        assertEquals("/home/user/project/~/file.py", result)
    }

    @Test
    fun `expandMacros handles relative paths`() {
        val projectDir = "/absolute/project"
        val path = "\$PROJECT_DIR\$/relative/path.py"

        val result = RunConfigResolver.expandMacros(path, projectDir)

        assertEquals("/absolute/project/relative/path.py", result)
    }

    @Test
    fun `expandMacros handles empty project dir`() {
        val path = "\$PROJECT_DIR\$/src/main.py"

        val result = RunConfigResolver.expandMacros(path, "")

        assertEquals("/src/main.py", result)
    }

    @Test
    fun `expandMacros handles path without macros or tilde`() {
        val result = RunConfigResolver.expandMacros("regular/path.py", "/project")

        assertEquals("regular/path.py", result)
    }

    @Test
    fun `expandMacros handles only PROJECT_DIR macro`() {
        val projectDir = "/my/project"
        val path = "\$PROJECT_DIR\$"

        val result = RunConfigResolver.expandMacros(path, projectDir)

        assertEquals("/my/project", result)
    }

    @Test
    fun `expandMacros handles tilde in middle of path`() {
        val path = "/absolute/~/relative"

        val result = RunConfigResolver.expandMacros(path, "/project")

        assertEquals("/absolute/~/relative", result)
    }

    @Test
    fun `expandMacros handles backslash paths with PROJECT_DIR`() {
        val projectDir = "C:\\Users\\test\\project"
        val path = "\$PROJECT_DIR\$\\src\\main.py"

        val result = RunConfigResolver.expandMacros(path, projectDir)

        assertEquals("C:\\Users\\test\\project\\src\\main.py", result)
    }
}
