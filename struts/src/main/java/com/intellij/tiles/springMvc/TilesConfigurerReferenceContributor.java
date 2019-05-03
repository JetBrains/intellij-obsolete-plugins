/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.tiles.springMvc;

import com.intellij.patterns.ElementPattern;
import com.intellij.psi.*;
import com.intellij.spring.model.utils.resources.SpringResourcesBuilder;
import com.intellij.spring.model.utils.resources.SpringResourcesUtil;
import com.intellij.spring.web.SpringWebConstants;
import com.intellij.spring.web.mvc.SpringMvcLibraryUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PsiJavaPatterns.*;
import static com.intellij.patterns.StandardPatterns.string;
import static com.intellij.tiles.springMvc.TilesConfigurerDefinitionsConverter.TILES_XML_CONDITION;

/**
 * {@code TilesConfigurer#setDefinitions(String...)} resolve to {@code tiles.xml} files.
 */
public class TilesConfigurerReferenceContributor extends PsiReferenceContributor {

  private static final ElementPattern<? extends PsiElement> SET_DEFINITIONS_PATTERN =
    psiElement(PsiLiteral.class)
      .with(SpringMvcLibraryUtil.IS_SPRING_MVC_PROJECT)
      .and(psiExpression().methodCallParameter(psiMethod().withName("setDefinitions").inClass(
        psiClass().withQualifiedName(string().oneOf(SpringWebConstants.TILES_CONFIGURER_CLASSES)))));

  static final PsiReferenceProvider PROVIDER = new PsiReferenceProvider() {
    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
      final String text = ElementManipulators.getValueText(element);

      final SpringResourcesBuilder builder = SpringResourcesBuilder
        .create(element, text)
        .fromRoot(true)
        .filter(TILES_XML_CONDITION)
        .endingSlashNotAllowed(true);
      return SpringResourcesUtil.getInstance().getReferences(builder);
    }
  };

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(SET_DEFINITIONS_PATTERN, PROVIDER, PsiReferenceRegistrar.HIGHER_PRIORITY);
  }
}
