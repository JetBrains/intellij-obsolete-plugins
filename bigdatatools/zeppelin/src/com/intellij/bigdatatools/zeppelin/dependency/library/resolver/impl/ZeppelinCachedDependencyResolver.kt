package com.intellij.bigdatatools.zeppelin.dependency.library.resolver.impl

import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.dependency.library.resolver.ZeppelinDependencyResolver
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.util.BdtAsyncPromise
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.isPending

class ZeppelinCachedDependencyResolver(private val baseResolver: ZeppelinDependencyResolver) : ZeppelinDependencyResolver by baseResolver {
  private val promises: MutableMap<PromiseKey, AsyncPromise<List<OrderRoot>>> = mutableMapOf()

  init {
    Disposer.register(this, baseResolver)
  }

  override fun dispose() = promises.clear()

  override fun resolveRoots(project: Project?,
                            dep: ZepDependency,
                            repos: List<Repository>,
                            rootType: OrderRootType,
                            force: Boolean,
                            indicator: ProgressIndicator?) =
    getAsyncPromise(project, dep, repos, rootType, force, indicator).get() ?: emptyList()

  private fun getAsyncPromise(project: Project?,
                              dep: ZepDependency,
                              repos: List<Repository>,
                              rootType: OrderRootType,
                              force: Boolean,
                              indicator: ProgressIndicator?): AsyncPromise<List<OrderRoot>> {
    val promiseKey = PromiseKey(dep, rootType)
    val promise: AsyncPromise<List<OrderRoot>> = synchronized(this) {
      val existsPromise = promises[promiseKey]
      when {
        existsPromise?.isPending == true -> return@synchronized existsPromise
        existsPromise?.isDone == true && (existsPromise.get()?.isNotEmpty() == true || !force) -> return existsPromise
        else -> {
          val newPromise = BdtAsyncPromise<List<OrderRoot>>()
          promises[promiseKey] = newPromise
          return@synchronized newPromise
        }
      }
    }

    try {
      val result = baseResolver.resolveRoots(project, dep, repos, rootType, force, indicator)
      promise.setResult(result)
    }
    catch (t: Throwable) {
      promise.setError(t)

      //It can be linked with poor connection so we do not need put bad results to map
      synchronized(this) {
        promises -= promiseKey
      }
    }
    return promise
  }

  private data class PromiseKey(val dep: ZepDependency, val type: OrderRootType)
}