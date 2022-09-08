package com.intellij.seam.highlighting.xml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.model.jam.MergedSeamComponent;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.SeamJamInstall;
import com.intellij.seam.model.jam.SeamJamModel;
import com.intellij.seam.model.xml.SeamDomModelManager;
import com.intellij.seam.model.xml.components.SeamComponents;
import com.intellij.seam.model.xml.components.SeamDomComponent;
import com.intellij.seam.resources.SeamInspectionBundle;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SeamDomModelInspection extends BasicDomElementsInspection<SeamComponents> {

  public SeamDomModelInspection() {
    super(SeamComponents.class);
  }

  @Override
  public void checkFileElement(final DomFileElement<SeamComponents> seamComponents, final DomElementAnnotationHolder holder) {
    super.checkFileElement(seamComponents, holder);

    checkJamDuplicatedComponents(seamComponents, holder);
  }

  private static void checkJamDuplicatedComponents(final DomFileElement<SeamComponents> seamComponents,
                                                   final DomElementAnnotationHolder holder) {
    final XmlFile xmlFile = seamComponents.getFile();
    final Module module = ModuleUtilCore.findModuleForPsiElement(xmlFile);

    final @Nullable SeamComponents model = SeamDomModelManager.getInstance(xmlFile.getProject()).getSeamModel(xmlFile);

    if (model != null && module != null) {
      final Map<String, List<SeamJamComponent>> map = getJamComponentNames(module);

      for (SeamDomComponent domComponent : DomUtil.getDefinedChildrenOfType(model, SeamDomComponent.class)) {
        final String name = domComponent.getComponentName();
        final PsiType type = domComponent.getComponentType();
        if (name != null && type != null && map.containsKey(name)) {
          final List<SeamJamComponent> components = map.get(name);
          if (components != null) {
            List<String> duplicatedInFiles = new ArrayList<>();
            for (SeamJamComponent component : components) {
              if(component instanceof MergedSeamComponent) continue;

              final PsiType psiType = component.getComponentType();
              if (psiType != null && !type.isAssignableFrom(psiType)) {
                final SeamJamComponent pair = SeamCommonUtils.getPair(domComponent);

                if ((pair == null && SeamCommonUtils.comparelInstalls(component, domComponent)) || (pair != null && SeamCommonUtils.comparelInstalls(pair, component))) {
                  final PsiFile containingFile = component.getContainingFile();
                  if (containingFile != null) {
                    duplicatedInFiles.add(containingFile.getName());
                  }
                }
              }
            }

            if (duplicatedInFiles.size() > 0) {
              final GenericAttributeValue<String> value = domComponent.getName();
              holder.createProblem(value.getXmlAttribute() == null ? domComponent : value, SeamInspectionBundle.message(
                  "jam.duplicated.component.annotation", duplicatedInFiles, duplicatedInFiles.size() > 1 ? "s:" : ""));
            }
          }
        }

      }

    }
  }

  private static Map<String, List<SeamJamComponent>> getJamComponentNames(final Module module) {
    Map<String, List<SeamJamComponent>> result = new HashMap<>();

    Set<SeamJamComponent> jamComponents = SeamJamModel.getModel(module).getSeamComponents(false);
    for (SeamJamComponent jamComponent : jamComponents) {
      if (jamComponent.getPsiElement() instanceof PsiCompiledElement) continue;

      final String name = jamComponent.getComponentName();
      final PsiFile psiFile = jamComponent.getContainingFile();
      final SeamJamInstall install = jamComponent.getInstall();
      if (install != null && !install.isInstall()) continue;

      if (!StringUtil.isEmptyOrSpaces(name) && psiFile != null) {
        if (!result.containsKey(name)) result.put(name, new ArrayList<>());
        result.get(name).add(jamComponent);
      }
    }
    return result;
  }
}
