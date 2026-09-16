package com.intellij.bigdatatools.zeppelin.psi

import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.scala.ScalaFileType
import org.jetbrains.plugins.scala.ScalaLanguage
import scala.Option
import scala.Some

class ZeppelinCustomScalaFileFactory : ZeppelinCustomPsiFileFactory {
  @Suppress("KotlinConstantConditions")
  override fun createCustomPsiFile(lang: Language, viewProvider: FileViewProvider): PsiFile? {
    if (lang != ScalaLanguage.INSTANCE) return null

    val file = ZeppelinScalaPsiFile(viewProvider, ScalaFileType.INSTANCE)

    class ModuleRef(val module: Module) : scala.ref.Reference<Module> {
      override fun apply(): Module = module
      override fun get(): Option<Module> = Some(module)
      override fun clear() {}
      override fun enqueue(): Boolean = false
      override fun isEnqueued(): Boolean = false
    }

    @Suppress("UNCHECKED_CAST")
    try {
      val clz = org.jetbrains.plugins.scala.project.`package$`::class.java.classLoader.loadClass(
        "org.jetbrains.plugins.scala.project.package\$UserDataKeys\$"
      )

      val instance = clz.fields[0].get(null) // it is always the only field MODULE$

      (instance::class.java.declaredMethods?.find { it.name == "SCALA_ATTACHED_MODULE" }?.invoke(
        instance) as? Key<scala.ref.Reference<Module>>)?.let { key ->
        ZeppelinCustomPsiFileFactory.findZeppelinModule(viewProvider.virtualFile, viewProvider.manager.project)?.let { module ->
          (file as? PsiFile)?.putUserData(key, ModuleRef(module))
        }
      }
    }
    catch (_: Exception) {
    }

    return file
  }

  override fun supports(lang: Language): Boolean = lang == ScalaLanguage.INSTANCE
}