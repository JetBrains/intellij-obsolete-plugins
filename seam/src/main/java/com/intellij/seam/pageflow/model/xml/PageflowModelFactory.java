package com.intellij.seam.pageflow.model.xml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.pageflow.impl.PageflowModelImpl;
import com.intellij.seam.pageflow.model.xml.pageflow.PageflowDefinition;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomService;
import com.intellij.util.xml.model.impl.DomModelFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PageflowModelFactory extends DomModelFactory<PageflowDefinition, PageflowModel, PsiElement> {

  public PageflowModelFactory(final Project project) {
    super(PageflowDefinition.class, project, "pageflow");
  }

  @Override
  protected List<PageflowModel> computeAllModels(@NotNull final Module module) {
    List<PageflowModel> models = new ArrayList<>();

    final GlobalSearchScope moduleContentScope = module.getModuleContentScope();
    final Collection<VirtualFile> pageflowlFiles = DomService.getInstance().getDomFileCandidates(PageflowDefinition.class,
                                                                                                 moduleContentScope);

    for (VirtualFile pageflowlFile : pageflowlFiles) {
      final PsiFile file = PsiManager.getInstance(module.getProject()).findFile(pageflowlFile);
      if (file instanceof XmlFile) {
        final PageflowModel pageflowModel = computeModel((XmlFile)file, module);
        if (pageflowModel != null) {
           models.add(pageflowModel);
        }
      }
    }

    return models;
  }

  @Override
  @Nullable
  public PageflowModel getModelByConfigFile(@Nullable XmlFile psiFile) {
    if (psiFile == null) {
      return null;
    }
    return computeModel(psiFile, ModuleUtilCore.findModuleForPsiElement(psiFile));
  }

  @Override
  protected PageflowModel computeModel(@NotNull XmlFile psiFile, @Nullable Module module) {
    if (module == null) return null;

    return createSingleModel(psiFile, module);
  }

  @Nullable
  private PageflowModel createSingleModel(final XmlFile psiFile, final Module module) {
    final DomFileElement<PageflowDefinition> componentsDomFileElement = getDomRoot(psiFile);
    if (componentsDomFileElement != null) {
      final HashSet<XmlFile> files = new HashSet<>();
      files.add(psiFile);

      DomFileElement<PageflowDefinition> fileElement = files.size() > 1 ? createMergedModelRoot(files) : componentsDomFileElement;

      if (fileElement != null) {
        return new PageflowModelImpl(module, fileElement, files);
      }
    }
    return null;
  }

  @Override
  protected PageflowModel createCombinedModel(@NotNull final Set<XmlFile> configFiles,
                                              @NotNull final DomFileElement<PageflowDefinition> mergedModel,
                                              final PageflowModel firstModel,
                                              final Module module) {
    throw new UnsupportedOperationException();
  }
}
