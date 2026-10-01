package com.intellij.dataWrangler.llm

import kotlinx.serialization.json.JsonElement

interface DataWranglerFacade {
  suspend fun openDataWrangler(): Status
  fun findDataWranglerState(): JsonElement
  suspend fun applyDataWranglerSolution(solution: JsonElement): Status

  fun findDataGrid(): Object?

  enum class Status {
    SUCCESS, SOURCE_NOT_FOUND, INVALID_SOURCE, FAILURE
  }
}