package com.intellij.dbt.test

import com.intellij.dbt.DbtIcons
import com.intellij.dbt.projectView.DbtIconProvider
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager

class DbtIconProviderTest : DbtTestCase() {
  fun testHasIconForDbtProject() {
    val directoryVirtualFile = myFixture.project.guessProjectDir()!!
    val directory = PsiManager.getInstance(myFixture.project).findDirectory(directoryVirtualFile)!!
    val iconProvider = DbtIconProvider()
    val icon = iconProvider.getIcon(directory, 0)
    assertEquals(DbtIcons.Dbt, icon)
  }

  fun testNoIconFor_dbt_packages_Folder() {
    val psiFile = myFixture.configureByFile("subdirectories/dbt_packages/dbt_project.yml")
    val directory = psiFile.parent!!
    val iconProvider = DbtIconProvider()
    val icon = iconProvider.getIcon(directory, 0)
    assertNull(icon)
  }
}