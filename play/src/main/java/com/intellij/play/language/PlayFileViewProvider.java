/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.play.language.groovy.PlayGroovyFileImpl;
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.templateLanguages.ConfigurableTemplateLanguageFileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.GroovyLanguage;

import java.util.Set;

public class PlayFileViewProvider extends MultiplePsiFilesPerDocumentFileViewProvider
  implements ConfigurableTemplateLanguageFileViewProvider {

  public PlayFileViewProvider(final PsiManager manager, final VirtualFile virtualFile, final boolean physical) {
    super(manager, virtualFile, physical);
  }

  @Override
  @NotNull
  public Language getBaseLanguage() {
    return PlayLanguage.INSTANCE;
  }

  @Override
  @NotNull
  public Set<Language> getLanguages() {
    return Set.of(getBaseLanguage(), getTemplateDataLanguage(), getGroovyLanguage());
  }

  @NotNull
  @Override
  protected PlayFileViewProvider cloneInner(@NotNull final VirtualFile copy) {
    return new PlayFileViewProvider(getManager(), copy, false);
  }

  @Override
  @NotNull
  public Language getTemplateDataLanguage() {
    return HTMLLanguage.INSTANCE;
  }

  @Override
  @Nullable
  protected PsiFile createFile(@NotNull final Language lang) {
    if (lang == getBaseLanguage()) {
      return LanguageParserDefinitions.INSTANCE.forLanguage(lang).createFile(this);
    }

    if (lang == getTemplateDataLanguage()) {
      final PsiFileImpl file = (PsiFileImpl)LanguageParserDefinitions.INSTANCE.forLanguage(lang).createFile(this);
      file.setContentElementType(PlayFileElementTypes.TEMPLATE_DATA);
      return file;
    }

    if (lang == getGroovyLanguage()) {
      return new PlayGroovyFileImpl(this);
    }

    return null;
  }

  public Language getGroovyLanguage() {
    return GroovyLanguage.INSTANCE;
  }
}
