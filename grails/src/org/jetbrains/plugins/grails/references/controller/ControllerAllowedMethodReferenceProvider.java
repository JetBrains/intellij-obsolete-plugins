// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsPatterns;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.grField;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.namedArgumentLabel;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.stringLiteral;

public class ControllerAllowedMethodReferenceProvider extends PsiReferenceProvider {

  // Method list got from http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.2
  public static final String[] HTTP_METHODS = {"GET", "POST", "HEAD", "PUT", "OPTIONS", "DELETE", "TRACE"};

  public static void register(PsiReferenceRegistrar registrar) {
    PsiElementPattern.Capture<GrNamedArgument> namedArgumentPattern =
      psiElement(GrNamedArgument.class).withParent(psiElement(GrListOrMap.class).withParent(
        grField().withName("allowedMethods").withModifiers("static").inClass(GrailsPatterns.artifact(GrailsArtifact.CONTROLLER))));

    registrar.registerReferenceProvider(namedArgumentLabel(null).withParent(namedArgumentPattern),
                                        new ControllerAllowedMethodReferenceProvider());

    PsiReferenceProvider valueRefProvider = new ValueReferenceProvider();

    registrar.registerReferenceProvider(stringLiteral().withParent(namedArgumentPattern), valueRefProvider);
    registrar.registerReferenceProvider(stringLiteral().withParent(psiElement(GrListOrMap.class).withParent(namedArgumentPattern)), valueRefProvider);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    GrArgumentLabel label = (GrArgumentLabel)element;
    
    GrField field = (GrField)element.getParent().getParent().getParent();
    
    final PsiClass controller = field.getContainingClass();
    assert controller != null;

    return new PsiReference[]{new ActionReference(label, false, GrailsArtifact.CONTROLLER.getArtifactName(controller))};
  }

  private static class ValueReferenceProvider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
      return new PsiReference[]{
        new PsiReferenceBase<>(element, false) {
          @Override
          public PsiElement resolve() {
            return null;
          }

          @Override
          public Object @NotNull [] getVariants() {
            PsiElement parent = getElement().getParent();

            if (parent instanceof GrListOrMap) {
              List<String> res = new ArrayList<>(Arrays.asList(HTTP_METHODS));
              GrailsPsiUtil.removeValuesFromList(res, (GrListOrMap)parent);
              return res.toArray();
            }

            return HTTP_METHODS;
          }
        }
      };
    }
  }
}
