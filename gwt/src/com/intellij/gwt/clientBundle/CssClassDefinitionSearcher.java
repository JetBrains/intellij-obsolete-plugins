package com.intellij.gwt.clientBundle;

import com.intellij.gwt.clientBundle.jam.ClientBundleMethodJamElement;
import com.intellij.gwt.clientBundle.jam.CssResourceClassJamElement;
import com.intellij.gwt.clientBundle.jam.CssResourceMethodJamElement;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public final class CssClassDefinitionSearcher implements QueryExecutor<PsiElement,  PsiElement> {
  @Override
  public boolean execute(@NotNull PsiElement sourceElement, @NotNull Processor<? super PsiElement> consumer) {
    return ContainerUtil.process(getDefinitions(sourceElement), consumer);
  }

  private static @NotNull Collection<? extends PsiElement> getDefinitions(PsiElement sourceElement) {

    return ReadAction.computeBlocking(()-> {
      if (sourceElement instanceof PsiMethod method) {
        final CssResourceMethodJamElement element = CssResourceMethodJamElement.getJamElement(method);
        if (element != null) {
          return element.findCssElements();
        }
        final ClientBundleMethodJamElement clientBundleMethod = ClientBundleMethodJamElement.getElement(method);
        if (clientBundleMethod != null) {
          return clientBundleMethod.getSourceFiles(true);
        }
      }
      else if (sourceElement instanceof PsiClass) {
        final CssResourceClassJamElement element = CssResourceClassJamElement.getJamElement((PsiClass)sourceElement);
        if (element != null) {
          return element.findStylesheetFiles(true, true);
        }
      }
      return Collections.emptyList();
    });
  }
}
