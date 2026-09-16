package com.intellij.bigdatatools.zeppelin.integration

import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.psi.PsiElement

class ZeppelinCurrentCellPlacer : ZeppelinAutoImportPlacer {
  override fun isAccepted(refsContainer: PsiElement): Boolean =
    ZeppelinAutoImportUtil.findCurrentCell(refsContainer) != ZeppelinAutoImportUtil.findFirstCell(refsContainer)

  override fun findAnchor(refsContainer: PsiElement, callback: (PsiElement?) -> Unit) {
    val cell = ZeppelinAutoImportUtil.findCurrentCell(refsContainer)
    val psiElement = cell?.let { ZeppelinAutoImportUtil.findSourceStart(it, refsContainer.language) }
    callback(psiElement)
  }

  override fun getName() = ZepMessagesBundle.message("popup.placer.current.cell")
}

class ZeppelinFirstCellPlacer : ZeppelinAutoImportPlacer {
  override fun isAccepted(refsContainer: PsiElement): Boolean = true

  override fun findAnchor(refsContainer: PsiElement, callback: (PsiElement?) -> Unit) {
    callback(
      ZeppelinAutoImportUtil.findFirstCell(refsContainer)?.let {
        ZeppelinAutoImportUtil.findSourceStart(it, refsContainer.language)
      }
    )
  }

  override fun getName() = ZepMessagesBundle.message("popup.placer.first.cell")
}