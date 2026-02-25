// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.spring;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.spring.SpringApiIcons;
import com.intellij.spring.model.SpringBeanPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;

import javax.swing.Icon;
import java.util.Collection;
import java.util.List;

final class SpringInjectionAnnotator extends LineMarkerProviderDescriptor {
  @Override
  public String getId() {
    return "GrailsSpringInjectedBean";
  }

  @Override
  public @NotNull Icon getIcon() {
    return SpringApiIcons.Gutter.SpringBean;
  }

  @Override
  public String getName() {
    return GrailsBundle.message("gutter.name.grails.spring.beans");
  }

  @Override
  public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
    return null;
  }

  @Override
  public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
    for (PsiElement element : elements) {
      PsiElement parent = element.getParent();
      if (parent instanceof GrField && element == ((GrField)parent).getNameIdentifierGroovy()) {
        SpringBeanPointer<?> bean = InjectedSpringBeanProvider.getInjectedBean((GrField)parent);
        if (bean == null) return;

        PsiElement navigateElement = bean.getPsiElement();
        if (!(navigateElement instanceof Navigatable)) return;

        result.add(
          NavigationGutterIconBuilder.create(SpringApiIcons.Gutter.ShowAutowiredDependencies,
                                             GrailsBundle.message("grails.gutter.spring.nav.group"))
            .setTarget(navigateElement)
            .setTooltipText(GrailsBundle.message("tooltip.injected.spring.bean", bean.getName())).createLineMarkerInfo(element)
        );
      }
    }
  }
}
