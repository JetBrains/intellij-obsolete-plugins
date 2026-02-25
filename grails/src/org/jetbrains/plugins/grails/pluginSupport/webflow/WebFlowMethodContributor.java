// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.webflow;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class WebFlowMethodContributor extends ClosureMemberContributor {
  @Override
  protected void processMembers(@NotNull GrClosableBlock closure, @NotNull PsiScopeProcessor processor, @NotNull PsiElement place, @NotNull ResolveState state) {
    PsiElement parent = closure.getParent();
    if (!(parent instanceof GrField)) return;

    if (!(WebFlowUtils.isFlowActionField((GrField)parent))) return;

    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return;

    String nameHint = ResolveUtil.getNameHint(processor);

    // Process variable 'flow'
    if (nameHint == null || "getFlow".equals(nameHint)) {
      GrLightMethodBuilder cachedGetFlow = CachedValuesManager.getCachedValue(parent, () -> {
        GrLightMethodBuilder method = new GrLightMethodBuilder(parent.getManager(), "getFlow");
        method.setReturnType(CommonClassNames.JAVA_UTIL_MAP, parent.getResolveScope());
        method.setData(((GrField)parent).getName());
        return CachedValueProvider.Result.create(method, PsiModificationTracker.MODIFICATION_COUNT);
      });
      if (!processor.execute(cachedGetFlow, state) || nameHint != null) return;
    }

    // Process variable 'conversation'
    if (nameHint == null || "getConversation".equals(nameHint)) {
      GrLightMethodBuilder cachedGetFlow = CachedValuesManager.getCachedValue(parent, () -> {
        GrLightMethodBuilder method = new GrLightMethodBuilder(parent.getManager(), "getConversation");
        method.setReturnType(CommonClassNames.JAVA_UTIL_MAP, parent.getResolveScope());
        method.setData(((GrField)parent).getName());
        return CachedValueProvider.Result.create(method, PsiModificationTracker.MODIFICATION_COUNT);
      });

      if (!processor.execute(cachedGetFlow, state) || nameHint != null) return;
    }

    // Process methods from FlowInfoCapturer.
    PsiClass flowInfoClass = JavaPsiFacade.getInstance(closure.getProject())
      .findClass("org.codehaus.groovy.grails.webflow.engine.builder.FlowInfoCapturer", closure.getResolveScope());

    if (flowInfoClass != null) {
      flowInfoClass.processDeclarations(processor, state, null, place);
    }
  }

}
