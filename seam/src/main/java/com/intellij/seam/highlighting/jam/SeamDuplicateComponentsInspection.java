package com.intellij.seam.highlighting.jam;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlElement;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.SeamJamInstall;
import com.intellij.seam.model.jam.SeamJamModel;
import com.intellij.seam.model.jam.SeamJamRole;
import com.intellij.seam.model.xml.SeamDomModelManager;
import com.intellij.seam.model.xml.components.SeamComponents;
import com.intellij.seam.model.xml.components.SeamDomComponent;
import com.intellij.seam.resources.SeamInspectionBundle;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeamDuplicateComponentsInspection extends SeamJamModelInspectionBase {

  @Override
  protected void checkSeamJamComponent(final SeamJamComponent seamComponent, final ProblemsHolder holder) {
    final Module module = seamComponent.getModule();
    if (module == null) return;

    if (isNotInstall(seamComponent)) return;

    final String componentName = seamComponent.getComponentName();
    final PsiType type = seamComponent.getComponentType();

    PsiElement identifyingPsiElement = seamComponent.getIdentifyingAnnotation();
    Set<SeamJamComponent> seamComponents = SeamJamModel.getModel(module).getSeamComponents(false, false);

    @NotNull List<SeamComponents> models = SeamDomModelManager.getInstance(module.getProject()).getAllModels(module);

    checkDuplicateNames(holder, seamComponent, componentName, type, identifyingPsiElement, seamComponents, models);
    for (SeamJamRole role : seamComponent.getRoles()) {
      final String roleName = role.getName();
      identifyingPsiElement = role.getIdentifyingAnnotation();
      checkDuplicateNames(holder, seamComponent, roleName, type, identifyingPsiElement, seamComponents, models);
    }
  }

  private static void checkDuplicateNames(final ProblemsHolder holder,
                                          final SeamJamComponent seamComponent,
                                          final String componentName,
                                          final PsiType type,
                                          final PsiElement identifyingPsiElement,
                                          final Set<SeamJamComponent> seamComponents,
                                          final  List<SeamComponents> models) {
    if (StringUtil.isEmptyOrSpaces(componentName) || identifyingPsiElement == null || type == null) return;

    Set<String> duplicatedInFiles = new HashSet<>();
    for (SeamJamComponent checkedComponent : seamComponents) {
      if (seamComponent.equals(checkedComponent) || isNotInstall(checkedComponent)) continue;
      if (isNameDuplicated(componentName, checkedComponent) && SeamCommonUtils.comparelInstalls(seamComponent, checkedComponent)) {

        final PsiFile containingFile = checkedComponent.getContainingFile();
        if (containingFile != null) {
          duplicatedInFiles.add(containingFile.getName());
        }
      }
    }

    for (SeamComponents model : models) {
      for (SeamDomComponent seamDomComponent : DomUtil.getDefinedChildrenOfType(model, SeamDomComponent.class)) {
        final PsiType psiType = seamDomComponent.getComponentType();
        if (componentName.equals(seamDomComponent.getComponentName()) && psiType != null && !type.isAssignableFrom(psiType) && SeamCommonUtils.comparelInstalls(seamComponent, seamDomComponent)) {
          final XmlElement xmlElement = seamDomComponent.getXmlElement();
          final PsiFile containingFile;
          if (xmlElement != null) {
            containingFile = xmlElement.getContainingFile();
            if (containingFile != null) {
              duplicatedInFiles.add(containingFile.getName());
              break;
            }
          }
        }
      }
    }

    if (duplicatedInFiles.size() > 0) {
      holder.registerProblem(identifyingPsiElement, SeamInspectionBundle.message("jam.duplicated.component.annotation", duplicatedInFiles,
                                                                                 duplicatedInFiles.size() > 1 ? "s:" : ""));
    }
  }

  private static boolean isNameDuplicated(final String componentName, final SeamJamComponent checkedComponent) {
    final boolean isDuplicated = componentName.equals(checkedComponent.getComponentName());

    return isDuplicated ? isDuplicated : isRoleNamesDuplicated(componentName, checkedComponent.getRoles());
  }

  private static boolean isRoleNamesDuplicated(final String componentName, final List<SeamJamRole> roles) {
    for (SeamJamRole role : roles) {
      if (componentName.equals(role.getName())) return true;
    }

    return false;
  }

  private static boolean isNotInstall(final SeamJamComponent checkedComponent) {
    final SeamJamInstall seamJamInstall = checkedComponent.getInstall();

    return seamJamInstall != null && !seamJamInstall.isInstall();
  }
}
