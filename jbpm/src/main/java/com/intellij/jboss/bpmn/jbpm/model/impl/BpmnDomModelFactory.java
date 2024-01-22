package com.intellij.jboss.bpmn.jbpm.model.impl;

import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDefinitions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomService;
import com.intellij.util.xml.model.impl.DomModelFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BpmnDomModelFactory extends DomModelFactory<TDefinitions, BpmnDomModel, PsiElement> {

  BpmnDomModelFactory(final Project project) {
    super(TDefinitions.class, project, "definitions");
  }

  @Override
  @Nullable
  protected List<BpmnDomModel> computeAllModels(@NotNull final Module module) {
    final Set<VirtualFile> configFiles = new HashSet<>();

    configFiles.addAll(DomService.getInstance().getDomFileCandidates(TDefinitions.class,
                                                                     module.getModuleContentScope()));
    configFiles.addAll(DomService.getInstance().getDomFileCandidates(TDefinitions.class,
                                                                     GlobalSearchScope.moduleWithLibrariesScope(module)));

    List<BpmnDomModel> models = new ArrayList<>(configFiles.size());
    for (VirtualFile BpmnDomFile : configFiles) {
      final PsiFile file = PsiManager.getInstance(module.getProject()).findFile(BpmnDomFile);
      if (file instanceof XmlFile) {
        final BpmnDomModel bpmnDomModel = computeModel((XmlFile)file, module);
        ContainerUtil.addIfNotNull(models, bpmnDomModel);
      }
    }

    return models;
  }

  @Override
  @Nullable
  public BpmnDomModel getModelByConfigFile(@Nullable XmlFile psiFile) {
    if (psiFile == null) {
      return null;
    }
    return createSingleModel(psiFile);
  }

  @Override
  @Nullable
  protected BpmnDomModel computeModel(@NotNull XmlFile psiFile, @Nullable Module module) {
    return createSingleModel(psiFile);
  }

  @Nullable
  private BpmnDomModel createSingleModel(final XmlFile psiFile) {
    final DomFileElement<TDefinitions> flowDomFileElement = getDomRoot(psiFile);
    if (flowDomFileElement == null) {
      return null;
    }

    return new BpmnDomModelImpl(flowDomFileElement, Collections.singleton(psiFile));
  }

  @Override
  protected BpmnDomModel createCombinedModel(@NotNull final Set<XmlFile> configFiles,
                                             @NotNull final DomFileElement<TDefinitions> mergedModel,
                                             final BpmnDomModel firstModel,
                                             final Module module) {
    throw new UnsupportedOperationException();
  }
}