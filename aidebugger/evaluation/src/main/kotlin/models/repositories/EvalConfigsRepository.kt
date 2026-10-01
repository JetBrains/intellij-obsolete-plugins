package com.intellij.aidebugger.evaluation.models.repositories

import com.intellij.aidebugger.evaluation.models.entities.ConfigInfo
import com.intellij.aidebugger.evaluation.models.entities.EvalRunConfig
import java.nio.file.Path

interface EvalConfigsRepository {
    fun listConfigs(): List<ConfigInfo>
    fun loadConfig(info: ConfigInfo): EvalRunConfig
    fun createOrUpdateConfig(name: String, cfg: EvalRunConfig): ConfigInfo
    fun renameConfig(oldName: String, newName: String): ConfigInfo?
    fun removeConfig(name: String)
    fun getConfigPathByName(name: String): Path?
}
