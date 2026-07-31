package com.intellij.gwt.inspections;

import com.intellij.codeInsight.navigation.PsiTargetNavigator;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.uiBinder.declarations.UiStyleElement;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.css.CssElementFactory;
import com.intellij.psi.css.CssRuleset;
import com.intellij.psi.css.CssStylesheet;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;

public class CreateCssClassLocalQuickFix extends LocalQuickFixAndIntentionActionOnPsiElement {
  private static final Logger LOG = Logger.getInstance(CreateCssClassLocalQuickFix.class);

  private final SmartPsiElementPointer<PsiElement> myPsiElementPointer;
  private final String myClassName;

  public CreateCssClassLocalQuickFix(@NotNull StylesheetFile stylesheetFile, String className) {
    this((PsiElement) stylesheetFile, className);
  }

  CreateCssClassLocalQuickFix(@NotNull XmlTag xmlTag, String className) {
    this((PsiElement) xmlTag, className);
  }

  private CreateCssClassLocalQuickFix(@NotNull PsiElement psiElement, String className) {
    super(psiElement);
    myPsiElementPointer = SmartPointerManager.getInstance(psiElement.getProject()).createSmartPsiElementPointer(psiElement);
    myClassName = className;
  }

  @Override
  public @NotNull String getText() {
    return GwtBundle.message("quickfix.name.create.css.class.0", myClassName);
  }

  @Override
  public @NotNull String getFamilyName() {
    return GwtBundle.message("quickfix.family.name.create.css.class");
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile psiFile,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {
    PsiElement psiElement = myPsiElementPointer.getElement();
    if (psiElement == null) {
      return;
    }
    if (psiElement instanceof XmlTag xmlTag) {
      List<StylesheetFile> stylesheetFiles = new UiStyleElement(xmlTag).getStylesheetFiles();
      if (stylesheetFiles.isEmpty()) {
        return;
      }
      if (stylesheetFiles.size() == 1) {
        applyFix(stylesheetFiles.get(0), project);
      }
      else if (editor != null) {
        StylesheetFile[] cssFilesArray = stylesheetFiles.toArray(StylesheetFile.EMPTY_ARRAY);
        new PsiTargetNavigator<>(cssFilesArray).navigate(editor, GwtBundle.message("popup.title.choose.css.file"), element -> {
          runWriteCommandAction(project, () -> applyFix(element, project));
          return true;
        });
      }
    }
    else if (psiElement instanceof StylesheetFile) {
      applyFix((StylesheetFile)psiElement, project);
    }
    else {
      LOG.error("StylesheetFile or XmlTag expected. Found: " + psiElement);
    }
  }

  private void applyFix(@NotNull StylesheetFile cssFile, @NotNull Project project) {
    VirtualFile virtualFile = cssFile.getOriginalFile().getVirtualFile();
    if (virtualFile == null) {
      return;
    }

    if (ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(Collections.singletonList(virtualFile)).hasReadonlyFiles()) {
      return;
    }

    try {
      CssStylesheet stylesheet = cssFile.getStylesheet();
      CssRuleset cssRuleset = CssElementFactory.getInstance(project).createRuleset("." + myClassName + "{\n\n}\n", cssFile.getLanguage());
      stylesheet.addRuleset(cssRuleset);
      final CssRuleset[] rulesets = stylesheet.getRulesets();
      final CssRuleset added = rulesets[rulesets.length - 1];
      Navigatable descriptor =
        PsiNavigationSupport.getInstance().createNavigatable(project, virtualFile, added.getBlock().getTextOffset());
      descriptor.navigate(true);
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }
}
