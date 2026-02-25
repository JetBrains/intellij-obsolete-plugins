// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api;

import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.GspDirectiveKind;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspXmlRootTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.impl.GspHtmlFileImpl;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement;

import java.util.List;

public interface GspFile extends XmlFile {

  GroovyFileBase getGroovyLanguageRoot();

  List<GspDirective> getDirectiveTags(GspDirectiveKind directiveKind, boolean searchInIncludes);

  void addImportForClass(PsiClass aClass) throws IncorrectOperationException;

  void addImportStatement(GrImportStatement statement);

  PsiElement createGroovyScriptletFromText(String text) throws IncorrectOperationException;

  @Override
  GspXmlRootTag getRootTag();

  @Override
  @NotNull
  FileViewProvider getViewProvider();

  boolean processJsInJavascriptTags(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, @NotNull PsiElement place);

  GspHtmlFileImpl getHtmlLanguageRoot();
}
