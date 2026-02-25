// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspGroovyFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.GspPageSkeleton;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspAttributeValue;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;
import org.jetbrains.plugins.groovy.GroovyLanguage;

public final class GspTagVariableDeclarationSearcher extends PomDeclarationSearcher {

  @Override
  public void findDeclarationsAt(@NotNull PsiElement element, int offsetInElement, @NotNull Consumer<? super PomTarget> consumer) {
    if (!(element instanceof GspAttributeValue)) return;
    PsiElement attribute = element.getParent();
    if (!(attribute instanceof XmlAttribute)) return;

    PsiElement tag = attribute.getParent();

    if (!(tag instanceof GspGrailsTag)) return;

    String attributeName = ((XmlAttribute)attribute).getName();
    boolean isStatus = attributeName.equals("status");
    if (!isStatus) {
      if (!attributeName.equals("var")) return;
    }

    String tagName = ((GspTag)tag).getName();
    boolean isEach = tagName.equals("g:each");

    if (!isEach && (isStatus || !tagName.equals("g:set"))) return;

    PsiFile file = tag.getContainingFile();
    if (!(file instanceof GspFile)) return;

    GspGroovyFile groovyFile = (GspGroovyFile)file.getViewProvider().getPsi(GroovyLanguage.INSTANCE);
    assert groovyFile != null;

    GspPageSkeleton skeleton = groovyFile.getSkeleton();

    PsiVariable variable;

    if (isEach) {
      GspPageSkeleton.EachTagDescription descr = skeleton.getEachTagDescription((GspTag)tag);
      if (descr == null) return;

      variable = isStatus ? descr.getStatusVariable() : descr.getVarVariable();
    }
    else {
      variable = skeleton.getVariableByDefTag((GspTag)tag);
    }

    if (variable == null) return;

    consumer.consume(variable);
  }
}
