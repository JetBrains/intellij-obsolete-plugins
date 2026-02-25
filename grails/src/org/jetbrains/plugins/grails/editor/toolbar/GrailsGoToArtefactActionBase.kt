/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.editor.toolbar

import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiClass
import org.jetbrains.plugins.grails.GrailsBundle
import org.jetbrains.plugins.grails.actions.ArtefactData
import org.jetbrains.plugins.grails.util.GrailsArtifact
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition
import javax.swing.Icon

abstract class GrailsGoToArtefactActionBase(private val artefactType: GrailsArtifact) : GrailsToolbarTargetAction<PsiClass>() {

  @NlsSafe override fun getTitle(artefactData: ArtefactData): String {
    return artefactData.artefactName.capitalize() + artefactType.suffix
  }

  override fun getNavigateTargets(artefactData: ArtefactData): MutableCollection<GrClassDefinition> = artefactType.getInstances(
      artefactData.module, artefactData.packageName, artefactData.artefactName
  )


  @ActionText override fun getNavigateTitle(target: PsiClass): String =
    GrailsBundle.message("action.text.go.to.artefact", target.name)

  override fun getNavigateIcon(target: PsiClass): Icon? = artefactType.icon

  override fun navigate(artefactData: ArtefactData, target: PsiClass): Unit = target.navigate(true)
}
