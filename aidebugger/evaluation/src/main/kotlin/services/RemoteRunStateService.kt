package com.intellij.aidebugger.evaluation.models.remote

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@State(name = "RemoteRunStateService", storages = [Storage("ai-debugger-remote-run.xml")])
@Service(Service.Level.PROJECT)
class RemoteRunStateService : PersistentStateComponent<RemoteRunStateService.State> {

    data class State(
        var executionId: String? = null,
        var configName: String? = null,
        var savedAt: Long? = null
    )

    private var myState: State = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun save(id: String?, configName: String?) {
        myState.executionId = id
        myState.configName = configName
        myState.savedAt = if (id == null) null else System.currentTimeMillis()
    }

    fun clear() {
        save(null, null)
    }

    companion object {
        fun getInstance(project: Project): RemoteRunStateService = project.service()
    }
}
