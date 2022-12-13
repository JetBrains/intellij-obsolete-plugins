/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.intellij.seam;

import com.intellij.javaee.el.impl.ELLanguage;
import com.intellij.javaee.el.providers.ELContextProvider;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.jsp.JspFile;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlText;
import com.intellij.seam.el.SeamELContextProvider;
import com.intellij.seam.el.SeamELInjectorUtil;
import com.intellij.seam.el.SeamElFileProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author peter
 */
public class SeamElXmlConfigProvider implements MultiHostInjector {

  private static boolean isSeamElContainerFile(@NotNull final XmlFile xmlFile) {
    for (SeamElFileProvider fileProvider : SeamElFileProvider.EP_NAME.getExtensionList()) {
      if (fileProvider.isSeamElContainer(xmlFile)) return true;
    }
    return false;
  }

  @Override
  public void getLanguagesToInject(@NotNull final MultiHostRegistrar registrar, @NotNull final PsiElement host) {
    final PsiElement originalElement = host.getOriginalElement();
    // operate only in seam xml config files
    final PsiFile psiFile = originalElement.getContainingFile();
    if (psiFile.getProject().isDefault()) return;
    if (psiFile instanceof XmlFile && !(psiFile instanceof JspFile) && isSeamElContainerFile((XmlFile)psiFile)) {
      final List<TextRange> ranges = SeamELInjectorUtil.getELTextRanges(originalElement);
      if (ranges.size() > 0) {
        for (TextRange textRange : ranges) {
          registrar.startInjecting(ELLanguage.INSTANCE)
            .addPlace(null, null, (PsiLanguageInjectionHost)originalElement, textRange)
            .doneInjecting();
        }
        originalElement.putUserData(ELContextProvider.ourContextProviderKey, new SeamELContextProvider(originalElement));
      }
    }
  }

  @Override
  @NotNull
  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Arrays.asList(XmlAttributeValue.class, XmlText.class);
  }
}
