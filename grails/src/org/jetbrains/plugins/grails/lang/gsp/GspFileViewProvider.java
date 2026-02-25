// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.addins.js.JavaScriptIntegrationUtil;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.GspGroovyFileImpl;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.impl.GspHtmlFileImpl;
import org.jetbrains.plugins.groovy.GroovyLanguage;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class GspFileViewProvider extends MultiplePsiFilesPerDocumentFileViewProvider implements TemplateLanguageFileViewProvider{
  private static final Set<Language> LANGUAGES =
    new LinkedHashSet<>(Arrays.asList(GspLanguage.INSTANCE, GroovyLanguage.INSTANCE, HTMLLanguage.INSTANCE));

  public GspFileViewProvider(PsiManager manager,
                             VirtualFile virtualFile,
                             boolean physical) {
    super(manager, virtualFile, physical);
  }

  @Override
  public @NotNull Language getBaseLanguage() {
    return GspLanguage.INSTANCE;
  }

  @Override
  public @NotNull Set<Language> getLanguages() {
    return LANGUAGES;
  }

  @Override
  public @NotNull Language getTemplateDataLanguage() {
    return HTMLLanguage.INSTANCE;
  }

  @Override
  protected @NotNull MultiplePsiFilesPerDocumentFileViewProvider cloneInner(final @NotNull VirtualFile copy) {
    return new GspFileViewProvider(getManager(), copy, false);
  }

  @Override
  protected PsiFile createFile(@NotNull Language language) {
    if (language == getBaseLanguage()) {
      ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language);
      assert parserDefinition != null;
      return parserDefinition.createFile(this);
    }
    if (language == GroovyLanguage.INSTANCE) {
      return new GspGroovyFileImpl(this);
    }
    if (language == HTMLLanguage.INSTANCE) {
      return new GspHtmlFileImpl(this);
    }
    return super.createFile(language);
  }

  @Override
  public PsiElement findElementAt(int offset, @NotNull Class<? extends Language> lang) {
    PsiElement ret = null;
    PsiFile mainRoot = getPsi(getBaseLanguage());
    PsiElement elementInBaseRoot = findElementByLanguage(offset, lang, ret, getBaseLanguage());
    if (isMeaningfulElement(elementInBaseRoot)) {
      return elementInBaseRoot;
    }
    for (Language language : LANGUAGES) {
      PsiElement found = findElementByLanguage(offset, lang, ret, language);
      if (ret == null || getPsi(language) != mainRoot) {
        ret = found;
      }
    }
    return ret;
  }

  private PsiElement findElementByLanguage(int offset, Class<? extends Language> lang, PsiElement ret, Language language) {
    if (!ReflectionUtil.isAssignable(lang, language.getClass())) return ret;
    if (lang.equals(Language.class) && !LANGUAGES.contains(language)) return ret;
    final PsiFile psiRoot = getPsi(language);
    final PsiElement psiElement = findElementAt(psiRoot, offset);
    if (psiElement == null || (psiElement instanceof OuterLanguageElement && !isMeaningfulElement(psiElement)))
      return ret;
    if (ret == null) {
      ret = psiElement;
    }
    return ret;
  }

  private static boolean isMeaningfulElement(PsiElement elementInBaseRoot) {
    return JavaScriptIntegrationUtil.isJavaScriptInjection(elementInBaseRoot);
  }
}
