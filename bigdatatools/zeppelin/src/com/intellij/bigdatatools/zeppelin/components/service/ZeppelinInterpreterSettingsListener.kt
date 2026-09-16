package com.intellij.bigdatatools.zeppelin.components.service

import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.interpreter.InterpreterTemplate
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings

interface ZeppelinInterpreterSettingsListener {
  fun updateInterpreterSettings(interpreterSettings: List<InterpreterSettings>) {}
  fun updateRepositories(repositories: List<Repository>, requestException: Throwable?) {}
  fun updateAvailableInterpreterTemplates(interpreters: List<InterpreterTemplate>, exception: Throwable?) {}
}