package com.jetbrains.hadoop.monitoring.data.models

import com.jetbrains.bigdatatools.common.util.BdtAsyncPromise
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.LogFileInfo
import org.jetbrains.concurrency.AsyncPromise

class DataModelFs(val requestList: (String) -> List<LogFileInfo>, val requestContent: (String) -> String) {
  private var listenersLists = emptyList<FsListListener>()
  private var listenersContent = emptyList<FsContentListener>()

  private val lists = mutableMapOf<String, List<LogFileInfo>>()
  private val contents = mutableMapOf<String, String>()

  fun resetPath(path: String) = lists.remove(path)

  fun resetContent(path: String) = contents.remove(path)

  fun getListFileInfo(path: String): AsyncPromise<List<LogFileInfo>> {
    val promise = BdtAsyncPromise<List<LogFileInfo>>()

    executeOnPooledThread {
      lists[path]?.let {
        promise.setResult(it)
        return@executeOnPooledThread
      }

      try {
        val result = requestList(path)
        listenersLists.forEach { it.onListUpdate(path, result) }
        lists[path] = result
        promise.setResult(result)
      }
      catch (t: Throwable) {
        listenersLists.forEach { it.onError("Get $path failed", t) }
        promise.setError(t)
      }
    }
    return promise
  }

  fun getContent(path: String): AsyncPromise<String> {
    val promise = BdtAsyncPromise<String>()

    executeOnPooledThread {
      contents[path]?.let {
        promise.setResult(it)
        return@executeOnPooledThread
      }

      try {
        val result = requestContent(path)
        listenersContent.forEach { it.onFileContentUpdate(path, result) }
        contents[path] = result
        promise.setResult(result)
      }
      catch (t: Throwable) {
        listenersContent.forEach { it.onError("Reading $path failed", t) }
        promise.setError(t)
      }
    }
    return promise
  }

  fun addListListener(listener: FsListListener) {
    listenersLists = listenersLists + listener
  }

  fun removeListListener(listener: FsListListener) {
    listenersLists = listenersLists - listener
  }

  fun addContentListener(listener: FsContentListener) {
    listenersContent = listenersContent + listener
  }

  fun removeContentListener(listener: FsContentListener) {
    listenersContent = listenersContent - listener
  }
}

interface FsListListener {
  fun onListUpdate(path: String, list: List<LogFileInfo>)
  fun onError(msg: String, t: Throwable)
}

interface FsContentListener {
  fun onFileContentUpdate(path: String, content: String)
  fun onError(msg: String, t: Throwable)
}

