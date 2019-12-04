/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.tiles.springMvc;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.spring.contexts.model.SpringModel;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.utils.SpringCommonUtils;
import com.intellij.spring.model.utils.SpringPropertyUtils;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.spring.web.SpringWebConstants;
import com.intellij.spring.web.mvc.views.ViewResolver;
import com.intellij.spring.web.mvc.views.ViewResolverFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public class TilesViewResolverFactory extends ViewResolverFactory {

  @NotNull
  @Override
  public Set<ViewResolver> handleViewResolverRegistry(String methodName,
                                                      PsiMethodCallExpression methodCallExpression,
                                                      SpringModel servletModel) {
    if ("tiles".equals(methodName)) {
      return Collections.singleton(new TilesViewResolver("ViewResolverRegistry#tiles()", servletModel));
    }
    return super.handleViewResolverRegistry(methodName, methodCallExpression, servletModel);
  }

  @Override
  protected boolean isMine(@Nullable CommonSpringBean bean, @NotNull PsiClass beanClass) {
    if (isTilesViewResolverClass(beanClass)) {
      return true;
    }

    // TODO
    // code setup: resolver.setViewClass(TilesView.class)

    if (!(bean instanceof SpringBean)) {
      return false;
    }

    final String viewClassValue = SpringPropertyUtils.getPropertyStringValue(bean, "viewClass");
    if (viewClassValue == null) {
      return false;
    }
    if (SpringWebConstants.TILES_2_VIEW_CLASS.equals(viewClassValue) ||
        SpringWebConstants.TILES_3_VIEW_CLASS.equals(viewClassValue)) {
      return true;
    }

    // try subclass
    final Module module = bean.getModule();
    if (module == null) {
      return false;
    }

    return isTilesViewClass(SpringCommonUtils.findLibraryClass(module, viewClassValue));
  }

  @Override
  public String getBeanClass() {
    return null;
  }

  @NotNull
  @Override
  public ViewResolver doCreate(@Nullable CommonSpringBean bean, SpringModel model) {
    assert bean != null;
    return new TilesViewResolver("TilesViewResolver[" + bean.getBeanName() + "]", model);
  }

  private static boolean isTilesViewClass(PsiClass psiClass) {
    return InheritanceUtil.isInheritor(psiClass, SpringWebConstants.TILES_2_VIEW_CLASS) ||
           InheritanceUtil.isInheritor(psiClass, SpringWebConstants.TILES_3_VIEW_CLASS);
  }

  private static boolean isTilesViewResolverClass(PsiClass psiClass) {
    return InheritanceUtil.isInheritor(psiClass, SpringWebConstants.TILES_2_VIEW_RESOLVER_CLASS) ||
           InheritanceUtil.isInheritor(psiClass, SpringWebConstants.TILES_3_VIEW_RESOLVER_CLASS);
  }
}
