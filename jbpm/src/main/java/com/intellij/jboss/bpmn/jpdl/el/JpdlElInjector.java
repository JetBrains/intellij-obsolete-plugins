package com.intellij.jboss.bpmn.jpdl.el;

import com.intellij.javaee.el.impl.ELLanguage;
import com.intellij.javaee.el.impl.ELUtil;
import com.intellij.javaee.el.providers.ELContextProvider;
import com.intellij.jboss.bpmn.jpdl.model.JpdlDomModelManager;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.jsp.JspFile;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class JpdlElInjector implements MultiHostInjector {
  @Override
  public void getLanguagesToInject(@NotNull final MultiHostRegistrar registrar, @NotNull final PsiElement host) {
    final PsiElement originalElement = host.getOriginalElement();
    // operate only in jpdl xml files
    final PsiFile psiFile = originalElement.getContainingFile();
    if (psiFile instanceof XmlFile && !(psiFile instanceof JspFile) && isJpdlFile((XmlFile)psiFile)) {
      final List<TextRange> ranges = ELUtil.getELTextRanges(originalElement, "#{", "}");
      if (ranges.size() > 0) {
        for (TextRange textRange : ranges) {
          registrar.startInjecting(ELLanguage.INSTANCE)
            .addPlace(null, null, (PsiLanguageInjectionHost)originalElement, textRange)
            .doneInjecting();
        }
        originalElement.putUserData(ELContextProvider.ourContextProviderKey, new JpdlElContextProvider(originalElement));
      }
    }
  }

  @Override
  @NotNull
  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Arrays.asList(XmlAttributeValue.class, XmlText.class);
  }

  private static boolean isJpdlFile(@NotNull final XmlFile xmlFile) {
    Project project = xmlFile.getProject();
    if (project.isDefault()) return false;
    return JpdlDomModelManager.getInstance(project).isJpdl(xmlFile);
  }
}
