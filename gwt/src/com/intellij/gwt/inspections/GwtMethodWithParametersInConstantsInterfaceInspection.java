/*
 * Copyright 2000-2007 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.inspections;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.i18n.GwtI18nManager;
import com.intellij.gwt.i18n.GwtI18nUtil;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceList;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GwtMethodWithParametersInConstantsInterfaceInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor @Nullable [] checkClass(final @NotNull PsiClass aClass, final @NotNull InspectionManager manager, final boolean isOnTheFly) {
    if (!shouldCheck(aClass)) return null;

    GwtI18nManager i18nManager = GwtI18nManager.getInstance(manager.getProject());
    PropertiesFile[] files = i18nManager.getPropertiesFiles(aClass);
    if (files.length == 0 || !i18nManager.isConstantsInterface(aClass)) return null;

    PsiJavaCodeReferenceElement extendsConstantsElement = findExtendsConstantsElement(aClass);

    List<ProblemDescriptor> problems = new ArrayList<>();
    for (PsiMethod psiMethod : aClass.getMethods()) {
      if (psiMethod.getParameterList().getParametersCount() > 0) {
        ReplaceConstantsByMessagesInExtendsListQuickFix quickFix = null;
        if (extendsConstantsElement != null) {
          quickFix = new ReplaceConstantsByMessagesInExtendsListQuickFix(aClass, extendsConstantsElement);
        }

        problems.add(manager.createProblemDescriptor(getElementToHighlight(psiMethod), GwtBundle.message(
          "problem.description.methods.with.parameters.are.not.allowed.in.an.interface.extending.constants"), quickFix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                                     isOnTheFly));
      }
    }

    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  @Override
  public ProblemDescriptor[] checkFile(final @NotNull PsiFile file, final @NotNull InspectionManager manager, final boolean isOnTheFly) {
    if (!shouldCheck(file) || !(file instanceof PropertiesFile propertiesFile)) return null;

    GwtI18nManager i18nManager = GwtI18nManager.getInstance(manager.getProject());
    PsiClass propertiesInterface = i18nManager.getPropertiesInterface(propertiesFile);
    if (propertiesInterface == null || !i18nManager.isConstantsInterface(propertiesInterface)) return null;

    List<ProblemDescriptor> problems = new ArrayList<>();
    PsiJavaCodeReferenceElement extendsConstantsElement = findExtendsConstantsElement(propertiesInterface);
    for (IProperty property : propertiesFile.getProperties()) {
      if (GwtI18nUtil.getParametersCount(property.getValue()) > 0) {
        ReplaceConstantsByMessagesInExtendsListQuickFix quickFix = null;
        if (extendsConstantsElement != null) {
          quickFix = new ReplaceConstantsByMessagesInExtendsListQuickFix(propertiesInterface, extendsConstantsElement);
        }
        String message = GwtBundle.message("problem.description.properties.with.parameters.are.not.allowed.if.the.associated.interface.extends.constants");
        problems.add(manager.createProblemDescriptor(property.getPsiElement(), message, quickFix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
      }
    }

    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static @Nullable PsiJavaCodeReferenceElement findExtendsConstantsElement(final PsiClass aClass) {
    PsiJavaCodeReferenceElement extendsConstantsElement = null;
    PsiReferenceList list = aClass.getExtendsList();
    if (list != null) {
      for (PsiJavaCodeReferenceElement element : list.getReferenceElements()) {
        PsiElement anInterface = element.resolve();
        if (anInterface instanceof PsiClass &&
            GwtI18nUtil.CONSTANTS_INTERFACE_NAME.equals(((PsiClass)anInterface).getQualifiedName())) {
          extendsConstantsElement = element;
          break;
        }
      }
    }
    return extendsConstantsElement;
  }

  private static final class ReplaceConstantsByMessagesInExtendsListQuickFix extends BaseGwtLocalQuickFixOnPsiElement {
    private ReplaceConstantsByMessagesInExtendsListQuickFix(final PsiClass anInterface,
                                                            final PsiJavaCodeReferenceElement extendsConstantsElement) {
      super(GwtBundle.message("quickfix.family.name.inherit.from.messages.instead.of.constants"), GwtBundle.message("quickfix.name.inherit.0.from.messages.instead.of.constants", anInterface.getName()),
            anInterface, extendsConstantsElement);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiClass anInterface) || !(endElement instanceof PsiJavaCodeReferenceElement extendsConstantsElement)) return;

      if (!FileModificationService.getInstance().preparePsiElementForWrite(anInterface.getContainingFile())) return;

      try {
        extendsConstantsElement.delete();
        PsiElementFactory factory = JavaPsiFacade.getInstance(anInterface.getProject()).getElementFactory();
        PsiClassType messagesType = factory.createTypeByFQClassName(GwtI18nUtil.MESSAGES_INTERFACE_NAME, anInterface.getResolveScope());
        PsiReferenceList extendsList = anInterface.getExtendsList();
        LOG.assertTrue(extendsList != null);
        extendsList.add(factory.createReferenceElementByType(messagesType));
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }
  }
}
