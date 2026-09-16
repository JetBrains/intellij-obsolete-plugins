package com.intellij.bigdatatools.zeppelin.refactoring

import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.ide.util.ClassFilter
import com.intellij.ide.util.TreeJavaClassChooserDialog
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeVisitor
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiVariable
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.extractMethod.ExtractMethodDialog
import com.intellij.refactoring.extractMethod.InputVariables
import com.intellij.refactoring.ui.MethodSignatureComponent
import com.intellij.ui.SeparatorFactory
import com.intellij.ui.TabbedPaneWrapper
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.scala.ScalaFileType
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.project.ScalaFeatures
import scala.Option
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ZeppelinExtractToMethodHandler(private val parentAction: ZeppelinExtractToMethodAction) : ZeppelinExtractHandlerBase() {

  inner class ZeppelinExtractMethodDialog(project: Project,
                                          private val selectedClass: PsiClass,
                                          inputVars: List<PsiVariable>,
                                          factory: PsiElementFactory) :
    ExtractMethodDialog(project,
                        selectedClass,
                        InputVariables(inputVars, project, LocalSearchScope.EMPTY, false, emptySet()),
                        PsiTypes.voidType(),
                        factory.createTypeParameterList(),
                        emptyArray<PsiType>(),
                        false, false, false,
                        ZepMessagesBundle.message("zeppelin.extract.method.declare.method"), "", null, emptyArray(), null) {

    private var scalaSignaturePanel: MethodSignatureComponent? = null

    override fun getSignature(): String {
      return StringBuilder(visibilityToString()).append("def ").append(chosenMethodName).append("(").append(
        chosenParameters.filter { it.isPassAsParameter }.joinToString(separator = ", ") { "${it.name}: ${it.type.presentableText}" }
      ).append(")").toString()
    }

    override fun createSignaturePanel(): JComponent {
      @Suppress("CAST_NEVER_SUCCEEDS") // need this cast to prevent false red code
      val scalaSignaturePanel = MethodSignatureComponent("", project, ScalaFileType.INSTANCE as FileType).apply {
        preferredSize = JBUI.size(500, 100)
        minimumSize = JBUI.size(500, 100)
      }
      this.scalaSignaturePanel = scalaSignaturePanel

      val panel = JPanel(BorderLayout()).apply {
        add(SeparatorFactory.createSeparator(RefactoringBundle.message("signature.preview.border.title"), scalaSignaturePanel), BorderLayout.NORTH)
        add(scalaSignaturePanel, BorderLayout.CENTER)
      }

      updateSignature()
      return panel
    }

    override fun updateSignature() {
      scalaSignaturePanel?.setSignature(signature)
    }

    fun visibilityToString(): String = when (val v = visibility) {
      PsiModifier.PACKAGE_LOCAL -> "private[${selectedClass.getPackage()?.name}] "
      PsiModifier.PUBLIC -> ""
      else -> "$v "
    }
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
    val classFilter = ClassFilter { it is ScTemplateDefinition } // can't inline this probably because of Kotlin compiler bug

    extractInfoInner(editor, file)?.let { fileInfo ->
      val initialSelectedClass = parentAction.initialSelectionClass

      val classChooser = object : TreeJavaClassChooserDialog(
        ZepMessagesBundle.message("zeppelin.extract.method.parent.class"),
        project,
        GlobalSearchScope.allScope(project),
        classFilter,
        initialSelectedClass
      ) {
        override fun createCenterPanel(): JComponent? = super.createCenterPanel().also { comp ->
          if (initialSelectedClass != null)
            (comp as? TabbedPaneWrapper.TabbedPaneHolder)?.tabbedPaneWrapper?.let { paneWrapper ->
              if (paneWrapper.tabCount > 1) paneWrapper.selectedIndex = 1
            }
        }
      }

      classChooser.showDialog()

      val selectedClass = classChooser.selected ?: return

      parentAction.initialSelectionClass = selectedClass

      val factory = PsiElementFactory.getInstance(project)

      val inputVars = fileInfo.outerVarsUsed.map {
        factory.createVariableDeclarationStatement(it.name, MyStubPsiType(it.tpe), null, file)
      }.map { declaration -> declaration.declaredElements[0] as PsiVariable }

      val dialog = ZeppelinExtractMethodDialog(project, selectedClass, inputVars, factory)
      if (!dialog.showAndGet()) return

      val outerVarsChosen = dialog.chosenParameters.filter { it.isPassAsParameter }.map { it.name }.toSet()

      insertMethodWithImports(fileInfo, outerVarsChosen, dialog.chosenMethodName, dialog.visibilityToString(), selectedClass)

      ZeppelinExtractRefactoringUtil.postProcessFile(
        selectedClass.containingFile.virtualFile,
        selectedClass.containingFile,
        project,
        dataContext
      )
    }
  }

  private fun insertMethodWithImports(fileText: ZeppelinExtractRefactoringUtil.ExtractedJobInfo, outerNamesInSignature: Set<String>,
                                      name: String, visibility: String, target: PsiClass) {
    val project = target.project
    val scalaFeatures = ScalaFeatures.forPsiOrDefault(target)
    val psiMethod = ScalaPsiElementFactory.createMethodFromText(
      ZeppelinExtractRefactoringUtil.createMethodText(fileText, outerNamesInSignature, name, visibility), scalaFeatures, project
    )
    val imports = if (fileText.importsBlock.isNotEmpty()) fileText.importsBlock.split('\n').map { imp ->
      ScalaPsiElementFactory.createImportFromText(imp, target, null)
    }.filterNotNull()
    else emptyList()

    CommandProcessor.getInstance().executeCommand(
      project, {
      runWriteAction {
        imports.forEach {
          val anchor = (target.containingFile as ScalaFile).typeDefinitions().head() as PsiElement
          anchor.parent.addBefore(it as PsiElement, anchor)
        }

        (target as ScTemplateDefinition).addMember(psiMethod, Option.empty())
      }
    }, null, null
    )
  }

  private fun PsiClass.getPackage(): PsiPackage? {
    val name = (containingFile as? PsiClassOwner)?.packageName ?: return null
    return JavaPsiFacade.getInstance(containingFile.project).findPackage(name)
  }

  private class MyStubPsiType(val commonName: String) : PsiType(emptyArray()) {
    override fun getPresentableText(): String = commonName

    override fun getCanonicalText(): String = commonName

    override fun isValid(): Boolean = true

    override fun equalsToText(text: String): Boolean = commonName == text

    override fun <A : Any?> accept(visitor: PsiTypeVisitor<A>): A = visitor.visitType(this)

    override fun getResolveScope(): GlobalSearchScope? = null

    override fun getSuperTypes(): Array<PsiType> = emptyArray()
  }
}