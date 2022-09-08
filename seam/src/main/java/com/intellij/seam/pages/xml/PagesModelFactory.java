package com.intellij.seam.pages.xml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.pages.impl.PagesModelImpl;
import com.intellij.seam.pages.xml.pages.Page;
import com.intellij.seam.pages.xml.pages.Pages;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomService;
import com.intellij.util.xml.model.impl.DomModelFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PagesModelFactory extends DomModelFactory<Pages, PagesModel, PsiElement> {

  public PagesModelFactory(Project project) {
    super(Pages.class, project, "pageflow");
  }

  @Override
  protected List<PagesModel> computeAllModels(@NotNull final Module module) {
    List<PagesModel> models = new ArrayList<>();

    final GlobalSearchScope moduleContentScope = module.getModuleContentScope();
    final Collection<VirtualFile> pageflowlFiles = DomService.getInstance().getDomFileCandidates(Page.class, moduleContentScope);

    for (VirtualFile pageflowlFile : pageflowlFiles) {
      final PsiFile file = PsiManager.getInstance(module.getProject()).findFile(pageflowlFile);
      if (file instanceof XmlFile) {
        final PagesModel pageflowModel = computeModel((XmlFile)file, module);
        if (pageflowModel != null) {
           models.add(pageflowModel);
        }
      }
    }

    return models;
  }

  @Override
  @Nullable
  public PagesModel getModelByConfigFile(@Nullable XmlFile psiFile) {
    if (psiFile == null) {
      return null;
    }
    return computeModel(psiFile, ModuleUtilCore.findModuleForPsiElement(psiFile));
  }

  @Override
  protected PagesModel computeModel(@NotNull XmlFile psiFile, @Nullable Module module) {
    if (module == null) return null;

    return createSingleModel(psiFile, module);
  }

  @Nullable
  private PagesModel createSingleModel(final XmlFile psiFile, final Module module) {
    final DomFileElement<Pages> componentsDomFileElement = getDomRoot(psiFile);
    if (componentsDomFileElement != null) {
      final HashSet<XmlFile> files = new HashSet<>();
      files.add(psiFile);

      DomFileElement<Pages> fileElement = files.size() > 1 ? createMergedModelRoot(files) : componentsDomFileElement;

      if (fileElement != null) {
        return new PagesModelImpl(module, fileElement, files);
      }
    }
    return null;
  }

  @Override
  protected PagesModel createCombinedModel(@NotNull final Set<XmlFile> configFiles,
                                           @NotNull final DomFileElement<Pages> mergedModel,
                                           final PagesModel firstModel,
                                           final Module module) {
    throw new UnsupportedOperationException();
  }

}
