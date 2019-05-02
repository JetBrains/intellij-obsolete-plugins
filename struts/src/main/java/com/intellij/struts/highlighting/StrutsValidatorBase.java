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

package com.intellij.struts.highlighting;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.javaee.facet.JavaeeFacetUtil;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.util.InspectionValidator;
import com.intellij.openapi.compiler.util.InspectionValidatorUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.facet.StrutsFacet;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.model.impl.DomModelFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Base class for Validators.
 *
 * @author Dmitry Avdeev
 */
public abstract class StrutsValidatorBase extends InspectionValidator {

  protected StrutsValidatorBase(@NotNull final String description,
                                @NotNull final String progressIndicatorText) {
    super(description, progressIndicatorText);
  }

  protected abstract DomModelFactory getFactory(Project project);

  @NotNull
  protected Set<XmlFile> getConfigFiles(@NotNull final Module module) {
    return getFactory(module.getProject()).getAllConfigFiles(module);
  }

  @Override
  public boolean isAvailableOnScope(@NotNull final CompileScope scope) {
    final Module[] modules = scope.getAffectedModules();
    return !JavaeeFacetUtil.getInstance().getJavaeeFacets(WebFacet.ID, modules).isEmpty();
  }

  protected abstract boolean isAvailableOnFacet(StrutsFacet facet);

  @Override
  public Collection<VirtualFile> getFilesToProcess(final Project project, final CompileContext context) {
    final ArrayList<VirtualFile> list = new ArrayList<>();
    for (final Module module : ModuleManager.getInstance(project).getModules()) {
      final Collection<WebFacet> webFacets = WebFacet.getInstances(module);
      boolean containsStruts = false;
      for (WebFacet webFacet : webFacets) {
        final StrutsFacet facet = StrutsFacet.getInstance(webFacet);
        if (facet != null && isAvailableOnFacet(facet)) {
          containsStruts = true;
        }
      }
      if (!containsStruts) continue;

      final Set<? extends PsiFile> psiFiles = getConfigFiles(module);
      for (final PsiFile psiFile : psiFiles) {
        ContainerUtil.addIfNotNull(list, psiFile.getVirtualFile());
      }
    }
    InspectionValidatorUtil.expandCompileScopeIfNeeded(list, context);
    return list;
  }

  @Override
  @NotNull
  public Collection<? extends PsiElement> getDependencies(final PsiFile psiFile) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(psiFile);
    if (module == null) {
      return Collections.emptyList();
    }
    final Set<? extends PsiFile> psiFiles = getConfigFiles(module);
    psiFiles.remove(psiFile);
    return psiFiles;
  }

  @Override
  public CompilerMessageCategory getCategoryByHighlightDisplayLevel(@NotNull final HighlightDisplayLevel severity, @NotNull final VirtualFile virtualFile,
                                                                    @NotNull final CompileContext context) {
    final CompilerMessageCategory level = super.getCategoryByHighlightDisplayLevel(severity, virtualFile, context);
    if (level == CompilerMessageCategory.ERROR) {
      final Module module = context.getModuleByFile(virtualFile);
      if (module != null) {
        WebFacet webFacet = WebUtil.getWebFacet(virtualFile, module.getProject());
        if (webFacet != null) {
          final StrutsFacet facet = StrutsFacet.getInstance(webFacet);
          if (facet != null && facet.getConfiguration().getValidationConfiguration().myReportErrorsAsWarnings) {
            return CompilerMessageCategory.WARNING;
          }
        }
      }
    }
    return level;
  }

}