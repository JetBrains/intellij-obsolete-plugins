/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.references.controller

import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor.GspTagMethod
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyMethodResult
import org.jetbrains.plugins.groovy.lang.resolve.api.GroovyOverloadResolver

final class GspTagMethodOverloadResolver : GroovyOverloadResolver {
  override fun compare(left: GroovyMethodResult, right: GroovyMethodResult): Int {
    return (left.element is GspTagMethod).compareTo(right.element is GspTagMethod)
  }
}
