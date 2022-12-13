package com.intellij.seam.el;

import com.intellij.javaee.el.providers.ElContextProviderEx;
import com.intellij.jsf.model.xml.FacesConfig;
import com.intellij.jsf.model.xml.FacesDomModelManager;
import com.intellij.jsp.javaee.web.el.impl.CustomJsfVariableResolverProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiVariable;
import com.intellij.seam.jsf.JsfContextBeansUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SeamELContextProvider extends ElContextProviderEx {
  private final PsiElement myHost;

  public SeamELContextProvider(final PsiElement host) {
    myHost = host;
  }

  @Override
  @Nullable
  public Iterator<? extends PsiVariable> getTopLevelElVariables(@Nullable final String nameHint) {
    List<PsiVariable> allVars = new ArrayList<>(JsfContextBeansUtils.getJspImplicitVariables(nameHint, myHost.getContainingFile()));

    // add custom variables (spring, etc.)
    Module module = ModuleUtilCore.findModuleForPsiElement(myHost.getContainingFile());
    if (module != null) {
      for (FacesConfig facesConfig : FacesDomModelManager.getInstance(module.getProject()).getAllModels(module)) {
        String variableResolverClass = facesConfig.getApplication().getVariableResolver().getStringValue();
        if (!StringUtil.isEmptyOrSpaces(variableResolverClass)) {
          for (CustomJsfVariableResolverProvider provider : CustomJsfVariableResolverProvider.EP_NAME.getExtensionList()) {
            if (provider.acceptVariableResolver(variableResolverClass, module)) {
              provider.addVars(allVars, module);
            }
          }
        }
      }
    }

    return allVars.iterator();
  }

  @Override
  public boolean acceptsGetMethodForLastReference(final PsiMethod getter) {
    return true;
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
