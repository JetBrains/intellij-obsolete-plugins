package com.intellij.aidebugger.python.utility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PythonVersionTest {

    @Test
    fun `parse valid Python version string`() {
        // Arrange
        val versionString = "Python 3.8.10"

        // Act
        val result = PythonVersion.parse(versionString)

        // Assert
        assertNotNull(result)
        assertEquals(3, result?.major)
        assertEquals(8, result?.minor)
        assertEquals(10, result?.patch)
    }

    @Test
    fun `parse valid Python version string with additional text`() {
        // Arrange
        val versionString = "Python 3.9.5 (default, May 11 2021, 08:20:37)"

        // Act
        val result = PythonVersion.parse(versionString)

        // Assert
        assertNotNull(result)
        assertEquals(3, result?.major)
        assertEquals(9, result?.minor)
        assertEquals(5, result?.patch)
    }

    @Test
    fun `parse returns null for invalid Python version format`() {
        // Arrange
        val versionString = "Python3.8.10"  // Missing space between Python and version

        // Act
        val result = PythonVersion.parse(versionString)

        // Assert
        assertNull(result)
    }

    @Test
    fun `parse returns null for non-Python version string`() {
        // Arrange
        val versionString = "Node.js 14.17.0"

        // Act
        val result = PythonVersion.parse(versionString)

        // Assert
        assertNull(result)
    }

    @Test
    fun `parse returns null for empty string`() {
        // Arrange
        val versionString = ""

        // Act
        val result = PythonVersion.parse(versionString)

        // Assert
        assertNull(result)
    }

    @Test
    fun `parse returns null for incomplete version string`() {
        // Arrange
        val versionString = "Python 3.8"  // Missing patch version

        // Act
        val result = PythonVersion.parse(versionString)

        // Assert
        assertNull(result)
    }

    @Test
    fun `parse handles different version numbers correctly`() {
        // Arrange & Act & Assert
        val testCases = listOf(
            Triple("Python 1.0.0", 1, 0),
            Triple("Python 2.7.18", 2, 7),
            Triple("Python 3.6.9", 3, 6),
            Triple("Python 3.10.4", 3, 10),
            Triple("Python 3.11.0", 3, 11)
        )

        for ((versionString, expectedMajor, expectedMinor) in testCases) {
            val result = PythonVersion.parse(versionString)
            assertNotNull("Failed for version: $versionString", result)
            assertEquals("Major version mismatch for: $versionString", expectedMajor, result?.major)
            assertEquals("Minor version mismatch for: $versionString", expectedMinor, result?.minor)
        }
    }

    @Test
    fun `compareTo with different major versions`() {
        // Arrange
        val olderVersion = PythonVersion(2, 7, 18)
        val newerVersion = PythonVersion(3, 6, 9)

        // Act & Assert
        assertTrue("Newer version should be greater than older version", newerVersion > olderVersion)
        assertTrue("Older version should be less than newer version", olderVersion < newerVersion)
        assertFalse("Versions should not be equal", newerVersion == olderVersion)
    }

    @Test
    fun `compareTo with same major but different minor versions`() {
        // Arrange
        val olderVersion = PythonVersion(3, 6, 9)
        val newerVersion = PythonVersion(3, 10, 4)

        // Act & Assert
        assertTrue("Newer version should be greater than older version", newerVersion > olderVersion)
        assertTrue("Older version should be less than newer version", olderVersion < newerVersion)
        assertFalse("Versions should not be equal", newerVersion == olderVersion)
    }

    @Test
    fun `compareTo with same major and minor but different patch versions`() {
        // Arrange
        val olderVersion = PythonVersion(3, 10, 0)
        val newerVersion = PythonVersion(3, 10, 4)

        // Act & Assert
        assertTrue("Newer version should be greater than older version", newerVersion > olderVersion)
        assertTrue("Older version should be less than newer version", olderVersion < newerVersion)
        assertFalse("Versions should not be equal", newerVersion == olderVersion)
    }

    @Test
    fun `compareTo with identical versions`() {
        // Arrange
        val version1 = PythonVersion(3, 10, 4)
        val version2 = PythonVersion(3, 10, 4)

        // Act & Assert
        assertEquals("Identical versions should be equal", 0, version1.compareTo(version2))
        assertTrue("Identical versions should be equal", version1 == version2)
        assertFalse("Identical versions should not be greater than each other", version1 > version2)
        assertFalse("Identical versions should not be less than each other", version1 < version2)
    }
}