/*
 * Copyright 2000-2006 JetBrains s.r.o.
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

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class NonJREEmulationClassesInClientCodeInspection extends AbstractClientCodeReferencesInspection {
  private static final Logger LOG = Logger.getInstance(NonJREEmulationClassesInClientCodeInspection.class);

  @Override
  protected void checkClassReference(@NotNull PsiJavaCodeReferenceElement reference,
                                     @NotNull PsiClass referencedClass,
                                     @NotNull String className,
                                     @NotNull VirtualFile virtualFile, @NotNull GwtFacet gwtFacet,
                                     List<GwtModule> gwtModules,
                                     @NotNull GwtModulesManager gwtModulesManager, @NotNull InspectionManager manager,
                                     boolean isOnTheFly,
                                     List<ProblemDescriptor> problems) {
    if (!isInJdk(virtualFile, gwtFacet)) {
      return;
    }

    PsiClass topLevelClass = PsiUtil.getTopLevelClass(referencedClass);
    if (topLevelClass == null) {
      topLevelClass = referencedClass;
    }

    if (!gwtFacet.getConfiguration().getSdk().containsJreEmulationClass(gwtModules, topLevelClass.getQualifiedName())) {
      final String message = GwtBundle.message("problem.description.class.0.is.not.presented.in.jre.emulation.library",
                                               className, gwtModules.get(0).getQualifiedName());
      problems.add(manager.createProblemDescriptor(reference, message, ((LocalQuickFix)null),
                                                   ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
    }
  }

  @Override
  protected void checkMethodReference(PsiJavaCodeReferenceElement reference,
                                      PsiMethod method,
                                      GwtFacet facet, List<GwtModule> gwtModules, GwtModulesManager gwtModulesManager,
                                      InspectionManager manager,
                                      boolean isOnTheFly,
                                      List<ProblemDescriptor> problems) {
    final PsiClass psiClass = method.getContainingClass();
    if (psiClass == null) return;
    final PsiFile psiFile = psiClass.getContainingFile();
    if (psiFile == null) return;
    final VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null || !isInJdk(virtualFile, facet)) return;

    final PsiClass emulationClass = facet.getConfiguration().getSdk().findJreEmulationClass(gwtModules, psiClass);
    if (emulationClass == null) return;

    if (!containsMethodWithSignature(emulationClass, method)) {
      final String message = GwtBundle.message("problem.description.method.0.is.not.presented.in.jre.emulation.library",
                                               PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY,
                                                                          PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_CONTAINING_CLASS | PsiFormatUtilBase.SHOW_PARAMETERS,
                                                                          PsiFormatUtilBase.SHOW_TYPE), gwtModules.get(0).getQualifiedName());
      problems.add(manager.createProblemDescriptor(reference, message, isOnTheFly, LocalQuickFix.EMPTY_ARRAY,
                                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
    }
  }

  private static boolean containsMethodWithSignature(PsiClass emulationClass, PsiMethod method) {
    if (method.isConstructor() && method.getParameterList().getParametersCount() == 0 && emulationClass.getConstructors().length == 0) {
      return true;
    }

    MethodSignatureBackedByPsiMethod signature = MethodSignatureBackedByPsiMethod.create(method, PsiSubstitutor.EMPTY, true);
    for (PsiMethod psiMethod : emulationClass.findMethodsByName(method.getName(), true)) {
      if (signature.equals(MethodSignatureBackedByPsiMethod.create(psiMethod, PsiSubstitutor.EMPTY, true))) {
        return true;
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Method with signature " + signature + " not found in " + emulationClass.getQualifiedName());
      final PsiFile file = emulationClass.getContainingFile();
      LOG.debug("File: " + (file != null ? file.getVirtualFile() : "null"));
      LOG.debug("Methods:");
      for (PsiMethod psiMethod : emulationClass.getMethods()) {
        LOG.debug(MethodSignatureBackedByPsiMethod.create(psiMethod, PsiSubstitutor.EMPTY, true).toString());
      }
    }
    return false;
  }

  private static boolean isInJdk(VirtualFile virtualFile, GwtFacet facet) {
    return !ProjectFileIndex.getInstance(facet.getModule().getProject()).findContainingSdks(virtualFile).isEmpty();
  }
}
