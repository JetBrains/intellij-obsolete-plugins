package com.intellij.dbt.test

import com.intellij.dbt.diagram.splitPackageAndName

class DbtLineageUtilTest : DbtTestCase() {
  fun testSplitPackageAndName() {
    val result = splitPackageAndName("jaffle_shop.stg_orders")
    assertEquals("jaffle_shop", result.first)
    assertEquals("stg_orders", result.second)
  }

  fun testSplitLongPackageAndName() {
    val result = splitPackageAndName("custom.package.jaffle_shop.stg_orders")
    assertEquals("custom.package.jaffle_shop", result.first)
    assertEquals("stg_orders", result.second)
  }
  fun testSplitNoPackageAndName() {
    val result = splitPackageAndName("jaffle_shop_stg_orders")
    assertNull(result.first)
    assertEquals("jaffle_shop_stg_orders", result.second)
  }
}