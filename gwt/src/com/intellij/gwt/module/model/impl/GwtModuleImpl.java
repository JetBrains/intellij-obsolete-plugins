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

package com.intellij.gwt.module.model.impl;

import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtFileSet;
import com.intellij.gwt.module.model.GwtInheritsEntry;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.module.model.GwtRelativePath;
import com.intellij.gwt.module.model.GwtStylesheetRef;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.UriUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.index.GwtModuleXmlConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public abstract class GwtModuleImpl implements GwtModule {
  private static final Logger LOG = Logger.getInstance(GwtModuleImpl.class);
  private String myCachedFileUrl;
  private VirtualFile myModuleFile;
  private String myName;
  private String myShortName;
  private VirtualFile myModuleDirectory;
  private GwtModulesManager myGwtModulesManager;

  @Override
  public String getOutputName() {
    final String renameTo = getRenameTo().getValue();
    if (renameTo != null) {
      return renameTo;
    }
    return getQualifiedName();
  }

  @Override
  public String getQualifiedName() {
    ensureInitialized();
    return myName;
  }

  private void ensureInitialized() {
    PsiFile psiFile = getModuleXmlFile();
    myModuleFile = psiFile.getOriginalFile().getVirtualFile();
    LOG.assertTrue(myModuleFile != null);
    if (myModuleFile.getUrl().equals(myCachedFileUrl)) {
      return;
    }

    myModuleDirectory = myModuleFile.getParent();
    LOG.assertTrue(myModuleDirectory != null);
    final Project project = psiFile.getProject();
    myGwtModulesManager = GwtModulesManager.getInstance(project);
    final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    VirtualFile sourceRoot = index.getSourceRootForFile(myModuleFile);
    if (sourceRoot == null) {
      sourceRoot = index.getClassRootForFile(myModuleFile);
    }
    String relativePath = sourceRoot != null ? VfsUtilCore.getRelativePath(myModuleFile, sourceRoot, '.') : myModuleFile.getName();
    if (relativePath == null) {
      LOG.error("Module file " + myModuleFile.getPath() + " is not under source root " + sourceRoot);
      return;
    }
    myName = relativePath.substring(0, relativePath.length() - GwtModuleXmlConstants.GWT_XML_SUFFIX.length());
    myShortName = StringUtil.getShortName(myName);
    myCachedFileUrl = myModuleFile.getUrl();
  }

  @Override
  public VirtualFile getModuleFile() {
    ensureInitialized();
    return myModuleFile;
  }

  @Override
  public XmlFile getModuleXmlFile() {
    return DomUtil.getFile(this);
  }


  @Override
  public VirtualFile getModuleDirectory() {
    ensureInitialized();
    return myModuleDirectory;
  }

  @Override
  public List<GwtModule> getInherited(final GlobalSearchScope scope) {
    ensureInitialized();
    final ArrayList<GwtModule> list = new ArrayList<>();
    for (GwtInheritsEntry inheritsEntry : getInheritses()) {
      final String value = inheritsEntry.getName().getValue();
      if (value != null) {
        GwtModule gwtModule = myGwtModulesManager.findGwtModuleByQualifiedName(value, scope);
        if (gwtModule != null) {
          list.add(gwtModule);
        }
      }
    }
    return list;
  }

  @Override
  public List<StylesheetFile> getStylesheetFiles() {
    ensureInitialized();
    List<StylesheetFile> list = new ArrayList<>();
    for (GwtStylesheetRef stylesheetRef : getStylesheets()) {
      ContainerUtil.addIfNotNull(list, stylesheetRef.getSrc().getValue());
    }
    return list;
  }

  @Override
  public String getShortName() {
    ensureInitialized();
    return myShortName;
  }

  @Override
  public Collection<VirtualFile> getSourceRoots(final boolean includeTests) {
    return getRootsByRelativePaths(getSources(), GwtModuleXmlConstants.DEFAULT_SOURCE_PATH, includeTests).keySet();
  }

  @Override
  public Collection<VirtualFile> getPublicRoots() {
    return getPublicRoots(true);
  }

  @Override
  public Collection<VirtualFile> getPublicRoots(final boolean includeTests) {
    return getRootsByRelativePaths(getPublics(), GwtModuleXmlConstants.DEFAULT_PUBLIC_PATH, includeTests).keySet();
  }

  @Override
  public Collection<VirtualFile> getSuperSourceRoots(final boolean includeTests) {
    return getRootsByRelativePaths(getSuperSources(), null, includeTests).keySet();
  }

  private List<VirtualFile> getGwtModuleRoots(final boolean includeTests) {
    PsiManager psiManager = PsiManager.getInstance(getManager().getProject());
    PsiDirectory psiDirectory = psiManager.findDirectory(myModuleDirectory);
    Module module = getModule();
    if (module != null && psiDirectory != null) {
      PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
      if (aPackage != null) {
        GlobalSearchScope baseScope = GlobalSearchScope.moduleWithDependenciesScope(module);
        GlobalSearchScope scope = includeTests ? baseScope : baseScope.intersectWith(GlobalSearchScopesCore.projectProductionScope(module.getProject()));
        PsiDirectory[] directories = aPackage.getDirectories(scope);
        if (directories.length > 0) {
          List<VirtualFile> roots = new ArrayList<>(directories.length);
          for (PsiDirectory directory : directories) {
            roots.add(directory.getVirtualFile());
          }
          return roots;
        }
      }
    }
    return Collections.singletonList(myModuleDirectory);
  }

  private Map<VirtualFile, GwtRelativePath> getRootsByRelativePaths(final List<GwtRelativePath> relativePaths,
                                                                    final @Nullable String defaultPath, final boolean includeTests) {
    ensureInitialized();
    final Map<VirtualFile, GwtRelativePath> roots = new LinkedHashMap<>();

    for (VirtualFile moduleRoot : getGwtModuleRoots(includeTests)) {
      if (relativePaths.isEmpty() && defaultPath != null) {
        final VirtualFile file = moduleRoot.findFileByRelativePath(defaultPath);
        if (file != null) {
          roots.put(file, null);
        }
      }

      for (GwtRelativePath relativePath : relativePaths) {
        final String pathValue = relativePath.getPath().getValue();
        if (pathValue != null) {
          final VirtualFile file = moduleRoot.findFileByRelativePath(UriUtil.trimTrailingSlashes(FileUtil.toSystemIndependentName(pathValue)));
          if (file != null) {
            roots.put(file, relativePath);
          }
        }
      }
    }
    return roots;
  }

  @Override
  public boolean isSourceFile(@NotNull VirtualFile file) {
    return isIncluded(file, getSourceRoots());
  }

  @Override
  public Map<VirtualFile, GwtRelativePath> getSourceRoots() {
    return unmodifiableMap(getRootsByRelativePaths(getSources(), GwtModuleXmlConstants.DEFAULT_SOURCE_PATH, true));
  }

  @Override
  public Map<VirtualFile, GwtRelativePath> getSuperSourceRoots() {
    return unmodifiableMap(getRootsByRelativePaths(getSuperSources(), null, true));
  }

  private static boolean isIncluded(VirtualFile file, Map<VirtualFile, GwtRelativePath> roots) {
    for (Map.Entry<VirtualFile, GwtRelativePath> entry : roots.entrySet()) {
      final VirtualFile root = entry.getKey();
      if (VfsUtilCore.isAncestor(root, file, false) && isIncluded(file, root, entry.getValue())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isIncluded(@NotNull VirtualFile file, @NotNull VirtualFile root, @Nullable GwtRelativePath pathElement) {
    if (pathElement == null) return true;

    final String path = VfsUtilCore.getRelativePath(file, root, '/');
    if (path == null) return false;

    final Boolean caseSensitiveValue = pathElement.isCaseSensitive().getValue();
    boolean caseSensitive = caseSensitiveValue == null || caseSensitiveValue.booleanValue();

    final Boolean defaultExcludes = pathElement.isDefaultExcludes();
    if ((defaultExcludes == null || defaultExcludes.booleanValue()) && GwtFilePatternUtil.isExcludedByDefault(path, caseSensitive)) {
      return false;
    }

    if (isAccepted(path, pathElement.getExcludes(), caseSensitive) || isAccepted(path, pathElement.getExcludesAttribute().getValue(), caseSensitive)) {
      return false;
    }

    final List<GwtFileSet> includes = pathElement.getIncludes();
    final String includesAttribute = pathElement.getIncludesAttribute().getValue();
    return includes.isEmpty() && includesAttribute == null
        || isAccepted(path, includes, caseSensitive) || isAccepted(path, includesAttribute, caseSensitive);
  }

  private static boolean isAccepted(@NotNull String path, @Nullable String patternsString, boolean caseSensitive) {
    if (patternsString == null) return false;

    final String[] patterns = patternsString.split("(\\s|,)");
    for (String pattern : patterns) {
      if (!pattern.isEmpty() && GwtFilePatternUtil.createPattern(pattern, caseSensitive).matcher(path).matches()) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAccepted(@NotNull String path, List<GwtFileSet> fileSets, boolean caseSensitive) {
    for (GwtFileSet fileSet : fileSets) {
      if (fileSet.matches(path, caseSensitive)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isPublicFile(@NotNull VirtualFile file) {
    return isIncluded(file, getRootsByRelativePaths(getPublics(), GwtModuleXmlConstants.DEFAULT_PUBLIC_PATH, true));
  }
}
