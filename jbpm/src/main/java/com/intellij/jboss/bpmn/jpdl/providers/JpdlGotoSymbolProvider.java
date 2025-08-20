package com.intellij.jboss.bpmn.jpdl.providers;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.model.xml.ProcessDefinition;
import com.intellij.jboss.bpmn.jpdl.utils.JpdlCommonUtils;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class JpdlGotoSymbolProvider extends GoToSymbolProvider {

  @Override
  protected void addNames(@NotNull final Module module, final Set<String> result) {
    result.addAll(JpdlCommonUtils.getProcessNames(module));
  }

  @Override
  protected void addItems(@NotNull final Module module, final String name, final List<NavigationItem> result) {
    JpdlCommonUtils.getProcessNames(module);
    for (ProcessDefinition definition : JpdlCommonUtils.getProcessDefinitions(module)) {
      if (name.equals(definition.getName().getStringValue())) {
        final PsiElement psiElement = definition.getXmlElement();

        if (psiElement != null) {
          final NavigationItem navigationItem = createNavigationItem(psiElement, name, JbossJbpmIcons.Jpdl.Process);
          if (!result.contains(navigationItem)) {
            result.add(navigationItem);
          }
        }
      }
    }
  }

  @Override
  protected boolean acceptModule(final Module module) {
    return true;
  }
}

