package com.intellij.jboss.bpmn.jpdl.model;

import com.intellij.jboss.bpmn.jpdl.impl.JpdlModelImpl;
import com.intellij.jboss.bpmn.jpdl.model.xml.ProcessDefinition;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomService;
import com.intellij.util.xml.model.impl.DomModelFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JpdlModelFactory extends DomModelFactory<ProcessDefinition, JpdlModel, PsiElement> {

  public JpdlModelFactory(final Project project) {
    super(ProcessDefinition.class, project, "process");
  }

  @Override
  protected List<JpdlModel> computeAllModels(@NotNull final Module module) {
    List<JpdlModel> models = new ArrayList<>();

    final GlobalSearchScope moduleContentScope = module.getModuleContentScope();
    final Collection<VirtualFile> jpdlCandidateFiles = DomService.getInstance().getDomFileCandidates(ProcessDefinition.class,
                                                                                                     moduleContentScope);

    for (VirtualFile jpdlCandidateFile : jpdlCandidateFiles) {
      final PsiFile file = PsiManager.getInstance(module.getProject()).findFile(jpdlCandidateFile);
      if (file instanceof XmlFile) {
        final JpdlModel jpdlModel = computeModel((XmlFile)file, module);
        if (jpdlModel != null) {
          models.add(jpdlModel);
        }
      }
    }

    return models;
  }

  @Override
  @Nullable
  public JpdlModel getModelByConfigFile(@Nullable XmlFile psiFile) {
    if (psiFile == null) {
      return null;
    }
    return computeModel(psiFile, ModuleUtilCore.findModuleForPsiElement(psiFile));
  }

  @Override
  protected JpdlModel computeModel(@NotNull XmlFile psiFile, @Nullable Module module) {
    if (module == null) return null;

    return createSingleModel(psiFile, module);
  }

  @Nullable
  private JpdlModel createSingleModel(final XmlFile psiFile, final Module module) {
    final DomFileElement<ProcessDefinition> componentsDomFileElement = getDomRoot(psiFile);
    if (componentsDomFileElement != null) {
      final HashSet<XmlFile> files = new HashSet<>();
      files.add(psiFile);
      return new JpdlModelImpl(module, componentsDomFileElement, files);
    }
    return null;
  }

  @Override
  protected JpdlModel createCombinedModel(@NotNull final Set<XmlFile> configFiles,
                                          @NotNull final DomFileElement<ProcessDefinition> mergedModel,
                                          final JpdlModel firstModel,
                                          final Module module) {
    throw new UnsupportedOperationException();
  }
}
