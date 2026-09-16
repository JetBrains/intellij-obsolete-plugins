package com.intellij.bigdatatools.plugin.spark.scala.submit.gutter

import com.intellij.bigdatatools.plugin.spark.java.submit.gutter.SparkRunLineMarkerContributorJvm
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.util.ScalaMainMethodUtil

class SparkRunLineMarkerContributorScala : SparkRunLineMarkerContributorJvm() {
  override fun findMainClass(element: PsiElement): String? {
    for (ancestor in generateSequence(element) { it.parent }) {
      when {
        ancestor is ScFunctionDefinition && ScalaMainMethodUtil.isScala3MainMethod(ancestor) -> {
          val packageName = ScalaPsiUtil.getPlacePackageName(ancestor as PsiElement)
          if (packageName.isNullOrEmpty()) {
            return ancestor.name
          }
          else {
            return packageName + "." + ancestor.name
          }
        }
        ancestor is ScObject && ScalaMainMethodUtil.hasScala2MainMethod(ancestor) -> {
          return ancestor.qualifiedName()
        }
        ancestor is PsiFile -> {
          break
        }
      }
    }
    return null
  }
}