// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.pluginClass;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.spring.GrailsResourcesGroovyMemberContributor;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

final class GrailsPluginClassMemberContributor extends NonCodeMembersContributor {
  // #CHECK# DefaultGrailsPlugin#doWithRuntimeConfiguration()
  private static final String CODE = """
    class GrailsPluginMembers {  private final org.codehaus.groovy.grails.commons.GrailsApplication application;
      private final org.codehaus.groovy.grails.plugins.GrailsPluginManager manager;
      private final org.codehaus.groovy.grails.plugins.GrailsPlugin plugin;
      private final org.springframework.context.ApplicationContext parentCtx;
      private final org.springframework.core.io.support.PathMatchingResourcePatternResolver resolver;
      private static final org.apache.commons.logging.Log log;
    }\s""";

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!GrailsUtils.isGrailsPluginClass(aClass)) return;

    if (ResolveUtil.shouldProcessProperties(processor.getHint(ElementClassHint.KEY))) {
      String name = ResolveUtil.getNameHint(processor);

      for (PsiField field : DynamicMemberUtils.getMembers(place.getProject(), CODE).getFields(name)) {
        if (!processor.execute(field, ResolveState.initial())) return;
      }
    }

    GrClosableBlock closure = PsiTreeUtil.getParentOfType(place, GrClosableBlock.class);
    if (closure != null) {
      if (isDoWithSpringClosure(closure, aClass)) {
        if (!GrailsResourcesGroovyMemberContributor.processBeanDefinition(processor, place, state, closure, null)) return;
      }
      else {
        GrClosableBlock parentClosure = PsiTreeUtil.getParentOfType(closure, GrClosableBlock.class);
        if (parentClosure != null && isDoWithSpringClosure(parentClosure, aClass)) {
          if (!GrailsResourcesGroovyMemberContributor.processBeanDefinition(processor, place, state, parentClosure, closure)) return;
        }
      }
    }
  }

  private static boolean isDoWithSpringClosure(@NotNull GrClosableBlock closure, @NotNull PsiClass pluginClass) {
    PsiElement eField = closure.getParent();
    return (eField instanceof GrField
            && "doWithSpring".equals(((GrField)eField).getName())
            && pluginClass.equals(((GrField)eField).getContainingClass()));
  }

}
