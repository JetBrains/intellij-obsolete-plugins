/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.codeInsight.daemon.impl.quickfix.AddDefaultConstructorFix;
import com.intellij.codeInsight.intention.QuickFixFactory;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.rpc.GwtSerializableUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GwtInconsistentSerializableClassInspection extends BaseGwtInspection {
  private static final Logger LOG = Logger.getInstance(GwtInconsistentSerializableClassInspection.class);

  @Override
  public ProblemDescriptor @Nullable [] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
    GwtFacet gwtFacet = getFacet(aClass);
    if (gwtFacet == null || aClass instanceof PsiTypeParameter) return null;

    PsiFile containingFile = aClass.getContainingFile();
    if (containingFile == null) return null;
    VirtualFile virtualFile = containingFile.getVirtualFile();
    if (virtualFile == null) return null;
    List<GwtModule> gwtModules = GwtModulesManager.getInstance(manager.getProject()).findGwtModulesByClientSourceFile(virtualFile);
    if (gwtModules.isEmpty()) return null;

    GwtSerializableUtil.SerializableChecker serializableChecker = GwtSerializableUtil.createSerializableChecker(gwtFacet, true);
    if (!serializableChecker.isMarkedSerializable(aClass)) return null;

    List<ProblemDescriptor> descriptors = new ArrayList<>();
    final PsiField[] psiFields = aClass.getFields();
    for (PsiField psiField : psiFields) {
      if (!psiField.hasModifierProperty(PsiModifier.TRANSIENT)) {
        final PsiType type = psiField.getType();
        if (!serializableChecker.isSerializable(type)) {
          final String description = GwtBundle.message("problem.description.field.0.is.not.serializable", type.getPresentableText());
          PsiElement element = psiField.getTypeElement();
          if (element == null) {
            element = psiField;
          }
          descriptors.add(manager.createProblemDescriptor(element, description, isOnTheFly, LocalQuickFix.EMPTY_ARRAY,
                                                          ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        }
      }
    }

    if (!GwtSerializableUtil.hasPublicNoArgConstructor(aClass) && !aClass.isEnum() && !(aClass instanceof PsiAnonymousClass)) {
      LOG.assertTrue(aClass.getName() != null, aClass);
      PsiMethod constructor = GwtSerializableUtil.findNoArgConstructor(aClass);
      final String description = GwtBundle.message("problem.description.serializable.class.should.provide.public.no.args.constructor");
      if (constructor == null) {
        LOG.debug("Highlighting missing constructor for " + aClass.getName());
        final LocalQuickFix quickfix = LocalQuickFix.from(new AddDefaultConstructorFix(aClass));
        descriptors.add(manager.createProblemDescriptor(getElementToHighlight(aClass), description, quickfix,
                                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
      }
      else if (!gwtFacet.getSdkVersion().isPrivateNoArgConstructorInSerializableClassAllowed()) {
        LOG.debug("Highlighting private no-arg constructor for " + aClass.getName());
        LocalQuickFix quickfix = QuickFixFactory.getInstance().createModifierListFix(constructor, PsiModifier.PUBLIC, true, true);
        descriptors.add(manager.createProblemDescriptor(getElementToHighlight(constructor), description, quickfix,
                                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
      }
    }

    return descriptors.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }
}
