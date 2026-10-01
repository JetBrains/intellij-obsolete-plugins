package com.intellij.aidebugger.python.viewModels

import org.junit.Assert.assertEquals
import org.junit.Test

object ViewModelUtilityTestCases {
    val DataMap1: Any? = mapOf(
        "my_data" to 10,
        "Messages" to listOf(
            "Hello",
            "World"
        )
    )

    val DataMap2: Any? = mapOf(
        "my_data" to 148,
        "messages" to listOf(
            mapOf("content" to "Hello"),
            mapOf("content" to "World")
        )
    )

    val DataList1: Any? = listOf(
        mapOf(
            "my_data" to 10,
            "Messages" to listOf(
                "Hello",
                "World"
            )
        )
    )
}


class ViewModelUtilityTest {
    @Test
    fun `getLastMessage works with Map as input`() {
        assertEquals("World",
            getLastMessage(ViewModelUtilityTestCases.DataMap1)
        )
        assertEquals(mapOf("content" to "World"),
            getLastMessage(ViewModelUtilityTestCases.DataMap2)
        )
    }

    @Test
    fun `getLastMessage works with List as input`() {
        assertEquals("World",
            getLastMessage(ViewModelUtilityTestCases.DataList1)
        )
    }
}