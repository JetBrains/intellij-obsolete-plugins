// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.GspDirectiveKind;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterHtmlElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspScriptletTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttribute;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspXmlRootTag;

public final class GspPsiElementFactoryImpl extends GspPsiElementFactory {

  Project myProject;

  public GspPsiElementFactoryImpl(Project project) {
    myProject = project;
  }

  @Override
  public GspDirective createDirectiveByKind(GspDirectiveKind kind) {
    return createElementFromText("<%@ " + StringUtil.toLowerCase(kind.toString()) + " %>");
  }

  @Override
  public GspDirectiveAttribute createDirectiveAttribute(@NotNull String name, @NotNull String value) {
    GspDirective gspDirective = createElementFromText("<%@ " + "page " + name + "=" + "\"" + value + "\" %>");
    return ((GspDirectiveAttribute) gspDirective.getAttribute(name));
  }

  @Override
  public GspScriptletTag createScriptletTagFromText(String s) {
    return createElementFromText("<%" + s + "%>");
  }

  @Override
  public GspOuterHtmlElement createOuterHtmlElement(String text) {
    return createElementFromText(text);
  }

  private GspFile createDummyFile(String s) {
    return (GspFile) PsiFileFactory.getInstance(myProject).createFileFromText("DUMMY__." + GspFileType.GSP_FILE_TYPE.getDefaultExtension(), s);
  }

  @Override
  public <T> T createElementFromText(String text) {
    GspFile psiFile = createDummyFile(text);
    GspXmlRootTag rootTag = psiFile.getRootTag();
    assert rootTag != null;
    return (T)rootTag.getFirstChild();
  }

}
