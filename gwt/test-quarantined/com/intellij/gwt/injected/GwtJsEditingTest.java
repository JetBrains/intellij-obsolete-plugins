package com.intellij.gwt.injected;

import com.intellij.codeInsight.injected.InjectedLanguageTestCase;
import com.intellij.gwt.jsinject.parser.GwtLanguageDialect;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.actions.DeleteAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class GwtJsEditingTest extends InjectedLanguageTestCase {
  public void testChangeParamListInJS() {
    configureByFile("/JsniEmptyMethod.java");
    PsiElement element = getFile().findElementAt(getEditor().getCaretModel().getOffset());
    PsiFile psiFile = element.getContainingFile();
    assertEquals(GwtLanguageDialect.GWT_DIALECT, psiFile.getLanguage());
    element = InjectedLanguageManager.getInstance(psiFile.getProject()).getInjectionHost(psiFile);
    assertEquals(JavaLanguage.INSTANCE, element.getLanguage());
    PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    assertTrue(method.hasModifierProperty(PsiModifier.NATIVE));
    int typeOffset = method.getParameterList().getParameters()[0].getTypeElement().getTextRange().getStartOffset();
    bringRealEditorBack();
    getEditor().getCaretModel().moveToOffset(typeOffset);

    WriteCommandAction.writeCommandAction(getProject()).run(() -> DeleteAction.deleteCharAtCaret(getEditor()));


    doHighlighting();
  }

  public void testEditPrefix() {
    configureByFile("/JsniEmptyMethod.java");
    PsiElement element = getFile().findElementAt(getEditor().getCaretModel().getOffset());
    PsiFile psiFile = element.getContainingFile();
    assertEquals(GwtLanguageDialect.GWT_DIALECT, psiFile.getLanguage());
    element = InjectedLanguageManager.getInstance(psiFile.getProject()).getInjectionHost(psiFile);
    assertEquals(JavaLanguage.INSTANCE, element.getLanguage());
    PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    assertTrue(method.hasModifierProperty(PsiModifier.NATIVE));
    int nameOffset = method.getNameIdentifier().getTextRange().getStartOffset();
    bringRealEditorBack();
    getEditor().getCaretModel().moveToOffset(nameOffset);

    type('x');

    doHighlighting();
  }

  @Override
  protected String getBasePath() {
    return PathManager.getHomePath() + "/plugins/GwtStudio/testData";
  }

  @NotNull
  @Override
  protected String getTestDataPath() {
    return getBasePath() + "/jsni/injection/";
  }
}
