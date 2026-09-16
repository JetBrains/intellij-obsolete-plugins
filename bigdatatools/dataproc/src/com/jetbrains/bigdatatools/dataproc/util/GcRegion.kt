@file:Suppress("EnumEntryName")

package com.jetbrains.bigdatatools.dataproc.util

import com.intellij.bigdatatools.coreUi.settings.components.RenderableEntity
import com.intellij.openapi.util.NlsSafe

enum class GcRegion(override val title: @NlsSafe String) : RenderableEntity {
  asia_east1("Changhua County, Taiwan, APAC"),
  asia_east2("Hong Kong, APAC"),
  asia_northeast1("Tokyo, Japan, APAC"),
  asia_northeast2("Osaka, Japan, APAC"),
  asia_northeast3("Seoul, South Korea, APAC"),
  asia_south1("Mumbai, India APAC"),
  asia_south2("Delhi, India APAC"),
  asia_southeast1("Jurong West, Singapore, APAC"),
  asia_southeast2("Jakarta, Indonesia, APAC"),
  australia_southeast1("Sydney, Australia, APAC"),
  australia_southeast2("Melbourne, Australia, APAC"),
  europe_central2("Warsaw, Poland, Europe"),
  europe_north1("Hamina, Finland, Europe"),
  europe_southwest1("Madrid, Spain, Europe"),
  europe_west1("St. Ghislain, Belgium, Europe"),
  europe_west2("London, England, Europe"),
  europe_west3("Frankfurt, Germany Europe"),
  europe_west4("Eemshaven, Netherlands, Europe"),
  europe_west6("Zurich, Switzerland, Europe"),
  europe_west8("Milan, Italy, Europe"),
  europe_west9("Paris, France, Europe"),
  me_west1("Tel Aviv, Israel, Middle East"),
  northamerica_northeast1("Montreal, Quebec, North America"),
  northamerica_northeast2("Toronto, Ontario, North America"),
  southamerica_east1("Osasco, Sao Paulo, Brazil, South America"),
  southamerica_west1("Santiago, Chile, South America"),
  us_central1("Council Bluffs, Iowa, North America"),
  us_east1("Moncks Corner, South Carolina, North America"),
  us_east4("Ashburn, Virginia, North America"),
  us_east5("Columbus, Ohio, North America"),
  us_south1("Dallas, Texas, North America"),
  us_west1("The Dalles, Oregon, North America"),
  us_west2("Los Angeles, California, North America"),
  us_west3("Salt Lake City, Utah, North America"),
  us_west4("Las Vegas, Nevada, North America");

  override val id = name.replace("_", "-")


  companion object {
    fun getFromId(id: String) = values().firstOrNull { it.id == id }
  }
}