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

package com.intellij.gwt.actions;

import com.intellij.CommonBundle;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.JavaTemplateUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class GwtCreateActionBase extends CreateElementActionBase {
  private static final Logger LOG = Logger.getInstance(GwtCreateActionBase.class);
  private static final @NonNls String NAME_TEMPLATE_PROPERTY = "NAME";

  @Override
  protected final PsiElement @NotNull [] invokeDialog(final @NotNull Project project, final @NotNull PsiDirectory directory) {
    Module module = ModuleUtilCore.findModuleForFile(directory.getVirtualFile(), project);
    if (module == null) return PsiElement.EMPTY_ARRAY;

    GwtFacet facet = GwtFacet.getInstance(module);
    if (facet == null) {
      int answer = Messages.showYesNoDialog(project, GwtBundle.message("question.text.gwt.facet.is.not.configured.for.module.0.do.you.want.to.create.it", module.getName()),
                                            GwtBundle.message("dialog.title.google.web.toolkit"), Messages.getQuestionIcon());
      if (answer != Messages.YES) {
        return PsiElement.EMPTY_ARRAY;
      }
      facet = GwtFacet.createNewFacet(module, null);
      ModulesConfigurator.showFacetSettingsDialog(facet, null);
      facet = GwtFacet.getInstance(module);
      if (facet == null) {
        return PsiElement.EMPTY_ARRAY;
      }
    }

    if (requireGwtModule()) {
      final GwtModule gwtModule = findGwtModule(project, directory);
      if (gwtModule == null) {
        final String message = GwtBundle.message("error.message.this.action.is.allowed.only.for.client.side.packages.of.a.gwt.module");
        Messages.showErrorDialog(project, message, CommonBundle.getErrorTitle());
        return PsiElement.EMPTY_ARRAY;
      }
    }

    MyInputValidator validator = new MyInputValidator(project, directory);
    showDialog(facet, directory, validator);

    return validator.getCreatedElements();
  }

  protected void showDialog(@NotNull GwtFacet facet, @NotNull PsiDirectory directory, @NotNull MyInputValidator validator) {
    Messages.showInputDialog(facet.getModule().getProject(), getDialogPrompt(), getDialogTitle(), Messages.getQuestionIcon(), "", validator);
  }

  protected PsiFile[] getAffectedFiles(final GwtModule gwtModule) {
    return PsiFile.EMPTY_ARRAY;
  }

  protected abstract boolean requireGwtModule();

  protected abstract @NlsContexts.DialogMessage String getDialogPrompt();

  protected abstract @NlsContexts.DialogTitle String getDialogTitle();

  private static @Nullable GwtModule findGwtModule(Project project, PsiDirectory directory) {
    return GwtModulesManager.getInstance(project).findGwtModuleByClientSourceFile(directory.getVirtualFile());
  }

  @Override
  public final void update(final @NotNull AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    super.update(e);

    if (presentation.isEnabled() && !isUnderSourceRootsOfModuleWithGwtFacet(e)) {
      presentation.setEnabledAndVisible(false);
    }
  }

  public static boolean isUnderSourceRootsOfModuleWithGwtFacet(final AnActionEvent e) {
    Module module = e.getData(PlatformCoreDataKeys.MODULE);
    if (module == null) {
      return false;
    }

    if (GwtFacet.getInstance(module) == null) {
      return false;
    }

    final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    final Project project = e.getProject();
    if (view != null && project != null) {
      ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
      PsiDirectory[] dirs = view.getDirectories();
      for (PsiDirectory dir : dirs) {
        if (projectFileIndex.isUnderSourceRootOfType(dir.getVirtualFile(), JavaModuleSourceRootTypes.SOURCES) && JavaDirectoryService.getInstance().getPackage(dir) != null) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  protected PsiElement @NotNull [] create(@NotNull String newName, @NotNull PsiDirectory directory) throws Exception {
    doCheckBeforeCreate(newName, directory);
    List<VirtualFile> files = new ArrayList<>();
    for (PsiFile psiFile : getAffectedFiles(findGwtModule(directory.getProject(), directory))) {
      final VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile != null) {
        files.add(virtualFile);
      }
    }
    ReadonlyStatusHandler.getInstance(directory.getProject()).ensureFilesWritable(files);
    final GwtModule gwtModule;
    if (requireGwtModule()) {
      gwtModule = findGwtModule(directory.getProject(), directory);
    }
    else {
      gwtModule = null;
    }
    return WriteAction.compute(() -> doCreate(newName, directory, gwtModule));
  }

  protected abstract PsiElement @NotNull [] doCreate(String newName, PsiDirectory directory, final GwtModule gwtModule) throws Exception;

  protected static PsiClass createClassFromTemplate(final PsiDirectory directory, String className, FileType fileType,
                                                    String templateName, @NonNls Object... parameters) throws IncorrectOperationException {
    final PsiFile file = createFromTemplateInternal(directory, className, className + "." + JavaFileType.INSTANCE.getDefaultExtension(),
                                                    fileType, templateName, parameters);
    return ((PsiJavaFile)file).getClasses()[0];
  }

  protected static PsiFile createFromTemplate(final PsiDirectory directory, @NonNls String fileName, FileType fileType,
                                              @NonNls String templateName, @NonNls Object... parameters) throws IncorrectOperationException {
    return createFromTemplateInternal(directory, FileUtilRt.getNameWithoutExtension(fileName), fileName, fileType, templateName, parameters);
  }

  protected static PsiFile createFromTemplateInternal(PsiDirectory directory, String name, String fileName, FileType fileType,
                                                      String templateName, @NonNls Object... parameters) throws IncorrectOperationException {
    String text = getTextByTemplate(directory, name, templateName, parameters);

    PsiManager psiManager = directory.getManager();
    PsiFile file = PsiFileFactory.getInstance(directory.getProject()).createFileFromText(fileName, fileType, text);

    CodeStyleManager.getInstance(psiManager).reformat(file);

    return (PsiFile)directory.add(file);
  }

  protected static @NotNull String getTextByTemplate(PsiDirectory directory, String name, String templateName, @NonNls Object... parameters) {
    final FileTemplate template = FileTemplateManager.getInstance(directory.getProject()).getJ2eeTemplate(templateName);

    Map<String, Object> properties = FileTemplateManager.getInstance(directory.getProject()).getDefaultContextMap();
    JavaTemplateUtil.setPackageNameAttribute(properties, directory);
    properties.put(NAME_TEMPLATE_PROPERTY, name);

    LOG.assertTrue(parameters.length % 2 == 0);
    for (int i = 0; i < parameters.length; i+=2) {
      properties.put((String)parameters[i], parameters[i + 1]);
    }
    try {
      return template.getText(properties);
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to load template for " + FileTemplateManager.getInstance(directory.getProject()).internalTemplateToSubject(templateName), e);
    }
  }

  @Override
  protected String getErrorTitle() {
    return CommonBundle.getErrorTitle();
  }

  protected void doCheckBeforeCreate(String newName, PsiDirectory directory) throws IncorrectOperationException {
    JavaDirectoryService.getInstance().checkCreateClass(directory, newName);
  }

}