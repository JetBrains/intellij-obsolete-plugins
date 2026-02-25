// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.html.impl;

import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.source.html.ScriptSupportUtil;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.addins.js.JavaScriptIntegrationUtil;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.gspIndex.GspIncludeIndex;
import org.jetbrains.plugins.grails.lang.gsp.gspIndex.GspIncludeInfo;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.grails.lang.gsp.psi.GspPsiUtil;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.api.GspLikeFile;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GspHtmlFileImpl extends XmlFileImpl implements GspTokenTypesEx, GspLikeFile {
  private static final int MAX_RESOLVED_INCLUDING = 1;
  private static final int MAX_ATTEMPTS_OF_FILE_REFERENCE_RESOLVE = 4;

  public GspHtmlFileImpl(FileViewProvider fileViewProvider) {
    super(fileViewProvider, GSP_HTML_TEMPLATE_ROOT);
  }

  @Override
  public XmlDocument getDocument() {
    return findChildByClass(XmlDocument.class);
  }

  @Override
  public @NotNull GspFile getGspLanguageRoot() {
    return (GspFile)getViewProvider().getPsi(GspLanguage.INSTANCE);
  }

  @Override
  public @NotNull FileType getFileType() {
    return getViewProvider().getFileType();
  }

  @Override
  public String toString() {
    return "GspHtmlFileImpl";
  }

  @Override
  public @NotNull PsiFile getOriginalFile() {
    final PsiFile original = super.getOriginalFile();
    if (original == this) {
      GspFile gspFile = GspPsiUtil.getGspFile(this);
      if (gspFile != null) {
        final PsiFile gspOriginal = gspFile.getOriginalFile();
        return gspOriginal.getViewProvider().getPsi(HTMLLanguage.INSTANCE);
      }
    }
    return original;
  }

  private static List<GspFile> getIncludingFiles(final GspFile gspFile) {
    Module module = ModuleUtilCore.findModuleForPsiElement(gspFile);
    if (module == null) return Collections.emptyList();

    String key = StringUtil.trimEnd(gspFile.getName(), ".gsp");

    GlobalSearchScope scope = module.getModuleContentScope();

    FileBasedIndex index = FileBasedIndex.getInstance();

    Collection<VirtualFile> containingFiles = index.getContainingFiles(GspIncludeIndex.NAME, key, scope);

    int gspFileCount = 0;
    for (VirtualFile containingFile : containingFiles) {
      if (FileTypeRegistry.getInstance().isFileOfType(containingFile, GspFileType.GSP_FILE_TYPE)) {
        gspFileCount++;
      }
    }

    if (gspFileCount > MAX_ATTEMPTS_OF_FILE_REFERENCE_RESOLVE) { // Optimization: don't resolve anything if files are many.
      return Collections.emptyList();
    }

    final List<GspFile> res = new ArrayList<>();
    final LinkedHashMap<VirtualFile, Collection<GspIncludeInfo>> valuesToProcess = new LinkedHashMap<>();
    index.processValues(GspIncludeIndex.NAME, key, null, (file, value) -> {
      if (!FileTypeRegistry.getInstance().isFileOfType(file, GspFileType.GSP_FILE_TYPE)) return true;

      valuesToProcess.put(file, value);
      return true;
    }, scope);

    int resolveAttempts = MAX_RESOLVED_INCLUDING;
    for(Map.Entry<VirtualFile, Collection<GspIncludeInfo>> entry:valuesToProcess.entrySet()) {
      for (GspIncludeInfo includeInfo : entry.getValue()) {
        if (resolveAttempts-- == 0) {
          return Collections.emptyList();
        }

        GspFile f = getPsiFileIfReferenceResolved(gspFile, entry.getKey(), includeInfo.getOffset());
        if (f != null) {
          res.add(f);

          break;
        }
      }
    }

    return res;
  }

  private static @Nullable GspFile getPsiFileIfReferenceResolved(@NotNull GspFile gspFileToCompare, @NotNull VirtualFile virtualFile, int offset) {
    PsiFile file = gspFileToCompare.getManager().findFile(virtualFile);
    if (!(file instanceof GspFile)) return null;

    PsiElement elementAt = file.findElementAt(offset);
    if (elementAt == null) return null;

    PsiElement viewElement = elementAt.getParent();
    if (viewElement == null) return null;

    for (PsiReference reference : viewElement.getReferences()) {
      if (reference instanceof FileReference) {
        final FileReference lastReference = ((FileReference)reference).getFileReferenceSet().getLastReference();
        if (lastReference == null) break;

        PsiElement resolve = RecursionManager.doPreventingRecursion(viewElement, false, lastReference::resolve);

        if (resolve == gspFileToCompare) {
          return (GspFile)file;
        }

        break;
      }
    }

    return null;
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     PsiElement lastParent,
                                     @NotNull PsiElement place) {
    if (!super.processDeclarations(processor, state, lastParent, place)) return false;

    if (JavaScriptIntegrationUtil.isJSElement(place)) {
      GspFile gspLanguageRoot = (GspFile)getGspLanguageRoot().getOriginalFile();
      if (!gspLanguageRoot.processJsInJavascriptTags(processor, state, place)) return false;

      if (GrailsUtils.getTemplateName(getName()) != null) {
        List<GspFile> includingFilesList = CachedValuesManager.getCachedValue(gspLanguageRoot, () -> CachedValueProvider.Result.create(
          getIncludingFiles(gspLanguageRoot),
          PsiModificationTracker.MODIFICATION_COUNT
        ));

        for (GspFile gspFile : includingFilesList) {
          if (!ScriptSupportUtil.processDeclarations(gspFile.getHtmlLanguageRoot(), processor, state, null, place)) return false;
          if (!gspFile.processJsInJavascriptTags(processor, state, place)) return false;
        }
      }
    }

    return true;
  }

  @Override
  public boolean isTemplateDataFile() {
    return true;
  }
}
