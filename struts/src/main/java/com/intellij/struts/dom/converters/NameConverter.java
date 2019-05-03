/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom.converters;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.ActionMappings;
import com.intellij.struts.dom.FormBean;
import com.intellij.struts.dom.FormBeans;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public abstract class NameConverter<T extends DomElement> implements CustomReferenceConverter<String> {

  private final String myClassPostfix;

  public NameConverter(@NonNls String classPostfix) {
    myClassPostfix = classPostfix;
  }

  protected abstract GenericDomValue<PsiClass> getClassElement(T parent);

  @Nullable
  protected abstract List<T> getSiblings(T parent);

  protected void preprocess(List<String> variants) {}

  @NotNull
  public Collection<? extends String> getVariants(final ConvertContext context) {
    //noinspection unchecked
    final T parent = (T)context.getInvocationElement().getParent();
    final GenericDomValue<PsiClass> classElement = getClassElement(parent);
    final PsiClass psiClass = classElement.getValue();
    if (psiClass != null) {
      final ArrayList<String> variants = new ArrayList<>();
      final String className = psiClass.getName();
      if (className != null && className.endsWith(myClassPostfix)) {
        String s = className.substring(0, className.length() - myClassPostfix.length());
        s = StringUtil.decapitalize(s);
        variants.add(s);
      } else {
        final Project project = psiClass.getProject();
        final PsiClassType classType = PsiTypesUtil.getClassType(psiClass);

        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
        final SuggestedNameInfo info = codeStyleManager.suggestVariableName(VariableKind.LOCAL_VARIABLE, null, null, classType);
        ContainerUtil.addAll(variants, info.names);
      }
      preprocess(variants);
      final List<T> list = getSiblings(parent);
      if (list != null) {
        for (int i = 0; i < variants.size(); i++) {
          String name = variants.get(i);
          int iter = 0;
          while (DomUtil.findByName(list, name) != null) {
            name = variants.get(i) + (++iter);
          }
          variants.set(i, name);
        }
      }
      return variants;
    }
    return Collections.emptyList();
  }

  @Override
  @NotNull
  public PsiReference[] createReferences(final GenericDomValue<String> genericDomValue, PsiElement element, final ConvertContext context) {
    final PsiReferenceBase<PsiElement> ref = new PsiReferenceBase<PsiElement>(element) {

      @Override
      public PsiElement resolve() {
        return genericDomValue.getParent().getXmlTag();
      }

      @Override
      public boolean isSoft() {
        return true;
      }

      //do nothing. the element will be renamed via PsiMetaData (com.intellij.refactoring.rename.RenameUtil.doRenameGenericNamedElement())
      @Override
      public PsiElement handleElementRename(@NotNull final String newElementName) throws IncorrectOperationException {
        return getElement();
      }

      @Override
      @NotNull
      public Object[] getVariants() {
        return NameConverter.this.getVariants(context).toArray();
      }
    };
    return new PsiReference[] {ref};
  }

  @SuppressWarnings({"WeakerAccess"})
  public static class ForAction extends NameConverter<Action> {
    @SuppressWarnings({"UnusedDeclaration"})
    public ForAction() {
      super("Action");
    }

    @Override
    protected void preprocess(final List<String> variants) {
      for (int i = 0; i < variants.size(); i++) {
        variants.set(i, "/" + variants.get(i));
      }
    }

    @Override
    protected GenericDomValue<PsiClass> getClassElement(final Action parent) {
      return parent.getType();
    }

    @Override
    @Nullable
    protected List<Action> getSiblings(final Action parent) {
      final StrutsModel model = StrutsManager.getInstance().getStrutsModel(parent.getXmlTag());
      if (model == null) {
        final DomElement element = parent.getParent();
        assert element != null;
        return ((ActionMappings)element).getActions();
      }
      else {
        return model.getActions();
      }
    }
  }

  public static class ForForm extends NameConverter<FormBean> {
    public ForForm() {
      super("Form");
    }

    @Override
    protected GenericDomValue<PsiClass> getClassElement(final FormBean parent) {
      return parent.getType();
    }

    @Override
    @Nullable
    protected List<FormBean> getSiblings(final FormBean parent) {
      final StrutsModel model = StrutsManager.getInstance().getStrutsModel(parent.getXmlTag());
      if (model == null) {
        final DomElement element = parent.getParent();
        assert element != null;
        return ((FormBeans)element).getFormBeans();
      }
      else {
        return model.getFormBeans();
      }
    }
  }
}
