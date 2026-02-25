// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.jsp.highlighter.JspxFileType;
import com.intellij.jsp.highlighter.NewJspFileType;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.GroovyMvcIcons;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.GspFileViewProvider;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.util.GroovyUtils;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class GrailsGotoRelatedProvider extends GotoRelatedProvider {

  @Override
  public @NotNull List<? extends GotoRelatedItem> getItems(@NotNull PsiElement context) {
    PsiFile containingFile = context.getContainingFile();
    if (containingFile == null) return Collections.emptyList();

    VirtualFile virtualFile = containingFile.getVirtualFile();
    if (virtualFile == null) return Collections.emptyList();

    Module module = ModuleUtilCore.findModuleForPsiElement(containingFile);
    if (module == null || !GrailsFramework.getInstance().hasSupport(module)) return Collections.emptyList();

    List<GotoRelatedItem> res = new ArrayList<>();

    if (containingFile.getViewProvider() instanceof GspFileViewProvider) {
      getRelatedForGsp(module, containingFile, virtualFile, res);
    }
    else {
      if (containingFile instanceof GroovyFile) {
        PsiClass artifactClass = GroovyUtils.getClassDefinition((GroovyFile)containingFile);
        GrailsArtifact artifact = GrailsArtifact.getType(artifactClass);

        if (artifact != null) {
          String name = artifact.getArtifactName(artifactClass);

          if (artifact == GrailsArtifact.CONTROLLER) {
            addAll(module, name, GrailsArtifact.DOMAIN, res);
            addAllRelatedView(artifactClass, res);
          }
          else if (artifact == GrailsArtifact.DOMAIN) {
            final Collection<GrClassDefinition> controllers = addAll(module, name, GrailsArtifact.CONTROLLER, res);
            for (GrClassDefinition controller : controllers) {
              addAllRelatedView(controller, res);
            }
          }
        }
      }
    }

    return res;
  }

  private static void getRelatedForGsp(@NotNull Module module, @NotNull PsiFile file, @NotNull VirtualFile gspFile, List<GotoRelatedItem> res) {
    String templateName = GrailsUtils.getTemplateName(gspFile.getName());
    if (templateName != null) {
      getRelatedForTemplate(file, res);
      return;
    }

    String name = GrailsUtils.getControllerNameByGsp(gspFile);
    if (name != null) {
      addAll(module, name, GrailsArtifact.DOMAIN, res);
      Collection<GrClassDefinition> controllers = addAll(module, name, GrailsArtifact.CONTROLLER, res);
      PsiMethod action = GrailsUtils.getControllerActions(name, module).get(gspFile.getNameWithoutExtension());
      if (action != null) {
        res.add(new GotoRelatedItem(GrailsUtils.toField(action), GrailsBundle.message("library.name")) {
          @Override
          public @Nullable Icon getCustomIcon() {
            return GroovyMvcIcons.Action_method;
          }
        });
      }
      for (GrClassDefinition controller : controllers) {
        addAllRelatedView(controller, res);
      }
    }
  }

  private static void addAllRelatedView(@NotNull PsiClass controller, List<GotoRelatedItem> res) {
    VirtualFile gspDir = GrailsUtils.getControllerGspDir(controller);
    if (gspDir == null) return;

    PsiManager psiManager = controller.getManager();

    for (VirtualFile child : gspDir.getChildren()) {
      FileType fileType = child.getFileType();
      if (fileType == GspFileType.GSP_FILE_TYPE || fileType == NewJspFileType.INSTANCE || fileType == JspxFileType.INSTANCE) {
        PsiFile psiFile = psiManager.findFile(child);
        if (psiFile != null) {
          res.add(new GotoRelatedItem(psiFile, GrailsBundle.message("view.group.title")));
        }
      }
    }
  }

  private static Collection<GrClassDefinition> addAll(Module module, String name, GrailsArtifact artifact, List<GotoRelatedItem> res) {
    Collection<GrClassDefinition> instances = artifact.getInstances(module, name);
    for (GrClassDefinition aClass : instances) {
      res.add(new GotoRelatedItem(aClass, GrailsBundle.message("library.name")));
    }
    return instances;
  }

  private static void getRelatedForTemplate(@NotNull PsiFile gspFile, List<GotoRelatedItem> res) {
    //ReferencesSearch.searchOptimized();
  }
}
