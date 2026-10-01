package com.intellij.aidebugger.common.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@State(
  name = "AI Debugger Settings",
  storages = [Storage("ai_debugger.xml")]
)
@Service(Service.Level.PROJECT)
class ProjectSettingsService : PersistentStateComponent<ProjectSettingsService.State> {

  data class State(
    var importsOfInterestPresent: Boolean = false,
  )

  companion object {
    fun getInstance(project: Project): ProjectSettingsService = project.service()
  }

  private var state = State()

  var importsOfInterestPresent: Boolean
    get() = state.importsOfInterestPresent
    set(value) { state.importsOfInterestPresent = value }

  val isAiProject: Boolean
    get() = state.importsOfInterestPresent

  override fun getState(): State? = state

  override fun loadState(state: State) {
    this.state = state
  }
}