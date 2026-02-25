// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Couple;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.grails.references.util.CodecUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.Map;

final class CodecNonCodeMemberContributor extends NonCodeMembersContributor {
  private static final String ENCODE_AS = "encodeAs";
  private static final String DECODE = "decode";

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!(qualifierType instanceof PsiClassType) && !(qualifierType instanceof PsiArrayType)) return;

    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return;

    if (aClass instanceof TagLibNamespaceDescriptor.DummyClass) return; // We cannot encode namespace prefix :)

    if (!(place instanceof GrReferenceExpression)) return;

    String name = ResolveUtil.getNameHint(processor);

    String codecName = null;
    boolean isEncode = false;

    if (name != null) {
      if (name.startsWith(ENCODE_AS)) {
        codecName = name.substring(ENCODE_AS.length());
        isEncode = true;
      }
      else {
        if (!name.startsWith(DECODE)) return;
        codecName = name.substring(DECODE.length());
      }

      if (codecName.isEmpty()) return;
    }

    if (GrailsFramework.getInstance().findAppDirectory(place) == null) return;

    GrExpression qualifier = ((GrReferenceExpression)place).getQualifier();
    if (qualifier instanceof GrReferenceExpression) {
      if (((GrReferenceExpression)qualifier).resolve() instanceof PsiClass) return;
    }

    final Module module = ModuleUtilCore.findModuleForPsiElement(place);
    if (module == null) return;

    Map<String, Couple<PsiMethod>> codecsMap = CodecUtil.getCodecMap(module);
    if (codecsMap.isEmpty()) return;

    processMap(codecsMap, codecName, processor, state, isEncode);
  }

  private static boolean processMap(Map<String, Couple<PsiMethod>> map,
                                    String codecName,
                                    PsiScopeProcessor processor,
                                    ResolveState state,
                                    boolean isEncode) {
    if (codecName == null) {
      for (Couple<PsiMethod> methodPair : map.values()) {
        if (methodPair.first != null && !processor.execute(methodPair.first, state)) return true;
        if (methodPair.second != null && !processor.execute(methodPair.second, state)) return true;
      }
    }
    else {
      Couple<PsiMethod> pair = map.get(codecName);
      if (pair != null) {
        PsiMethod method = isEncode ? pair.first : pair.second;
        if (method != null) {
          processor.execute(method, state);
        }
        return true;
      }
    }

    return false;
  }

}
