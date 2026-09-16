package com.intellij.bigdatatools.zeppelin.components.connections

import com.google.gson.JsonObject
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.models.connection.AngularRemoveResponse
import com.intellij.bigdatatools.zeppelin.models.connection.AngularUpdateResponse
import com.intellij.bigdatatools.zeppelin.models.connection.Progress
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.bigdatatools.zeppelin.models.notebook.ParagraphOutput
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook

interface ZeppelinConnectionListener {
  fun onConnected() {}
  fun onConnectionError(throwable: Throwable) {}
  fun onDisconnected(statusCode: Int?, reason: String?) {}

  fun onZeppelinInfoChange(newInfo: ZeppelinInfo?) {}

  fun updateNotebookList(notebooks: List<NotebookInfo>) {}
  fun updateNotebook(notebook: ZeppelinNotebook) {}
  fun addCell(jsonCell: JsonObject, index: Int) {}
  fun removeCell(jsonCell: JsonObject) {}
  fun moveCell(sourceId: String, toIndex: Int) {}
  fun updateCell(paragraphJson: JsonObject) {}
  fun pathParagraph(paragraphId: String, patch: String) {}
  fun updateProgress(progress: Progress) {}
  fun updateOutput(paragraphOutput: ParagraphOutput, isUpdate: Boolean) {}

  fun updateInterpreterBindings(bindings: List<Interpreter>) {}
  fun updateInterpreterSettings(interpreterSettings: List<InterpreterSettings>) {}
  fun updateAvailableInterpreters(interpreters: List<InterpreterSettings>, exception: Throwable?) {}
  fun updateRepositories(repositories: List<Repository>, requestException: Throwable? = null) {}
  fun updateDefaultInterpreter(defaultInterpreterSettings: InterpreterSettings) {}
  fun updateBindingsInterpretersSettings(bindingInterpreterSettings: List<InterpreterSettings>) {}

  fun updateCollaborativeModeStatus(status: Boolean) {}
  fun updateAngularObject(newAngularObject: AngularUpdateResponse) {}
  fun removeAngularObject(angularObject: AngularRemoveResponse) {}
  fun onServerError(info: String) {}
  fun onParagraphInfo(info: Map<String, Any>) {}
}