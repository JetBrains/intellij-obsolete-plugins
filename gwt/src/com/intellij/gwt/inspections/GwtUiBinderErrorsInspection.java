package com.intellij.gwt.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR;
import static com.intellij.gwt.GwtBundle.message;
import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_BINDER_INTERFACE;
import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_BINDER_NAMESPACE;
import static com.intellij.gwt.uiBinder.UiBinderUtil.getComponentClassName;
import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;
import static com.intellij.psi.util.InheritanceUtil.isInheritor;
import static com.intellij.psi.util.InheritanceUtil.isInheritorOrSelf;
import static com.intellij.psi.util.PsiTypesUtil.getPsiClass;

public final class GwtUiBinderErrorsInspection extends BaseGwtInspection {

  @Override
  public ProblemDescriptor @Nullable [] checkClass(@NotNull PsiClass uiBinderInheritor, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!uiBinderInheritor.isInterface() || !isInheritor(uiBinderInheritor, true, UI_BINDER_INTERFACE)) {
      return null;
    }

    Module module = findModuleForPsiElement(uiBinderInheritor);
    if (module == null) return null;

    List<ProblemDescriptor> problems = new ArrayList<>();
    XmlFile uiXmlFile = UiBinderMappingService.getInstance(module).getUiXmlFile(uiBinderInheritor);
    if (uiXmlFile == null) {
      PsiIdentifier nameIdentifier = uiBinderInheritor.getNameIdentifier();
      if (nameIdentifier != null) {
        PsiElement nameElement = nameIdentifier.getNavigationElement();
        String message = message("problem.description.ui.xml.not.found");
        problems.add(manager.createProblemDescriptor(nameElement, message, isOnTheFly, null, GENERIC_ERROR));
      }
    }
    else {
      if (uiBinderInheritor.getTypeParameters().length == 0) {
        PsiReferenceList extendsList = uiBinderInheritor.getExtendsList();
        if (extendsList != null) {
          PsiJavaCodeReferenceElement uiBinderReferenceElement = null;
          for (PsiJavaCodeReferenceElement referenceElement : extendsList.getReferenceElements()) {
            if (UI_BINDER_INTERFACE.equals(referenceElement.getQualifiedName())) {
              uiBinderReferenceElement = referenceElement;
              break; // there could be only one UiBinder interface in extendsList
            }
          }

          if (uiBinderReferenceElement == null) return null; // impossible

          PsiReferenceParameterList uiBinderParameterList = uiBinderReferenceElement.getParameterList();
          if (uiBinderParameterList != null) {
            PsiTypeElement[] uiBinderTypeParameterElements = uiBinderParameterList.getTypeParameterElements();
            if (uiBinderTypeParameterElements.length == 2) {
              PsiTypeElement typeElement = uiBinderTypeParameterElements[0];

              PsiClass psiClass = getPsiClass(typeElement.getType());
              if (psiClass != null) {
                XmlTag uiXmlRootTag = uiXmlFile.getRootTag();
                if (uiXmlRootTag == null) {
                  String message = message("problem.description.ui.xml.root.not.found");
                  problems.add(manager.createProblemDescriptor(typeElement, message, isOnTheFly, null, GENERIC_ERROR));
                }
                else {
                  XmlTag[] uiXmlFileRootSubtags = uiXmlRootTag.getSubTags();
                  List<XmlTag> widgetTags = new SmartList<>();
                  for (XmlTag uiXmlFileRootSubtag : uiXmlFileRootSubtags) {
                    if (!UI_BINDER_NAMESPACE.equals(uiXmlFileRootSubtag.getNamespace())) {
                      widgetTags.add(uiXmlFileRootSubtag);
                    }
                  }
                  if (widgetTags.isEmpty()) {
                    String message = message("problem.description.ui.xml.root.not.found");
                    problems.add(manager.createProblemDescriptor(typeElement, message, isOnTheFly, null, GENERIC_ERROR));
                  }
                  else if (widgetTags.size() == 1) {
                    XmlTag widgetTag = ContainerUtil.getFirstItem(widgetTags);

                    String componentClassName = getComponentClassName(widgetTag);
                    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(uiBinderInheritor.getProject());
                    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);

                    PsiClass resolvedComponentClass = psiFacade.findClass(componentClassName, scope);
                    if (resolvedComponentClass != null) {
                      if (!isInheritorOrSelf(resolvedComponentClass, psiClass, true)) {
                        String message = message("problem.description.inconsistent.element.type",
                                                 widgetTag.getName(), resolvedComponentClass.getQualifiedName());
                        problems.add(manager.createProblemDescriptor(typeElement, message, isOnTheFly, null, GENERIC_ERROR));
                      }
                    }
                  }
                  else {
                    String message = message("problem.description.ui.xml.root.ambiguity");
                    problems.add(manager.createProblemDescriptor(typeElement, message, isOnTheFly, null, GENERIC_ERROR));
                  }
                }
              }
            }
          }
        }
      }
    }
    return problems.isEmpty() ? ProblemDescriptor.EMPTY_ARRAY : problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }
}
