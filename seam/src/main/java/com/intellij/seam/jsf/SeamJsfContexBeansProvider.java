package com.intellij.seam.jsf;

import com.intellij.jsp.javaee.web.el.impl.CustomJsfContextBeansProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiVariable;

import java.util.List;

public class SeamJsfContexBeansProvider implements CustomJsfContextBeansProvider {

  @Override
  public void addVars(final List<PsiVariable> resultVars, final PsiFile file) {
    resultVars.addAll(JsfContextBeansUtils.getJspImplicitVariables(file));
  }
}