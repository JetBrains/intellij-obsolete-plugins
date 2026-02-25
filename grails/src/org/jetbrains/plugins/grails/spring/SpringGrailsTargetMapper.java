// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.spring;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Computable;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.targets.AliasingPsiTarget;
import com.intellij.psi.targets.AliasingPsiTargetMapper;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.utils.SpringBeanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.stubs.GroovyShortNamesCache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SpringGrailsTargetMapper implements AliasingPsiTargetMapper {

  @Override
  public @NotNull Set<AliasingPsiTarget> getTargets(final @NotNull PomTarget target) {
    return ApplicationManager.getApplication().runReadAction(new Computable<>() {
      @Override
      public Set<AliasingPsiTarget> compute() {
        CommonSpringBean bean = SpringBeanUtils.getInstance().findBean(target);

        if (bean == null) return Collections.emptySet();

        String name = bean.getBeanName();
        if (name == null) return Collections.emptySet();

        Module module = bean.getModule();
        if (module == null) return Collections.emptySet();

        GlobalSearchScope scope = GlobalSearchScope.moduleWithDependentsScope(module);
        GroovyShortNamesCache cache = GroovyShortNamesCache.getGroovyShortNamesCache(module.getProject());

        Set<AliasingPsiTarget> res = new HashSet<>();

        for (final PsiField psiField : cache.getFieldsByName(name, scope)) {
          SpringBeanPointer<?> pointer = InjectedSpringBeanProvider.getInjectedBean(psiField);
          if (pointer != null && bean.equals(pointer.getSpringBean())) {
            res.add(new AliasingPsiTarget(psiField) {
              @Override
              public @NotNull AliasingPsiTarget setAliasName(@NotNull String newAliasName) {
                psiField.setName(newAliasName);
                return super.setAliasName(newAliasName);
              }
            });
          }
        }

        return res;
      }
    });
  }

}
