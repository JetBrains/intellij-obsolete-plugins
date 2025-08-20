package com.intellij.jboss.bpmn.jpdl.el;

import com.intellij.javaee.el.providers.ElContextProviderEx;
import com.intellij.jsp.javaee.web.el.impl.CustomJsfContextBeansProvider;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JpdlElContextProvider extends ElContextProviderEx {
  private final PsiElement myHost;

  public JpdlElContextProvider(final PsiElement host) {
    myHost = host;
  }

  @Override
  @Nullable
  public Iterator<? extends PsiVariable> getTopLevelElVariables(@Nullable final String nameHint) {

    List<PsiVariable> vars = new ArrayList<>(JpdlELVariablesCollectorUtil.collectPredefinedVariables(myHost.getContainingFile()));
    for (CustomJsfContextBeansProvider provider : CustomJsfContextBeansProvider.EP_NAME.getExtensionList()) {
      provider.addVars(vars, myHost.getContainingFile());
    }
    return vars.iterator();
  }

  @Override
  public boolean acceptsSetMethodForLastReference(final PsiMethod setter) {
    return false;
  }

  @Override
  public boolean acceptsNonPropertyMethodForLastReference(final PsiMethod method) {
    if (isObjectClassMethod(method)) return false;

    return true;
  }

  private static boolean isObjectClassMethod(final PsiMethod method) {
    return CommonClassNames.JAVA_LANG_OBJECT.equals(method.getContainingClass().getQualifiedName());
  }
}

