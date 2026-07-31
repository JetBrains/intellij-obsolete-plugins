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

package com.intellij.gwt.refactorings;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.i18n.GwtI18nManager;
import com.intellij.gwt.i18n.GwtI18nUtil;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.RefactoringFactory;
import com.intellij.refactoring.RenameRefactoring;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Function;
import com.intellij.util.FunctionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class GwtRefactoringListenerProvider implements RefactoringElementListenerProvider {
  private final Project myProject;
  private static final ThreadLocal<Boolean> myInsideGwtListener = ThreadLocal.withInitial(() -> false);

  public GwtRefactoringListenerProvider(final Project project) {
    myProject = project;
  }

  @Override
  public @Nullable RefactoringElementListener getListener(PsiElement element) {
    final PsiFile containingFile = element.getContainingFile();
    if (containingFile == null || !GwtFacet.isInModuleWithGwtFacet(element.getProject(), containingFile.getVirtualFile())) {
      return null;
    }

    RefactoringElementListener listener = null;
    if (element instanceof PsiClass) {
      listener = getServiceClassListener((PsiClass)element);
    }
    else if (element instanceof PsiMethod) {
      listener = getServiceMethodListener((PsiMethod)element);
    }

    if (listener != null) {
      return listener;
    }

    return getPropertiesClassListener(element);
  }

  private @Nullable RefactoringElementListener getServiceMethodListener(final PsiMethod method) {
    final PsiMethod asyncMethod = RemoteServiceUtil.findAsynchronousMethod(method);
    if (asyncMethod == null) return null;
    return new RefactoringElementListenerBase() {
      @Override
      public void elementRenamed(final @NotNull PsiElement newElement) {
        rename(asyncMethod, ((PsiMethod)newElement).getName());
      }
    };
  }

  private @Nullable RefactoringElementListener getServiceClassListener(final PsiClass psiClass) {
    if (!RemoteServiceUtil.isRemoteServiceInterface(psiClass)) {
      return null;
    }

    final PsiClass async = RemoteServiceUtil.findAsynchronousInterface(psiClass);

    if (async == null) {
      return null;
    }

    return new RefactoringElementListenerBase() {
      @Override
      public void elementRenamed(@NotNull PsiElement newElement) {
        rename(async, ((PsiClass)newElement).getName() + RemoteServiceUtil.ASYNC_SUFFIX);
      }
    };
  }

  private @Nullable RefactoringElementListener getPropertiesClassListener(PsiElement element) {
    final GwtI18nManager i18nManager = GwtI18nManager.getInstance(myProject);
    final Map<PsiNamedElement, Function<String, String>> elementsToRename = new HashMap<>(1);

    if (element instanceof IProperty property) {
      final PsiMethod method = i18nManager.getMethod(property);
      if (method != null && Objects.equals(property.getUnescapedKey(), method.getName())) {
        elementsToRename.put(method, FunctionUtil.id());
      }
    }

    if (element instanceof PsiMethod method) {
      final IProperty[] properties = i18nManager.getProperties(method);
      for (IProperty property : properties) {
        if (Objects.equals(property.getUnescapedKey(), method.getName())) {
          elementsToRename.put((PsiNamedElement)property.getPsiElement(), FunctionUtil.id());
        }
      }
    }

    if (element instanceof PsiClass psiClass) {
      final String className = psiClass.getName();
      final PropertiesFile[] files = i18nManager.getPropertiesFiles(psiClass);
      for (PropertiesFile file : files) {
        final String fileName = file.getName();
        if (className != null && fileName.startsWith(className)) {
          final String suffix = fileName.substring(className.length());
          elementsToRename.put(file.getContainingFile(), s -> s + suffix);
        }
      }
    }

    if (!elementsToRename.isEmpty()) {
      return new RefactoringElementListenerBase() {
        @Override
        public void elementRenamed(@NotNull PsiElement newElement) {
          for (Map.Entry<PsiNamedElement, Function<String, String>> entry : elementsToRename.entrySet()) {
            final PsiNamedElement psiElement = entry.getKey();
            final String newName = ((PsiNamedElement)newElement).getName();
            if (psiElement instanceof PsiMethod && !PsiNameHelper.getInstance(myProject).isIdentifier(newName)) {
              final GwtFacet gwtFacet = GwtFacet.findFacetBySourceFile(myProject, psiElement.getContainingFile().getVirtualFile());
              if (gwtFacet != null && newName != null) {
                GwtI18nUtil.addKeyAnnotationOrJavaDoc((PsiMethod)psiElement, newName, gwtFacet.getSdkVersion(), JavaPsiFacade.getInstance(myProject)
                  .getElementFactory());
              }
            }
            else {
              rename(psiElement, entry.getValue().fun(newName));
            }
          }
        }
      };
    }
    return null;
  }

  private void rename(final PsiElement element, final String newName) {
    if (Boolean.TRUE.equals(myInsideGwtListener.get())) {
      return;
    }
    try {
      myInsideGwtListener.set(true);
      RenameRefactoring rename = RefactoringFactory.getInstance(myProject).createRename(element, newName);
      rename.setSearchInComments(false);
      rename.setSearchInNonJavaFiles(false);
      rename.setPreviewUsages(false);

      UsageInfo[] usage = rename.findUsages();
      if (rename.preprocessUsages(new Ref<>(usage))) rename.doRefactoring(usage);
    }
    finally{
      myInsideGwtListener.set(false);
    }
  }

  private abstract static class RefactoringElementListenerBase implements RefactoringElementListener {
    @Override
    public void elementMoved(@NotNull PsiElement newElement) {
    }

    @Override
    public void elementRenamed(@NotNull PsiElement newElement) {
    }
  }

}
