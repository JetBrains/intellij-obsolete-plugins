package com.intellij.bigdatatools.zeppelin.integration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.Nls
import javax.swing.Icon

interface ZeppelinAutoImportPlacer {
  fun getName(): @Nls String

  fun getIcon(): Icon? = null

  fun isAccepted(refsContainer: PsiElement): Boolean

  fun findAnchor(refsContainer: PsiElement, callback: (PsiElement?) -> Unit)

  companion object {
    private val EP_NAME: ExtensionPointName<ZeppelinAutoImportPlacer> =
      ExtensionPointName.create("com.intellij.bigdatatools.zeppelin.zeppelinAutoImportPlacer")

    fun getImportPlacer(refsContainer: PsiElement?): ZeppelinAutoImportPlacer? {
      if (refsContainer == null) return null
      val placers = EP_NAME.extensionList.filter { it.isAccepted(refsContainer) }

      if (placers.isEmpty()) return null
      if (placers.size == 1) return placers[0]

      return ZeppelinPopupPlacer(placers)
    }
  }
}
