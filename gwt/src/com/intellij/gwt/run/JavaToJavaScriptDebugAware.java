package com.intellij.gwt.run;

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.javascript.debugger.ExpressionInfoFactory;
import com.intellij.javascript.debugger.NameMapper;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import com.intellij.xdebugger.evaluation.ExpressionInfo;
import com.jetbrains.javascript.debugger.JavaScriptDebugAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;
import org.jetbrains.debugger.MemberFilter;
import org.jetbrains.debugger.MemberFilterWithNameMappings;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaToJavaScriptDebugAware extends JavaScriptDebugAware {
  private static final Pattern POSTFIX_PATTERN = Pattern.compile("_\\d+_g\\$$");

  @Override
  public @NotNull Class<? extends XLineBreakpointType<?>> getBreakpointTypeClass() {
    return JavaLineBreakpointType.class;
  }

  @Override
  public @Nullable MemberFilter createMemberFilter(final @Nullable NameMapper nameMapper, @NotNull PsiElement element, int end) {
    if (nameMapper == null || element.getContainingFile().getLanguage() != JavaLanguage.INSTANCE) {
      return null;
    }

    PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
    if (method == null) {
      return null;
    }

    for (PsiParameter parameter : method.getParameterList().getParameters()) {
      nameMapper.map(parameter);
    }

    PsiCodeBlock body = method.getBody();
    if (body != null) {
      body.accept(new JavaRecursiveElementVisitor() {
        @Override
        public void visitLocalVariable(@NotNull PsiLocalVariable variable) {
          nameMapper.map(variable);
        }
      });
    }

    Map<String, String> rawNameToSource = nameMapper.getRawNameToSource();
    return rawNameToSource == null ? null : new MemberFilterWithNameMappings(rawNameToSource);
  }

  @Override
  public @Nullable PsiElement getNavigationElementForSourcemapInspector(@NotNull PsiFile file) {
    if (file instanceof PsiJavaFile) {
      PsiClass[] classes = ((PsiJavaFile)file).getClasses();
      if (classes.length > 0) {
        return classes[0].getNavigationElement();
      }
    }
    return null;
  }

  @Override
  protected @NotNull LanguageFileType getFileType() {
    return JavaFileType.INSTANCE;
  }

  @Override
  public @NotNull Promise<ExpressionInfo> getEvaluationInfo(@NotNull PsiElement element, @NotNull Document document, @NotNull ExpressionInfoFactory expressionInfoFactory) {
    if (element instanceof PsiIdentifier) {
      return expressionInfoFactory.create(element, document);
    }
    return Promises.resolvedPromise(null);
  }

  @Override
  public @NotNull String normalizeMemberName(@NotNull String name) {
    Matcher matcher = POSTFIX_PATTERN.matcher(name);
    if (matcher.find()) {
      return name.substring(0, matcher.start());
    }
    else {
      return name;
    }
  }
}