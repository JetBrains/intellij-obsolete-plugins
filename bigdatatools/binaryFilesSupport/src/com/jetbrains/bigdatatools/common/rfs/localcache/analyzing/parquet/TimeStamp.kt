package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone
import kotlin.math.absoluteValue

object TimeStamp {
  val Int.str2: String
    get() = when (this) {
      in 0..9 -> "0$this"
      in -9..-1 -> "-0${this.absoluteValue}"
      else -> this.toString()
    }

  val Int.str3: String
    get() = when (this) {
      in 0..9 -> "00$this"
      in 10..99 -> "0$this"
      in -9..-1 -> "-00${this.absoluteValue}"
      in -99..-10 -> "-0${this.absoluteValue}"
      else -> this.toString()
    }

  private val Int.str4: String
    get() = when (this) {
      in 0..9 -> "000$this"
      in 10..99 -> "00$this"
      in 100..999 -> "0$this"
      in -9..-1 -> "-000${this.absoluteValue}"
      in -99..-10 -> "-00${this.absoluteValue}"
      in -999..-100 -> "-0${this.absoluteValue}"
      else -> this.toString()
    }

  val Int.str6: String
    get() = when (this) {
      in 0..9 -> "00000$this"
      in 10..99 -> "0000$this"
      in 100..999 -> "000$this"
      in 1_000..9_999 -> "00$this"
      in 10_000..99_999 -> "0$this"
      in -9..-1 -> "-00000${this.absoluteValue}"
      in -99..-10 -> "-0000${this.absoluteValue}"
      in -999..-100 -> "-000${this.absoluteValue}"
      in -9_999..-1_000 -> "-00${this.absoluteValue}"
      in -99_999..-10_000 -> "-0${this.absoluteValue}"
      else -> this.toString()
    }

  val Int.str9: String
    get() = when (this) {
      in 0..9 -> "00000000$this"
      in 10..99 -> "0000000$this"
      in 100..999 -> "000000$this"
      in 1_000..9_999 -> "00000$this"
      in 10_000..99_999 -> "0000$this"
      in 100_000..999_999 -> "000$this"
      in 1_000_000..9_999_999 -> "00$this"
      in 10_000_000..99_999_999 -> "0$this"
      in -9..-1 -> "-00000000${this.absoluteValue}"
      in -99..-10 -> "-0000000${this.absoluteValue}"
      in -999..-100 -> "-000000${this.absoluteValue}"
      in -9_999..-1_000 -> "-00000${this.absoluteValue}"
      in -99_999..-10_000 -> "-0000${this.absoluteValue}"
      in -999_999..-100_000 -> "-000${this.absoluteValue}"
      in -9_999_999..-1_000_000 -> "-00${this.absoluteValue}"
      in -99_999_999..-10_000_000 -> "-0${this.absoluteValue}"
      else -> this.toString()
    }

  fun LocalDate.export(): String =
    "${year.str4}-${monthValue.str2}-${dayOfMonth.str2}"

  fun getZoneId(isAdjustedToUTC: Boolean): ZoneId = if (isAdjustedToUTC) ZoneOffset.UTC else TimeZone.getDefault().toZoneId()
}