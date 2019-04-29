/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.i18n;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@State(name = "ResourceBeansContainer")
public class ResourceBeansContainer implements PersistentStateComponent<Element> {
  private final ResourceBundlesBean myBean;

  public static ResourceBeansContainer getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, ResourceBeansContainer.class);
  }

  public ResourceBeansContainer(@NotNull Project project) {
    myBean = new ResourceBundlesBean(project);
  }

  @Override
  public Element getState() {
    final Element element = new Element("state");
    myBean.writeExternal(element);
    return element;
  }

  @Override
  public void loadState(@NotNull Element state) {
    myBean.readExternal(state);
  }

  public void registerResourceBundle(final PsiClass aClass) {
    myBean.registerResourceBundle(aClass);
  }

  public PsiClass getResourceBundle() {
    return myBean.getResourceBundle();
  }
}
