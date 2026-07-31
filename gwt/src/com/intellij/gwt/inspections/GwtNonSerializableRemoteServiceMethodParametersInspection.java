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

import com.intellij.codeInsight.intention.QuickFixFactory;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.options.OptPane;
import com.intellij.codeInspection.options.OptionController;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.rpc.GwtGenericsUtil;
import com.intellij.gwt.rpc.GwtSerializableUtil;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Tag;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.codeInspection.options.OptPane.checkbox;
import static com.intellij.codeInspection.options.OptPane.pane;

public final class GwtNonSerializableRemoteServiceMethodParametersInspection extends BaseGwtInspection {
  private final GwtSerializableInspectionState myGwtSerializableInspectionState = new GwtSerializableInspectionState();
  private static final @NonNls String SETTINGS_ELEMENT = "settings";

  @Override
  public ProblemDescriptor @Nullable [] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
    GwtFacet gwtFacet = getFacet(aClass);
    if (gwtFacet == null) return null;

    if (RemoteServiceUtil.isRemoteServiceInterface(aClass)) {
      return checkRemoteService(gwtFacet, aClass, manager, isOnTheFly, isOnTheFly);
    }
    return null;
  }

  @Override
  public void readSettings(final @NotNull Element node) throws InvalidDataException {
    final Element settings = node.getChild(SETTINGS_ELEMENT);
    if (settings != null) {
      XmlSerializer.deserializeInto(myGwtSerializableInspectionState, settings);
    }
  }

  @Override
  public void writeSettings(final @NotNull Element node) throws WriteExternalException {
    final Element settings = new Element(SETTINGS_ELEMENT);
    XmlSerializer.serializeInto(myGwtSerializableInspectionState, settings, new SkipDefaultValuesSerializationFilters());
    if (!settings.getContent().isEmpty()) {
      node.addContent(settings);
    }
  }

  @Override
  public @NotNull OptPane getOptionsPane() {
    return pane(checkbox("myGwtSerializableInspectionState", GwtBundle.message("checkbox.text.report.interfaces")));
  }

  @Override
  public @NotNull OptionController getOptionController() {
    return super.getOptionController()
      .onValue("myGwtSerializableInspectionState", myGwtSerializableInspectionState::isReportInterfaces,
               myGwtSerializableInspectionState::setReportInterfaces);
  }

  private ProblemDescriptor[] checkRemoteService(final GwtFacet gwtFacet, final PsiClass aClass, final InspectionManager manager,
                                                 boolean onTheFly, boolean isOnTheFly) {
    ArrayList<ProblemDescriptor> result = new ArrayList<>(0);

    GwtSerializableUtil.SerializableChecker serializableChecker = GwtSerializableUtil.createSerializableChecker(gwtFacet,
                                                                                                                myGwtSerializableInspectionState.isReportInterfaces());

    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(manager.getProject());
    for (final PsiMethod method : aClass.getMethods()) {
      for (final PsiParameter param : method.getParameterList().getParameters()) {
        String typeParametersString = GwtGenericsUtil.getTypeParametersString(method, param.getName());
        checkTypeSerial(param.getTypeElement(), typeParametersString, serializableChecker, manager, result, isOnTheFly);
      }
      final PsiTypeElement returnTypeElement = method.getReturnTypeElement();
      if (returnTypeElement != null) {
        String typeParameters = GwtGenericsUtil.getReturnTypeParametersString(method);
        checkTypeSerial(returnTypeElement, typeParameters, serializableChecker, manager, result, isOnTheFly);
      }

      PsiJavaCodeReferenceElement[] thrown = method.getThrowsList().getReferenceElements();
      for (PsiJavaCodeReferenceElement referenceElement : thrown) {
        PsiClassType classType = psiFacade.getElementFactory().createType(referenceElement);
        PsiClass psiClass = classType.resolve();

        if (psiClass != null && !InheritanceUtil.isInheritor(psiClass, false, CommonClassNames.JAVA_LANG_EXCEPTION)) {
          String message = GwtBundle.message("problem.description.0.is.not.a.checked.exception", psiClass.getQualifiedName());
          result.add(manager.createProblemDescriptor(referenceElement, message, onTheFly, LocalQuickFix.EMPTY_ARRAY,
                                                     ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        }
        else {
          checkTypeSerial(classType, referenceElement, null, serializableChecker, manager, result, isOnTheFly);
        }
      }
    }

    return result.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static void checkTypeSerial(PsiTypeElement typeElement, final @Nullable String typeParameterStrings,
                                      final GwtSerializableUtil.SerializableChecker serializableChecker,
                                      InspectionManager manager,
                                      List<ProblemDescriptor> result, boolean isOnTheFly) {
    PsiType type = typeElement.getType();
    checkTypeSerial(type, typeElement, typeParameterStrings, serializableChecker, manager, result, isOnTheFly);
  }

  private static void checkTypeSerial(PsiType type, final PsiElement typeElement, final @Nullable String typeParameterStrings,
                                      final GwtSerializableUtil.SerializableChecker serializableChecker,
                                      final InspectionManager manager,
                                      final List<ProblemDescriptor> result, boolean onTheFly) {
    if (!type.isValid()) return;
    List<PsiType> typeParameters = GwtGenericsUtil.getTypeParameters(typeElement, typeParameterStrings);

    if (serializableChecker.isSerializable(type, typeParameters)) {
      return;
    }
    while (type instanceof PsiArrayType) {
      type = ((PsiArrayType)type).getComponentType();
    }

    if (!(type instanceof PsiClassType classType)) {
      return;
    }

    if (!serializableChecker.getVersion().isGenericsSupported() && classType.getParameters().length > 0) {
      String description = GwtBundle.message("problem.description.generics.isnt.supported.in.gwt.before.1.5.version");
      result.add(manager.createProblemDescriptor(typeElement, description, onTheFly, LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
      return;
    }

    PsiClass aClass = classType.resolve();
    if (aClass != null) {
      boolean haveGenericParameters = serializableChecker.getVersion().isGenericsSupported() && classType.getParameters().length > 0;
      final String description;
      final LocalQuickFix[] quickFixes;
      String typeString = type.getCanonicalText();
      if (typeParameterStrings == null && !haveGenericParameters && GwtSerializableUtil.isCollection(type)) {
        description = GwtBundle.message("problem.description.type.of.collection.elements.is.not.specified", typeString);
        quickFixes = LocalQuickFix.EMPTY_ARRAY;
      }
      else {
        if (typeParameterStrings != null && !haveGenericParameters) {
          typeString += typeParameterStrings;
        }

        if (!isInSources(aClass)) {
          quickFixes = LocalQuickFix.EMPTY_ARRAY;
          description = GwtBundle.message("problem.description.type.is.not.serializable", typeString);
        }
        else {
          final List<PsiClass> list = serializableChecker.getSerializableMarkerInterfaces();
          final PsiElementFactory psiFactory = JavaPsiFacade.getInstance(aClass.getProject()).getElementFactory();
          quickFixes = new LocalQuickFix[list.size()];
          for (int i = 0; i < list.size(); i++) {
            quickFixes[i] = LocalQuickFix.from(QuickFixFactory.getInstance()
              .createExtendsListFix(aClass, psiFactory.createType(list.get(i)), true));
          }
          description = GwtBundle.message("problem.description.gwt.serializable.type.0.should.implements.marker.interface.1", typeString, serializableChecker.getPresentableSerializableClassesString());
        }
      }
      result.add(manager.createProblemDescriptor(typeElement, description, onTheFly, quickFixes, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
    }
  }

  private static boolean isInSources(final PsiClass aClass) {
    VirtualFile file = aClass.getContainingFile().getVirtualFile();
    if (file == null) return false;
    Module module = ModuleUtilCore.findModuleForFile(file, aClass.getProject());
    return module != null && ModuleRootManager.getInstance(module).getFileIndex().isInSourceContent(file);
  }

  public static class GwtSerializableInspectionState {
    private boolean myReportInterfaces = true;

    @Tag("report-interfaces")
    public boolean isReportInterfaces() {
      return myReportInterfaces;
    }

    public void setReportInterfaces(final boolean reportInterfaces) {
      myReportInterfaces = reportInterfaces;
    }
  }
}
