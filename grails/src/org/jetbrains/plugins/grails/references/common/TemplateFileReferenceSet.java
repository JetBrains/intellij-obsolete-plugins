// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class TemplateFileReferenceSet extends PluginSupportFileReferenceSet {

  private static final Pattern TEMPLATE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-]+");
  private static final String WEB_APP = "web-app";

  private final String myControllerName;

  private final GspTagWrapper myTagWrapper;

  public TemplateFileReferenceSet(@Nullable String controllerName,
                                  @NotNull String str,
                                  @NotNull PsiElement element,
                                  int startInElement,
                                  PsiReferenceProvider provider,
                                  final boolean isCaseSensitive, boolean endingSlashNotAllowed,
                                  @Nullable GspTagWrapper tagWrapper) {
    super(str, element, startInElement, provider, isCaseSensitive, endingSlashNotAllowed, false);
    myControllerName = controllerName;
    myTagWrapper = tagWrapper;
    reparse();
  }

  private @Nullable FileReference getContextPathReference() {
    if (myTagWrapper == null) return null;

    PsiElement contextPathElement = myTagWrapper.getAttributeValue("contextPath");

    if (contextPathElement != null) {
      for (PsiReference ref : contextPathElement.getReferences()) {
        if (ref instanceof FileReference) {
          return ((FileReference)ref).getLastFileReference();
        }
      }
    }

    return null;
  }

  @Override
  public MyFileReference createNonPluginFileReference(TextRange range, int index, String text) {
    FileReference prevRef = null;
    if (index == 0) {
      FileReference contextPathRef = getContextPathReference();
      if (contextPathRef instanceof PluginDirReference) {
        prevRef = contextPathRef;
      }
    }
    return new TmplFileReference(range, index, text, this, prevRef);
  }

  private void addRoot(List<PsiFileSystemItem> res, VirtualFile root) {
    if (!isAbsolutePathReference() && myControllerName != null) {
      root = root.findChild(myControllerName);
      if (root == null) return;
    }

    PsiDirectory dir = getElement().getManager().findDirectory(root);
    if (dir == null) return;

    res.add(dir);
  }

  private void addRootAndGrailsApp(List<PsiFileSystemItem> res, @Nullable VirtualFile root) {
    if (root != null) {
      VirtualFile grailsApp = root.findChild(GrailsUtils.GRAILS_APP_DIRECTORY);
      if (grailsApp != null) {
        VirtualFile viewDir = grailsApp.findChild(GrailsUtils.VIEWS_DIRECTORY);
        if (viewDir != null) {
          addRoot(res, viewDir);
        }
      }

      VirtualFile webAppRoot = root.findChild(WEB_APP);
      if (webAppRoot != null) {
        addRoot(res, webAppRoot);
      }

      addRoot(res, root);
    }
  }

  @Override
  public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
    VirtualFile file = getElement().getContainingFile().getOriginalFile().getVirtualFile();
    if (file == null) return Collections.emptyList();

    List<PsiFileSystemItem> res = new ArrayList<>();

    PsiElement pluginElement = myTagWrapper == null ? null : myTagWrapper.getAttributeValue("plugin");

    if (pluginElement != null) {
      PsiReference pluginRef = pluginElement.getReference();
      if (pluginRef != null) {
        PsiDirectory psiPluginRoot = (PsiDirectory)pluginRef.resolve();
        if (psiPluginRoot != null) {
          addRootAndGrailsApp(res, psiPluginRoot.getVirtualFile());
        }
      }

      return res;
    }

    FileReference contextPathRef = getContextPathReference();
    if (contextPathRef != null) {
      if (contextPathRef instanceof PluginDirReference) return Collections.emptyList();

      for (ResolveResult resolveResult : contextPathRef.multiResolve(false)) {
        PsiElement item = resolveResult.getElement();
        if (item instanceof PsiFileSystemItem) {
          addRootAndGrailsApp(res, ((PsiFileSystemItem)item).getVirtualFile());
        }
      }

      return res;
    }

    VirtualFile root = GrailsUtils.findParent(file, GrailsUtils.GRAILS_APP_DIRECTORY);
    if (root != null) {
      root = root.getParent();
    }

    addRootAndGrailsApp(res, root);

    return res;
  }

  @Override
  protected boolean isAcceptToCompletion(@NotNull VirtualFile fileOrDir) {
    return super.isAcceptToCompletion(fileOrDir) || GrailsUtils.getTemplateName(fileOrDir.getName()) != null;
  }

  private static class TmplFileReference extends MyFileReference {

    private final FileReference previousRef;

    TmplFileReference(final TextRange range,
                             final int index,
                             final String text,
                             final FileReferenceSet set,
                             FileReference previousRef) {
      super(set, range, index, text);
      this.previousRef = previousRef;
    }

    @Override
    public FileReference getPreviousReference() {
      if (previousRef != null) {
        return previousRef;
      }
      return super.getPreviousReference();
    }

    @Override
    public void innerResolveInContext(final @NotNull String text,
                                      final @NotNull PsiFileSystemItem context,
                                      final @NotNull Collection<? super ResolveResult> result,
                                      final boolean caseSensitive) {
      super.innerResolveInContext(isLast() ? GrailsUtils.getFileNameByTemplateName(text) : text, context, result, caseSensitive);
    }

    @Override
    protected Object createLookupItem(PsiElement candidate) {
      if (candidate instanceof GspFile file) {

        String templateName = GrailsUtils.getTemplateName(file.getName());
        assert templateName != null;

        return LookupElementBuilder.create(templateName).withIcon(file.getIcon(0));
      }

      return super.createLookupItem(candidate);
    }

    @Override
    protected @Nullable String pathToString(@NotNull String path) {
      int index = path.lastIndexOf('/');
      if (index == -1) {
        return GrailsUtils.getTemplateName(path);
      }

      String templateName = GrailsUtils.getTemplateName(path.substring(index + 1));
      if (templateName == null) return null;
      return path.substring(0, index + 1) + templateName;
    }

    @Override
    public @NotNull LocalQuickFix @Nullable [] getQuickFixes() {

      if (!isLast()) return LocalQuickFix.EMPTY_ARRAY;

      String text = getText();
      if (!TEMPLATE_NAME_PATTERN.matcher(text).matches()) return LocalQuickFix.EMPTY_ARRAY;

      VirtualFile context = null;

      for (PsiFileSystemItem item : getContexts()) {
        VirtualFile virtualFile = item.getVirtualFile();
        if (virtualFile == null || !virtualFile.isDirectory()) continue;

        if (context == null) {
          context = virtualFile;
        }
        else {
          if (VfsUtilCore.isAncestor(context, virtualFile, false)) {
            context = virtualFile;
          }
          else if (!VfsUtilCore.isAncestor(virtualFile, context, false)) {
            if (virtualFile.getName().equals(WEB_APP)) {
              continue;
            }

            return LocalQuickFix.EMPTY_ARRAY;
          }
        }
      }

      if (context == null) return LocalQuickFix.EMPTY_ARRAY;

      final String templateName = GrailsUtils.getFileNameByTemplateName(text);

      final VirtualFile finalContext = context;

      return new LocalQuickFix[] {
        new LocalQuickFix() {
          @Override
          public @NotNull String getFamilyName() {
            return GrailsBundle.message("quick.fix.family.name.create.template.0", templateName);
          }

          @Override
          public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            VirtualFile createdFile;

            try {
              createdFile = finalContext.createChildData(project, templateName);
            }
            catch (IOException ignored) {
              return;
            }

            PsiFile psiFile = PsiManager.getInstance(project).findFile(createdFile);
            if (psiFile == null) return;

            psiFile.navigate(true);
          }
        }
      };
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getUnresolvedMessagePattern() {
      if (isLast()) {
        return GrailsBundle.message("unresolved.pattern.cannot.resolve.template.0.gsp", getCanonicalText());
      }

      return super.getUnresolvedMessagePattern();
    }
  }
}
