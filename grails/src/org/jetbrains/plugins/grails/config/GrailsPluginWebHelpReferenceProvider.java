// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.openapi.paths.WebReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;

import java.util.regex.Matcher;

public class GrailsPluginWebHelpReferenceProvider extends GrailsMethodNamedArgumentReferenceProvider.Contributor.Provider
  implements GrailsMethodNamedArgumentReferenceProvider.Contributor {

  @Override
  public void register(GrailsMethodNamedArgumentReferenceProvider registrar) {
    registrar.register(0, this, new LightMethodCondition(GrailsPluginConfigMethodContributor.METHOD_KIND),
                       ArrayUtilRt.toStringArray(GrailsPluginConfigMethodContributor.SCOPES));
  }

  @Override
  public PsiReference[] createRef(final @NotNull PsiElement element,
                                  @NotNull GrMethodCall methodCall,
                                  int argumentIndex,
                                  @NotNull GroovyResolveResult resolveResult) {

    GrLiteralImpl literal = (GrLiteralImpl)element;

    String value = (String)literal.getValue();
    assert value != null;

    Matcher matcher = GrailsPluginNameCompletionContributor.DEPENDENCY_FORMAT.matcher(value);
    if (!matcher.matches()) return PsiReference.EMPTY_ARRAY;

    final String pluginName = matcher.group(2);

    if (pluginName.isEmpty()) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{new WebReference(element, "https://grails.org/plugin/" + pluginName)};
  }
}
