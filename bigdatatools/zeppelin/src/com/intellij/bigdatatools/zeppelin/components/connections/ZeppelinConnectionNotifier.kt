package com.intellij.bigdatatools.zeppelin.components.connections

import com.google.gson.Gson
import com.intellij.bigdatatools.zeppelin.api.remote.websocket.WsResponseMessage
import com.intellij.bigdatatools.zeppelin.components.connections.parser.ZeppelinVersionAdapter
import com.intellij.bigdatatools.zeppelin.models.ZeppelinException
import com.intellij.bigdatatools.zeppelin.models.connection.AngularRemoveResponse
import com.intellij.bigdatatools.zeppelin.models.connection.AngularUpdateResponse
import com.intellij.bigdatatools.zeppelin.models.connection.Progress
import com.intellij.bigdatatools.zeppelin.models.connection.WsMessages
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import com.intellij.bigdatatools.zeppelin.models.notebook.ParagraphOutput
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.utils.JsonParser
import com.intellij.openapi.diagnostic.Logger

internal class ZeppelinConnectionNotifier {
  private var connectionListeners: List<ZeppelinConnectionListener> = emptyList()

  fun notifyOnMessage(message: WsResponseMessage, zeppelinInfo: ZeppelinInfo?) {
    try {
      processOperation(message, zeppelinInfo)
    }
    catch (e: Exception) {
      logger.error(e)
    }
  }

  fun notifyOnClose(statusCode: Int?, reason: String?) = tryNotifyWithExceptionLog {
    it.onDisconnected(statusCode, reason)
  }

  fun notifyOnError(throwable: Throwable) = tryNotifyWithExceptionLog {
    it.onConnectionError(throwable)
  }

  fun notifyOnConnect() = tryNotifyWithExceptionLog {
    it.onConnected()
  }

  fun notifyServerInfoChange(zeppelinInfo: ZeppelinInfo?) {
    tryNotifyWithExceptionLog {
      it.onZeppelinInfoChange(zeppelinInfo)
    }
  }

  fun addListener(listener: ZeppelinConnectionListener) {
    connectionListeners = connectionListeners + listener
  }

  fun removeListener(listener: ZeppelinConnectionListener) {
    connectionListeners = connectionListeners - listener
  }

  @Suppress("UNCHECKED_CAST")
  private fun processOperation(response: WsResponseMessage,
                               zeppelinInfo: ZeppelinInfo?) {
    zeppelinInfo ?: let {
      logger.error("Received message and Zeppelin info is not found, messages are skipped")
      return
    }
    when (response.op) {
      WsMessages.PARAS_INFO -> {
        val data = response.data as Map<String, Any>

        tryNotifyWithExceptionLog {
          it.onParagraphInfo(data)
        }
      }
      WsMessages.ERROR_INFO -> {
        val data = response.data as Map<String, String>
        val info = data["info"] ?: return
        tryNotifyWithExceptionLog {
          it.onServerError(info)
        }
      }
      WsMessages.NOTE -> {
        val noteJson = Gson().toJsonTree(response.data).asJsonObject.getAsJsonObject("note").toString()
        val notebook = ZeppelinNotebook(noteJson.reader())
        tryNotifyWithExceptionLog {
          it.updateNotebook(notebook)
        }
      }
      WsMessages.PATCH_PARAGRAPH -> {
        val data = response.data as Map<String, String>
        val paragraphId = data["paragraphId"] ?: error("PATCH_PARAGRAPH paragraph id")
        val patch = data["patch"] ?: error("PATCH_PARAGRAPH patch")
        tryNotifyWithExceptionLog {
          it.pathParagraph(paragraphId, patch)
        }
      }
      WsMessages.COLLABORATIVE_MODE_STATUS -> {
        val data = response.data as Map<String, Any>
        val status = data["status"] as? Boolean ?: false
        tryNotifyWithExceptionLog {
          it.updateCollaborativeModeStatus(status)
        }
      }
      WsMessages.PARAGRAPH -> {
        if (response.data == "" || response.data == emptyMap<Any, Any>()) {
          logger.info("Empty paragraph has been received")
          return
        }
        val paragraphJson = Gson().toJsonTree(response.data).asJsonObject.getAsJsonObject("paragraph")
        tryNotifyWithExceptionLog {
          it.updateCell(paragraphJson)
        }
      }
      WsMessages.PARAGRAPH_REMOVED -> {
        val paragraphJson = Gson().toJsonTree(response.data).asJsonObject

        tryNotifyWithExceptionLog {
          it.removeCell(paragraphJson)
        }
      }
      WsMessages.PARAGRAPH_MOVED -> {
        val data = response.data as Map<String, Any>
        val index = (data["index"] as Double).toInt()
        val id = data["id"].toString()

        tryNotifyWithExceptionLog {
          it.moveCell(id, index)
        }
      }
      WsMessages.PARAGRAPH_ADDED -> {
        val map = JsonParser.fromValueMap(response.data, Any::class.java)
        val paragraphJson = Gson().toJsonTree(response.data).asJsonObject.getAsJsonObject("paragraph")
        val index = JsonParser.fromValueObject(map["index"] ?: error("Ws message does not contain index"),
                                               Int::class.java)

        tryNotifyWithExceptionLog {
          it.addCell(paragraphJson, index)
        }
      }
      WsMessages.NOTES_INFO -> {
        val data = (response.data as Map<*, *>)["notes"] ?: throw Exception("Cannot parse NOTES_INFO response")
        val notebookList = ZeppelinVersionAdapter.getInstance(zeppelinInfo).parseNotebookInfos(data)

        tryNotifyWithExceptionLog {
          it.updateNotebookList(notebookList)
        }
      }
      WsMessages.PROGRESS -> {
        val progress = JsonParser.fromValueObject(response.data, Progress::class.java)

        tryNotifyWithExceptionLog {
          it.updateProgress(progress)
        }
      }
      WsMessages.ANGULAR_OBJECT_UPDATE -> {
        val angularObject = JsonParser.fromValueObject(response.data, AngularUpdateResponse::class.java)
        tryNotifyWithExceptionLog {
          it.updateAngularObject(angularObject)
        }
      }
      WsMessages.ANGULAR_OBJECT_REMOVE -> {
        val angularObject = JsonParser.fromValueObject(response.data, AngularRemoveResponse::class.java)
        tryNotifyWithExceptionLog {
          it.removeAngularObject(angularObject)
        }
      }
      WsMessages.INTERPRETER_BINDINGS -> {
        val jsonList = JsonParser.fromValueMap(response.data, Any::class.java)["interpreterBindings"]
                       ?: throw ZeppelinException("Expected interpreterBindings field in the response from the server")
        val interpreters = JsonParser.fromValueList(jsonList, Interpreter::class.java)

        tryNotifyWithExceptionLog {
          it.updateInterpreterBindings(interpreters)
        }
      }
      WsMessages.INTERPRETER_SETTINGS -> {
        val jsonList = JsonParser.fromValueMap(response.data, Any::class.java)["interpreterSettings"]
                       ?: throw ZeppelinException("Expected interpreterBindings field in the response from the server")
        val interpreters = ZeppelinVersionAdapter.getInstance(zeppelinInfo).parseInterpretersSettings(jsonList)

        tryNotifyWithExceptionLog {
          it.updateInterpreterSettings(interpreters)
        }
      }

      WsMessages.PARAGRAPH_UPDATE_OUTPUT -> {
        val paragraphOutput = JsonParser.fromValueObject(response.data, ParagraphOutput::class.java)

        tryNotifyWithExceptionLog {
          it.updateOutput(paragraphOutput, true)
        }
      }
      WsMessages.PARAGRAPH_APPEND_OUTPUT -> {
        val paragraphOutput = JsonParser.fromValueObject(response.data, ParagraphOutput::class.java)

        connectionListeners.forEach {
          it.updateOutput(paragraphOutput, false)
        }
      }
      else -> {
      }
    }
  }

  private fun tryNotifyWithExceptionLog(body: (listener: ZeppelinConnectionListener) -> Unit) = connectionListeners.forEach {
    body(it)
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}