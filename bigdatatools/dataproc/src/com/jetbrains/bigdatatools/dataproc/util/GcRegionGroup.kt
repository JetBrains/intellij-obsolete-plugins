@file:Suppress("EnumEntryName")

package com.jetbrains.bigdatatools.dataproc.util

enum class GcRegionGroup(val title: String) {
  APAC("Asia-Pacific"),
  NORTH_AMERICA("North America"),
  SOUTH_AMERICA("South America"),
  EUROPE("Europe"),
  MIDDLE_EAST("Middle East");

  val id = name.replace("_", "-")
  val regions get() = GcRegion.entries.filter { getGroupForRegion(it) == this }

  companion object {
    fun getGroupForRegion(region: GcRegion): GcRegionGroup = when (region) {
      GcRegion.asia_east1 -> APAC
      GcRegion.asia_east2 -> APAC
      GcRegion.asia_northeast1 -> APAC
      GcRegion.asia_northeast2 -> APAC
      GcRegion.asia_northeast3 -> APAC
      GcRegion.asia_south1 -> APAC
      GcRegion.asia_south2 -> APAC
      GcRegion.asia_southeast1 -> APAC
      GcRegion.asia_southeast2 -> APAC
      GcRegion.australia_southeast1 -> APAC
      GcRegion.australia_southeast2 -> APAC
      GcRegion.europe_central2 -> EUROPE
      GcRegion.europe_north1 -> EUROPE
      GcRegion.europe_southwest1 -> EUROPE
      GcRegion.europe_west1 -> EUROPE
      GcRegion.europe_west2 -> EUROPE
      GcRegion.europe_west3 -> EUROPE
      GcRegion.europe_west4 -> EUROPE
      GcRegion.europe_west6 -> EUROPE
      GcRegion.europe_west8 -> EUROPE
      GcRegion.europe_west9 -> EUROPE
      GcRegion.me_west1 -> MIDDLE_EAST
      GcRegion.northamerica_northeast1 -> NORTH_AMERICA
      GcRegion.northamerica_northeast2 -> NORTH_AMERICA
      GcRegion.southamerica_east1 -> SOUTH_AMERICA
      GcRegion.southamerica_west1 -> SOUTH_AMERICA
      GcRegion.us_central1 -> NORTH_AMERICA
      GcRegion.us_east1 -> NORTH_AMERICA
      GcRegion.us_east4 -> NORTH_AMERICA
      GcRegion.us_east5 -> NORTH_AMERICA
      GcRegion.us_south1 -> NORTH_AMERICA
      GcRegion.us_west1 -> NORTH_AMERICA
      GcRegion.us_west2 -> NORTH_AMERICA
      GcRegion.us_west3 -> NORTH_AMERICA
      GcRegion.us_west4 -> NORTH_AMERICA
    }
  }
}