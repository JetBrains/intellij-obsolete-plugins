package com.intellij.bigdatatools.zeppelin.interpreters.components

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinNoteCacheConnection
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Key
import com.jetbrains.bigdatatools.common.delegate.Delegate2

class SparkInterpreterPrecodeHandler(private val notebookVirtualFile: NotebookVirtualFile,
                                     private val noteCacheConnection: ZeppelinNoteCacheConnection) : Disposable {
  private val delegate = Delegate2<InterpreterType, String, Unit>()

  var sparkPrecode: String = getPrecode(InterpreterType.SPARK)
    private set
  var pySparkPrecode: String = getPrecode(InterpreterType.PYSPARK)
    private set

  private val connectionListener = object : ZeppelinConnectionListener {
    override fun updateBindingsInterpretersSettings(bindingInterpreterSettings: List<InterpreterSettings>) {
      val newSparkPrecode = getPrecodeForInterpreters(bindingInterpreterSettings, InterpreterType.SPARK)
      if (newSparkPrecode != sparkPrecode) {
        sparkPrecode = newSparkPrecode
        delegate.notify(InterpreterType.SPARK, newSparkPrecode)
      }

      val newPySparkPrecode = getPrecodeForInterpreters(bindingInterpreterSettings, InterpreterType.PYSPARK)
      if (newPySparkPrecode != pySparkPrecode) {
        pySparkPrecode = newPySparkPrecode
        delegate.notify(InterpreterType.PYSPARK, newPySparkPrecode)
      }
    }
  }

  init {
    noteCacheConnection.addListener(connectionListener)
    notebookVirtualFile.putUserData(KEY, this)
  }

  override fun dispose() {
    noteCacheConnection.removeListener(connectionListener)
    notebookVirtualFile.putUserData(KEY, null)
  }

  fun subscribe(listener: (InterpreterType, String) -> Unit) {
    delegate.plusAssign(listener)
  }

  private fun getPrecode(type: InterpreterType): String {
    val settings = noteCacheConnection.bindingInterpreterSettings
    return getPrecodeForInterpreters(settings, type)
  }

  private fun getPrecodeForInterpreters(settings: List<InterpreterSettings>, type: InterpreterType): String {
    val precodeList = settings
      .filter { it.group == "spark" }
      .mapNotNull { it.properties["zeppelin.${type.value}.precode"]?.value as? String }
      .filter { it.isNotBlank() }
      .map { it.trim() }
    return precodeList.firstOrNull() ?: ""
  }

  enum class InterpreterType(val value: String) {
    SPARK("SparkInterpreter"), PYSPARK("PySparkInterpreter")
  }

  companion object {
    fun getForFile(notebookVirtualFile: NotebookVirtualFile) = notebookVirtualFile.getUserData(KEY)

    private val KEY = Key<SparkInterpreterPrecodeHandler>("SPARK_PRECODE_HANDLER")
  }
}
