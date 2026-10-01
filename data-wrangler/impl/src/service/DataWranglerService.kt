package com.intellij.dataWrangler.impl.service

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.executor.DataWranglerEngine
import com.intellij.dataWrangler.impl.operations.DataWranglerTransformationStepsManagerImpl
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.VisibleForTesting

private val LOG = fileLogger()

@Service(Service.Level.PROJECT)
class DataWranglerService(internal val project: Project, val coroutineScope: CoroutineScope) {

  private fun getDWEngines(): List<DataWranglerEngine<*>> {
    return DataWranglerEngine.EP.extensionList
  }

  fun createDWSession(dataContext: DataContext): DataWranglerSessionImpl<*>? {
    for (engine in getDWEngines()) {
      val session = tryCreateDWSession(engine, dataContext)
      if (session != null) {
        return session
      }
    }
    LOG.warn("No valid DW engine available")
    return null
  }

  private fun <C: DataWranglerContext> tryCreateDWSession(engine: DataWranglerEngine<C>, dataContext: DataContext): DataWranglerSessionImpl<C>? {
    val context = engine.createInitialContext(dataContext) ?: return null
    return createDWSession(engine, context)
  }

  @VisibleForTesting
  fun <C : DataWranglerContext> createDWSession(engine: DataWranglerEngine<C>, context: C): DataWranglerSessionImpl<C> {
    val transformationManager = DataWranglerTransformationStepsManagerImpl.createDefault(context)
    val previewProvider = engine.getPreviewProvider()

    return DataWranglerSessionImpl(coroutineScope, engine, transformationManager, previewProvider, context)
  }

  fun canCreateDWSession(dataContext: DataContext): Boolean {
    for (engine in getDWEngines()) {
      if (engine.canCreateDWContext(dataContext)) {
        return true
      }
    }
    return false
  }
}