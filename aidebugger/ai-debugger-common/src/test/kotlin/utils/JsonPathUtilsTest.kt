package com.intellij.aidebugger.common.utils

import com.intellij.aidebugger.common.utility.JsonPathToken
import com.intellij.aidebugger.common.utility.JsonPathUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonPathUtilsTest {

    @Test
    fun `parseJsonPath should handle simple property access`() {
        val tokens = JsonPathUtils.parseJsonPath("$.prop1")
        assertEquals(
            listOf(JsonPathToken.Segment("prop1")),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle nested property access`() {
        val tokens = JsonPathUtils.parseJsonPath("$.prop1.prop2.prop3")
        assertEquals(
            listOf(
                JsonPathToken.Segment("prop1"),
                JsonPathToken.Segment("prop2"),
                JsonPathToken.Segment("prop3")
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle array index access`() {
        val tokens = JsonPathUtils.parseJsonPath("$.arr[0]")
        assertEquals(
            listOf(
                JsonPathToken.Segment("arr"),
                JsonPathToken.Idx(0)
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle negative array index`() {
        val tokens = JsonPathUtils.parseJsonPath("$.arr[-1]")
        assertEquals(
            listOf(
                JsonPathToken.Segment("arr"),
                JsonPathToken.Idx(-1)
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle multiple array indices`() {
        val tokens = JsonPathUtils.parseJsonPath("$.arr[0][1]")
        assertEquals(
            listOf(
                JsonPathToken.Segment("arr"),
                JsonPathToken.Idx(0),
                JsonPathToken.Idx(1)
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle mixed property and array access`() {
        val tokens = JsonPathUtils.parseJsonPath("$.events[0].payload")
        assertEquals(
            listOf(
                JsonPathToken.Segment("events"),
                JsonPathToken.Idx(0),
                JsonPathToken.Segment("payload")
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle complex nested path`() {
        val tokens = JsonPathUtils.parseJsonPath("$.events[0][0].payload.inputs.messages[0].content")
        assertEquals(
            listOf(
                JsonPathToken.Segment("events"),
                JsonPathToken.Idx(0),
                JsonPathToken.Idx(0),
                JsonPathToken.Segment("payload"),
                JsonPathToken.Segment("inputs"),
                JsonPathToken.Segment("messages"),
                JsonPathToken.Idx(0),
                JsonPathToken.Segment("content")
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle path without dollar sign`() {
        val tokens = JsonPathUtils.parseJsonPath("prop1.prop2")
        assertEquals(
            listOf(
                JsonPathToken.Segment("prop1"),
                JsonPathToken.Segment("prop2")
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle path with leading dot`() {
        val tokens = JsonPathUtils.parseJsonPath(".prop1.prop2")
        assertEquals(
            listOf(
                JsonPathToken.Segment("prop1"),
                JsonPathToken.Segment("prop2")
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle empty path`() {
        val tokens = JsonPathUtils.parseJsonPath("")
        assertEquals(emptyList<JsonPathToken>(), tokens)
    }

    @Test
    fun `parseJsonPath should handle path with only dollar sign`() {
        val tokens = JsonPathUtils.parseJsonPath("$")
        assertEquals(emptyList<JsonPathToken>(), tokens)
    }

    @Test
    fun `parseJsonPath should handle path with whitespace`() {
        val tokens = JsonPathUtils.parseJsonPath("  $.prop1.prop2  ")
        assertEquals(
            listOf(
                JsonPathToken.Segment("prop1"),
                JsonPathToken.Segment("prop2")
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle array index with whitespace`() {
        val tokens = JsonPathUtils.parseJsonPath("$.arr[ 5 ]")
        assertEquals(
            listOf(
                JsonPathToken.Segment("arr"),
                JsonPathToken.Idx(5)
            ),
            tokens
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseJsonPath should reject invalid array index`() {
        JsonPathUtils.parseJsonPath("$.arr[invalid]")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseJsonPath should reject dot before array index`() {
        JsonPathUtils.parseJsonPath("$.arr.[0]")
    }

    @Test
    fun `parseJsonPath should handle large array index`() {
        val tokens = JsonPathUtils.parseJsonPath("$.arr[999]")
        assertEquals(
            listOf(
                JsonPathToken.Segment("arr"),
                JsonPathToken.Idx(999)
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle large negative array index`() {
        val tokens = JsonPathUtils.parseJsonPath("$.arr[-100]")
        assertEquals(
            listOf(
                JsonPathToken.Segment("arr"),
                JsonPathToken.Idx(-100)
            ),
            tokens
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseJsonPath should reject consecutive dots`() {
        JsonPathUtils.parseJsonPath("$.prop1..prop2")
    }

    @Test
    fun `parseJsonPath should handle property names with numbers`() {
        val tokens = JsonPathUtils.parseJsonPath("$.prop123.test456")
        assertEquals(
            listOf(
                JsonPathToken.Segment("prop123"),
                JsonPathToken.Segment("test456")
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle single property without dollar`() {
        val tokens = JsonPathUtils.parseJsonPath("prop")
        assertEquals(
            listOf(JsonPathToken.Segment("prop")),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle array without property`() {
        val tokens = JsonPathUtils.parseJsonPath("$[0]")
        assertEquals(
            listOf(JsonPathToken.Idx(0)),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle multiple consecutive arrays`() {
        val tokens = JsonPathUtils.parseJsonPath("$[0][1][2]")
        assertEquals(
            listOf(
                JsonPathToken.Idx(0),
                JsonPathToken.Idx(1),
                JsonPathToken.Idx(2)
            ),
            tokens
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseJsonPath should reject property after array without dot`() {
        JsonPathUtils.parseJsonPath("$.arr[0]prop")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseJsonPath should reject empty brackets`() {
        JsonPathUtils.parseJsonPath("$.arr[]")
    }

    @Test
    fun `parseJsonPath should handle property name with underscores`() {
        val tokens = JsonPathUtils.parseJsonPath("$.my_property.another_one")
        assertEquals(
            listOf(
                JsonPathToken.Segment("my_property"),
                JsonPathToken.Segment("another_one")
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle property name with hyphens`() {
        val tokens = JsonPathUtils.parseJsonPath("$.my-property.another-one")
        assertEquals(
            listOf(
                JsonPathToken.Segment("my-property"),
                JsonPathToken.Segment("another-one")
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle property after array with dot`() {
        val tokens = JsonPathUtils.parseJsonPath("$.arr[0].prop")
        assertEquals(
            listOf(
                JsonPathToken.Segment("arr"),
                JsonPathToken.Idx(0),
                JsonPathToken.Segment("prop")
            ),
            tokens
        )
    }

    @Test
    fun `parseJsonPath should handle array after array`() {
        val tokens = JsonPathUtils.parseJsonPath("$.arr[0][1]")
        assertEquals(
            listOf(
                JsonPathToken.Segment("arr"),
                JsonPathToken.Idx(0),
                JsonPathToken.Idx(1)
            ),
            tokens
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseJsonPath should reject invalid character after closing bracket`() {
        JsonPathUtils.parseJsonPath("$.arr[0]x")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseJsonPath should reject invalid character sequence after bracket`() {
        JsonPathUtils.parseJsonPath("$.arr[0]invalid")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseJsonPath should reject trailing dot`() {
        JsonPathUtils.parseJsonPath("$.prop1.")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseJsonPath should reject multiple consecutive dots`() {
        JsonPathUtils.parseJsonPath("$.prop1...prop2")
    }
}
