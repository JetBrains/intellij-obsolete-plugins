/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.artefact.api

import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.plugins.grails.artefact.impl.ControllerArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.DomainArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.FilterArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.InterceptorArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.ServiceArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.TaglibArtefactHandler

private val EP_NAME = ExtensionPointName<GrailsArtefactHandler>("org.intellij.grails.artefactHandler")

private val namedHandlers = sequenceOf<GrailsArtefactHandler>(
  DomainArtefactHandler,
  ControllerArtefactHandler,
  ServiceArtefactHandler,
  TaglibArtefactHandler,
  InterceptorArtefactHandler,
  FilterArtefactHandler
)

@Service(Service.Level.APP)
internal class HandlerCache {
  val idToHandler by lazy(LazyThreadSafetyMode.NONE) {
    allHandlers.associateBy { it.artefactHandlerID }
  }

  val annotationToHandler: Map<String, GrailsArtefactHandler> by lazy(LazyThreadSafetyMode.NONE) {
    allHandlers.associateByM { it.annotationFqns }
  }
}

internal val allHandlers: Sequence<GrailsArtefactHandler>
  get() {
    return namedHandlers.plus(EP_NAME.extensionList)
  }

internal val displayableArtefactHandlers: Sequence<GrailsDisplayableArtefactHandler>
  get() {
    return allHandlers.filterIsInstance(GrailsDisplayableArtefactHandler::class.java)
  }

private inline fun <T, K> Sequence<T>.associateByM(keySelector: (T) -> Iterable<K>): Map<K, T> {
  val result = mutableMapOf<K, T>()
  for (handler in this) {
    for (fqn in keySelector(handler)) {
      result[fqn] = handler
    }
  }
  return result
}