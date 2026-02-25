// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMember;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

import java.util.Set;

public final class GrailsImplicitUsageProvider implements ImplicitUsageProvider {

  private static final Set<String> DOMAIN_METHODS = Set.of("beforeInsert", "beforeUpdate", "beforeDelete", "beforeValidate",
                                                                  "afterInsert", "afterUpdate", "afterDelete", "onLoad", "afterLoad");

  @Override
  public boolean isImplicitUsage(@NotNull PsiElement element) {
    if (element instanceof GrClassDefinition) {
      if (GrailsArtifact.getType((PsiClass)element) != null) {
        return true;
      }
    }
    else if (element instanceof GrMember member) {

      if (member instanceof GrField field) {

        if (GrailsStaticFieldCompletionProvider.isGrailsField(field)) {
          return true;
        }

        if (GrailsUtils.isGrailsPluginClass(field.getContainingClass())) {
          return true;
        }
      }

      GrailsArtifact artifact = GrailsArtifact.getType(member.getContainingClass());
      if (artifact != null) {
        switch (artifact) {
          case CONTROLLER -> {
            if (GrailsUtils.isControllerAction(member)) {
              return true;
            }
          }
          case DOMAIN -> {
            if (member instanceof GrMethod) {
              if (DOMAIN_METHODS.contains(member.getName())) {
                return true;
              }
            }
          }
          case BOOTSTRAP -> {
            if (member instanceof GrField) {
              String filedName = member.getName();
              if ("init".equals(filedName) || "destroy".equals(filedName)) {
                return true;
              }
            }
          }
          case CODEC -> {
            if (member instanceof GrField && member.hasModifierProperty(PsiModifier.STATIC)) {
              String filedName = member.getName();
              if ("encode".equals(filedName) || "decode".equals(filedName)) {
                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }

  @Override
  public boolean isImplicitRead(@NotNull PsiElement element) {
    return false;
  }

  @Override
  public boolean isImplicitWrite(@NotNull PsiElement element) {
    return false;
  }
}
