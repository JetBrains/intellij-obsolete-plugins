/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.api

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import org.jetbrains.plugins.grails.structure.GrailsApplication

interface GrailsViewNodeProvider {

  fun createNodes(application: GrailsApplication, settings: ViewSettings): Collection<AbstractTreeNode<*>>
}
