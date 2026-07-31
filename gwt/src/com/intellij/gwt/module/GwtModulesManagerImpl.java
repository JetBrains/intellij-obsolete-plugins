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

package com.intellij.gwt.module;

import com.intellij.gwt.clientBundle.css.GwtCssDeclarationsManager;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.index.GwtHtmlFileIndex;
import com.intellij.gwt.module.index.GwtModuleRenameToIndex;
import com.intellij.gwt.module.model.GwtEntryPoint;
import com.intellij.gwt.module.model.GwtInheritsEntry;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.web.GwtWebUtil;
import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.css.CssRuleset;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.css.resolve.CssResolveManager;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.index.GwtModuleXmlConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GwtModulesManagerImpl extends GwtModulesManager {
  private static final @NonNls String CSS_EXTENSION = ".css";
  private static final Key<CachedValue<MultiMap<String, CssClass>>> CACHED_CSS_CLASS_DECLARATIONS = Key.create("CACHED_CSS_CLASS_DECLARATIONS");
  private static final Key<CachedValue<InheritedModulesInfo>> CACHED_GWT_INHERITED_MODULES = Key.create("CACHED_GWT_INHERITED_MODULES");
  private final Project myProject;
  private final ProjectFileIndex myProjectFileIndex;

  public GwtModulesManagerImpl(final Project project) {
    myProject = project;
    myProjectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
  }

  @Override
  public @NotNull List<GwtModule> getAllGwtModules() {
    return getGwtModules(GlobalSearchScope.allScope(myProject));
  }

  private List<GwtModule> getGwtModules(@NotNull GlobalSearchScope scope) {
    final GwtModulesFinder finder = new GwtModulesFinder(myProject);
    final Collection<VirtualFile> candidates = getGwtModuleFiles(scope);
    for (VirtualFile file : candidates) {
      if (myProjectFileIndex.isInSource(file) || myProjectFileIndex.isInLibraryClasses(file)) {
        finder.processFile(file);
      }
    }

    return finder.getResults();
  }

  @Override
  public @NotNull Collection<VirtualFile> getGwtModuleFiles(@NotNull GlobalSearchScope scope) {
    return DomService.getInstance().getDomFileCandidates(GwtModule.class, scope);
  }

  @Override
  public @NotNull List<GwtModule> getGwtModules(final @NotNull Module module, final boolean includeTests) {
    return getGwtModules(module.getModuleContentScope());
  }


  @Override
  public @Nullable GwtModule findGwtModuleByClientSourceFile(@NotNull VirtualFile file) {
    List<GwtModule> gwtModules = findGwtModulesByClientSourceFile(file);
    return !gwtModules.isEmpty() ? gwtModules.get(0) : null;
  }

  @Override
  public @NotNull List<GwtModule> findGwtModulesByClientSourceFile(final @NotNull VirtualFile file) {
    return findModulesByClientOrPublicFile(file, true, false);
  }

  private @NotNull List<GwtModule> findModulesByClientOrPublicFile(final VirtualFile file, final boolean clientFileAllowed, final boolean publicFileAllowed) {
    final GwtModulesFinder finder = new GwtModulesFinder(myProject);
    VirtualFile parent = file.getParent();
    while (parent != null && (myProjectFileIndex.isInSource(parent) || myProjectFileIndex.isInLibraryClasses(parent))) {
      finder.processChildren(parent);
      parent = parent.getParent();
    }

    ArrayList<GwtModule> gwtModules = new ArrayList<>();
    for (GwtModule module : finder.getResults()) {
      if (clientFileAllowed && module.isSourceFile(file)) {
        gwtModules.add(module);
      }
      if (publicFileAllowed && module.isPublicFile(file)) {
        gwtModules.add(module);
        break;
      }
    }
    return gwtModules;
  }

  @Override
  public XmlFile @NotNull [] findHtmlFilesByModule(@NotNull GwtModule module) {
    final Collection<VirtualFile> htmlFiles = getAllHtmlFiles(module);
    if (htmlFiles.isEmpty()) return XmlFile.EMPTY_ARRAY;

    final VirtualFile parent = module.getModuleDirectory();
    final VirtualFile defaultFile = parent.findFileByRelativePath(
      GwtModuleXmlConstants.DEFAULT_PUBLIC_PATH + "/" + module.getShortName() + "." + HtmlFileType.INSTANCE.getDefaultExtension());

    List<XmlFile> result = new ArrayList<>();
    for (VirtualFile file : htmlFiles) {
      final FileViewProvider viewProvider = PsiManager.getInstance(myProject).findViewProvider(file);
      if (viewProvider != null) {
        final XmlFile xmlFile = (XmlFile)viewProvider.getPsi(HTMLLanguage.INSTANCE);
        if (xmlFile != null) {
          if (file.equals(defaultFile)) {
            result.add(0, xmlFile);
          }
          else {
            result.add(xmlFile);
          }
        }
      }
    }
    return result.toArray(XmlFile.EMPTY_ARRAY);
  }

  @Override
  public @NotNull Collection<VirtualFile> getAllHtmlFiles(GwtModule module) {
    return GwtHtmlFileIndex.getHtmlFilesByModule(myProject, module.getOutputName());
  }

  @Override
  public @Nullable PsiElement findTagById(@NotNull XmlFile htmlFile, final String id) {
    final Map<String, XmlTag> id2Tag = getHtmlId2TagMap(htmlFile);
    return id2Tag.get(id);
  }

  @Override
  public Collection<GwtModule> getGwtModuleToCompile(Module module, boolean includeTests) {
    final List<GwtModule> result = new ArrayList<>();
    GwtFacet facet = GwtFacet.getInstance(module);
    for (GwtModule gwtModule : getGwtModules(module, includeTests)) {
      if (!isLibraryModule(gwtModule) && (facet == null || facet.getConfiguration().isModuleCompilationEnabled(gwtModule))) {
        result.add(gwtModule);
      }
    }
    return result;
  }

  @Override
  public Collection<GwtModule> getCompilableGwtModules(Module module, boolean includeTests) {
    final List<GwtModule> result = new ArrayList<>();
    for (GwtModule gwtModule : getGwtModules(module, includeTests)) {
      if (!isLibraryModule(gwtModule)) {
        result.add(gwtModule);
      }
    }
    return result;
  }

  private static Map<String, XmlTag> getHtmlId2TagMap(final XmlFile htmlFile) {
    final Map<String, XmlTag> id2Tag = new HashMap<>();
    htmlFile.accept(new XmlRecursiveElementVisitor() {
      @Override public void visitXmlTag(@NotNull XmlTag tag) {
        final String elementId = tag.getAttributeValue("id");
        if (elementId != null) {
          id2Tag.put(elementId, tag);
        }
        super.visitXmlTag(tag);
      }
    });
    return id2Tag;
  }

  @Override
  public @NotNull Set<CssClass> findCssClasses(final GwtModule module, final String className) {
    Set<CssClass> result = new HashSet<>();
    FileManager fileManager = PsiManagerEx.getInstanceEx(myProject).getFileManager();
    for (VirtualFile moduleFile : getInheritedModules(module).getModuleFiles()) {
      PsiFile psiFile = fileManager.findFile(moduleFile);
      if (psiFile != null) {
        GwtModule gwtModule = getGwtModuleByXmlFile(psiFile);
        if (gwtModule != null) {
          result.addAll(getCssClass2DeclarationMap(gwtModule).get(className));
        }
      }
    }
    return result;
  }

  private MultiMap<String, CssClass> getCssClass2DeclarationMap(final GwtModule module) {
    CachedValue<MultiMap<String, CssClass>> cachedValue = module.getModuleXmlFile().getUserData(CACHED_CSS_CLASS_DECLARATIONS);
    if (cachedValue == null) {
      cachedValue =
        CachedValuesManager.getManager(myProject).createCachedValue(() -> computeCssDeclarations(module), false);
      module.getModuleXmlFile().putUserData(CACHED_CSS_CLASS_DECLARATIONS, cachedValue);
    }
    return cachedValue.getValue();
  }

  private CachedValueProvider.Result<MultiMap<String, CssClass>> computeCssDeclarations(GwtModule module) {
    final MultiMap<String, CssClass> cssClass2Declaration = new MultiMap<>();
    final List<StylesheetFile> list = module.getStylesheetFiles();
    for (StylesheetFile stylesheetFile : list) {
      GwtCssDeclarationsManager.collectDeclarations(stylesheetFile, CssClass.class, cssClass2Declaration);
    }

    final XmlFile[] htmlFiles = findHtmlFilesByModule(module);
    final CssResolveManager resolveManager = CssResolveManager.getInstance();
    for (XmlFile htmlFile : htmlFiles) {
      for (CssRuleset cssRuleset : resolveManager.getNewResolver().resolveAll(htmlFile)) {
        GwtCssDeclarationsManager.collectCssClasses(cssRuleset, cssClass2Declaration);
      }
    }
    return CachedValueProvider.Result.create(cssClass2Declaration, PsiModificationTracker.MODIFICATION_COUNT,
                                             ProjectRootManager.getInstance(myProject));
  }

  @Override
  public String[] getAllCssClassNames(final GwtModule module) {
    final Set<String> classesSet = new HashSet<>();
    FileManager fileManager = PsiManagerEx.getInstanceEx(myProject).getFileManager();
    for (VirtualFile moduleFile : getInheritedModules(module).getModuleFiles()) {
      PsiFile psiFile = fileManager.findFile(moduleFile);
      if (psiFile != null) {
        GwtModule gwtModule = getGwtModuleByXmlFile(psiFile);
        if (gwtModule != null) {
          classesSet.addAll(getCssClass2DeclarationMap(gwtModule).keySet());
        }
      }
    }
    return ArrayUtilRt.toStringArray(classesSet);
  }

  @Override
  public @Nullable StylesheetFile findPreferableCssFile(final GwtModule module) {
    final List<StylesheetFile> list = module.getStylesheetFiles();
    if (!list.isEmpty()) {
      return list.get(0);
    }

    final XmlFile[] htmlFiles = findHtmlFilesByModule(module);
    StylesheetFile result = null;
    for (XmlFile htmlFile : htmlFiles) {
      final StylesheetFile[] files = CssResolveManager.getInstance().getNewResolver().resolveStyleSheets(htmlFile, null);
      final String expectedFileName = module.getShortName() + CSS_EXTENSION;
      for (StylesheetFile cssFile : files) {
        if (expectedFileName.equals(cssFile.getName())) {
          return cssFile;
        }
      }
      if (files.length > 0 && result == null) {
        result = files[0];
      }
    }
    return result;
  }

  @Override
  public boolean isInheritedOrSelf(GwtModule gwtModule, GwtModule inheritedModule) {
    final Set<String> names = getInheritedModules(gwtModule).getModuleNames();
    return names.contains(inheritedModule.getQualifiedName());
  }

  @Override
  public @NotNull Collection<GwtModule> findAllInheritedModules(@NotNull Collection<GwtModule> modules, @NotNull GlobalSearchScope scope) {
    Set<String> allNames = new LinkedHashSet<>();
    for (GwtModule module : modules) {
      allNames.addAll(getInheritedModules(module).getModuleNames());
    }
    List<GwtModule> result = new ArrayList<>();
    for (String name : allNames) {
      ContainerUtil.addIfNotNull(result, findGwtModuleByQualifiedName(name, scope));
    }
    return result;
  }

  private InheritedModulesInfo getInheritedModules(final GwtModule gwtModule) {
    CachedValue<InheritedModulesInfo> cachedValue = getGwtInheritedModulesCache(gwtModule);
    if (cachedValue == null) {
      cachedValue = CachedValuesManager.getManager(myProject).createCachedValue(() -> {
        Module module = gwtModule.getModule();
        GlobalSearchScope scope =
          module != null ? GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module) : GlobalSearchScope.allScope(myProject);
        InheritedModulesInfo inheritedModules = new InheritedModulesInfo();
        collectAllInherited(gwtModule, scope, inheritedModules);
        return CachedValueProvider.Result.create(inheritedModules, inheritedModules.generateDependenciesArray(myProject));
      }, false);
      putGwtInheritedModulesCache(gwtModule, cachedValue);
    }
    return cachedValue.getValue();
  }

  private void collectAllInherited(GwtModule gwtModule, GlobalSearchScope scope, InheritedModulesInfo result) {
    VirtualFile moduleVirtualFile = gwtModule.getModuleXmlFile().getVirtualFile();
    if (result.getModuleFiles().contains(moduleVirtualFile)) return;

    result.addGwtModule(gwtModule);

    boolean moduleIsInLibraryClasses = myProjectFileIndex.isInLibraryClasses(moduleVirtualFile);
    for (GwtInheritsEntry inheritsEntry : gwtModule.getInheritses()) {
      String inheritedModuleName = inheritsEntry.getName().getValue();
      if (inheritedModuleName == null) continue;
      if (result.getModuleNames().contains(inheritedModuleName)) continue;

      GwtModule inheritedModule = findGwtModuleByQualifiedName(inheritedModuleName, scope);
      if (inheritedModule == null) continue;

      InheritedModulesInfo inheritedModulesInfo = null;
      if (!moduleIsInLibraryClasses && myProjectFileIndex.isInLibraryClasses(inheritedModule.getModuleXmlFile().getVirtualFile())) {
        // here we cache all in-library modules that are inherited from project modules. These are usually color schemes or other modules
        // like "com.google.gwt.user.User" that could be inherited many times across the project
        inheritedModulesInfo = getInheritedModules(inheritedModule);
      } else {
        CachedValue<InheritedModulesInfo> cachedValue = getGwtInheritedModulesCache(gwtModule);
        if (cachedValue != null && cachedValue.hasUpToDateValue()) {
          // read value of up-to-date cache only to prevent infinite recursion in case of loops in modules hierarchy
          inheritedModulesInfo = cachedValue.getValue();
        }
      }

      if (inheritedModulesInfo != null) {
        result.merge(inheritedModulesInfo);
      } else {
        collectAllInherited(inheritedModule, scope, result);
      }
    }
  }

  @Override
  public boolean isLibraryModule(GwtModule module) {
    return module.getEntryPoints().isEmpty() && getAllHtmlFiles(module).isEmpty() && !getInheritedModules(module).hasEntryPoints();
  }

  @Override
  public boolean isUnderGwtModule(final VirtualFile file) {
    final GwtModulesFinder finder = new GwtModulesFinder(myProject);
    VirtualFile parent = file.getParent();
    while (parent != null && myProjectFileIndex.isInSource(parent)) {
      finder.processChildren(parent);
      parent = parent.getParent();
    }
    return !finder.getResults().isEmpty();
  }

  @Override
  public @Nullable GwtModule findGwtModuleByQualifiedName(final @NotNull String qualifiedName, final GlobalSearchScope scope) {
    final List<GwtModule> gwtModules = findGwtModulesByQualifiedName(qualifiedName, scope);
    return gwtModules.isEmpty() ? null : gwtModules.get(0);
  }

  @Override
  public @NotNull Collection<GwtModule> findGwtModulesByOutputName(@NotNull String outputName, GlobalSearchScope scope) {
    Set<GwtModule> gwtModules = new HashSet<>();
    gwtModules.addAll(findGwtModulesByQualifiedName(outputName, scope));
    final PsiManager psiManager = PsiManager.getInstance(myProject);
    for (VirtualFile file : GwtModuleRenameToIndex.getGwtXmlFiles(outputName, scope)) {
      final PsiFile psiFile = psiManager.findFile(file);
      if (psiFile instanceof XmlFile) {
        ContainerUtil.addIfNotNull(gwtModules, getGwtModuleByXmlFile(psiFile));
      }
    }
    return gwtModules;
  }

  private static @Nullable String getPathFromPublicRoot(final @NotNull GwtModule gwtModule, @NotNull VirtualFile file) {
    for (VirtualFile root : gwtModule.getPublicRoots()) {
      if (VfsUtilCore.isAncestor(root, file, false)) {
        return VfsUtilCore.getRelativePath(file, root, '/');
      }
    }
    return null;
  }

  @Override
  public List<GwtModule> findGwtModulesByQualifiedName(final String qualifiedName, final GlobalSearchScope scope) {
    List<GwtModule> modules = new ArrayList<>();
    String name = qualifiedName;
    String packageName = "";
    do {
      final PsiPackage psiPackage = JavaPsiFacade.getInstance(myProject).findPackage(packageName);
      if (psiPackage != null) {
        final PsiDirectory[] directories = psiPackage.getDirectories(scope);
        for (PsiDirectory directory : directories) {
          final PsiFile psiFile = directory.findFile(name + GwtModuleXmlConstants.GWT_XML_SUFFIX);
          if (psiFile instanceof XmlFile) {
            final DomFileElement<GwtModule> fileElement = DomManager.getDomManager(myProject).getFileElement((XmlFile)psiFile, GwtModule.class);
            if (fileElement != null) {
              modules.add(fileElement.getRootElement());
            }
          }
        }
      }

      int dot = name.indexOf('.');
      if (dot == -1) break;

      final String shortName = name.substring(0, dot);
      packageName = !packageName.isEmpty() ? packageName + "." + shortName : shortName;
      name = name.substring(dot + 1);
    } while(true);

    return modules;
  }

  @Override
  public String[] getAllIds(@NotNull XmlFile htmlFile) {
    final Set<String> idSet = getHtmlId2TagMap(htmlFile).keySet();
    return ArrayUtilRt.toStringArray(idSet);
  }

  @Override
  public @NotNull List<GwtModule> findModulesByClass(final @NotNull PsiElement context, final @Nullable String className) {
    if (className == null) return Collections.emptyList();

    PsiClass[] psiClasses = JavaPsiFacade.getInstance(context.getProject()).findClasses(className, context.getResolveScope());
    for (PsiClass psiClass : psiClasses) {
      PsiFile psiFile = psiClass.getContainingFile();
      if (psiFile != null) {
        VirtualFile file = psiFile.getVirtualFile();
        if (file != null) {
          List<GwtModule> modules = findGwtModulesByClientSourceFile(file);
          if (!modules.isEmpty()) {
            return modules;
          }
        }
      }
    }
    return Collections.emptyList();
  }

  @Override
  public GwtModule findGwtModuleByEntryPoint(final @NotNull PsiClass psiClass) {
    PsiFile psiFile = psiClass.getContainingFile();
    if (psiFile == null) return null;

    VirtualFile file = psiFile.getVirtualFile();
    if (file == null) return null;

    List<GwtModule> gwtModules = findGwtModulesByClientSourceFile(file);
    for (GwtModule gwtModule : gwtModules) {
      List<GwtEntryPoint> entryPoints = gwtModule.getEntryPoints();
      for (GwtEntryPoint entryPoint : entryPoints) {
        String className = entryPoint.getEntryClass().getValue();
        if (className != null && className.equals(psiClass.getQualifiedName())) {
          return gwtModule;
        }
      }
    }
    return null;
  }

  @Override
  public GwtModule getGwtModule(@NotNull PsiFile gwtXmlFile) {
    final VirtualFile file = gwtXmlFile.getVirtualFile();
    if (file == null) return null;

    final GwtModulesFinder finder = new GwtModulesFinder(myProject);
    finder.processFile(file);
    return ContainerUtil.getFirstItem(finder.getResults(), null);
  }

  @Override
  public @NotNull List<Pair<GwtModule, String>> findGwtModulesByPublicFile(final @NotNull VirtualFile file) {
    List<GwtModule> gwtModules = findModulesByClientOrPublicFile(file, false, true);
    List<Pair<GwtModule, String>> pairs = new ArrayList<>();
    for (GwtModule gwtModule : gwtModules) {
      String path = getPathFromPublicRoot(gwtModule, file);
      if (path != null) {
        pairs.add(Pair.create(gwtModule, path));
      }
    }
    return pairs;
  }

  @Override
  public @Nullable GwtModule getGwtModuleByXmlFile(@NotNull PsiFile file) {
    if (file instanceof XmlFile) {
      DomFileElement<GwtModule> fileElement = DomManager.getDomManager(myProject).getFileElement((XmlFile)file, GwtModule.class);
      if (fileElement != null) {
        return fileElement.getRootElement();
      }
    }
    return null;
  }

  @Override
  public boolean isInheritedOrSelf(final GwtModule gwtModule, final List<GwtModule> referencedModules) {
    for (GwtModule referencedModule : referencedModules) {
      if (isInheritedOrSelf(gwtModule, referencedModule)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nullable String getOutputPath(@NotNull GwtModule gwtModule, @NotNull VirtualFile file) {
    final String path = getPathFromPublicRoot(gwtModule, file);
    if (path != null) {
      return GwtWebUtil.getOutputPath(gwtModule, path);
    }
    final GwtFacet gwtFacet = GwtFacet.getInstance(gwtModule);
    if (gwtFacet != null && gwtFacet.getSdkVersion().isHtmlFilesOutsideSourcesAreAllowed()) {
      return GwtWebUtil.getRelativeToWebRootPath(file, myProject);
    }
    return null;
  }

  private static class GwtModulesFinder implements ContentIterator {
    private final List<GwtModule> myResults;
    private final PsiManager myPsiManager;
    private final DomManager myDomManager;

    GwtModulesFinder(final Project project) {
      myResults = new ArrayList<>();
      myPsiManager = PsiManager.getInstance(project);
      myDomManager = DomManager.getDomManager(project);
    }

    @Override
    public boolean processFile(@NotNull VirtualFile fileOrDir) {
      if (!fileOrDir.isDirectory() && FileTypeRegistry.getInstance().isFileOfType(fileOrDir, XmlFileType.INSTANCE) &&
          fileOrDir.getNameWithoutExtension().endsWith(GwtModuleXmlConstants.GWT_SUFFIX)) {
        final PsiFile psiFile = myPsiManager.findFile(fileOrDir);
        if (psiFile instanceof XmlFile) {
          final DomFileElement<GwtModule> fileElement = myDomManager.getFileElement((XmlFile)psiFile, GwtModule.class);
          if (fileElement != null) {
            myResults.add(fileElement.getRootElement());
          }
        }
      }
      return true;
    }

    public List<GwtModule> getResults() {
      return myResults;
    }

    public void processChildren(final VirtualFile parent) {
      List<VirtualFile> directories = getDirectories(parent);

      for (VirtualFile directory : directories) {
        final VirtualFile[] files = directory.getChildren();
        if (files != null) {
          for (VirtualFile virtualFile : files) {
            processFile(virtualFile);
          }
        }
      }
    }

    private List<VirtualFile> getDirectories(final VirtualFile directory) {
      Module module = ModuleUtilCore.findModuleForFile(directory, myPsiManager.getProject());

      if (module != null) {
        PsiDirectory psiDirectory = myPsiManager.findDirectory(directory);
        if (psiDirectory != null) {
          PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
          if (psiPackage != null) {
            List<VirtualFile> directories = new ArrayList<>();
            PsiDirectory[] psiDirectories = psiPackage.getDirectories(module.getModuleWithDependentsScope());
            for (PsiDirectory dir : psiDirectories) {
              directories.add(dir.getVirtualFile());
            }
            return directories;
          }
        }
      }

      return Collections.singletonList(directory);
    }
  }

  private static class InheritedModulesInfo {
    private final Set<VirtualFile> myModuleFiles = new LinkedHashSet<>();
    private final Set<String> myModuleNames = new LinkedHashSet<>();
    private boolean myHasEntryPoints;

    public boolean hasEntryPoints() {
      return myHasEntryPoints;
    }

    public Set<VirtualFile> getModuleFiles() {
      return myModuleFiles;
    }

    public Set<String> getModuleNames() {
      return myModuleNames;
    }

    public void addGwtModule(GwtModule gwtModule) {
      myModuleFiles.add(gwtModule.getModuleXmlFile().getVirtualFile());
      myModuleNames.add(gwtModule.getQualifiedName());
      if (!myHasEntryPoints) {
        myHasEntryPoints = !gwtModule.getEntryPoints().isEmpty();
      }
    }

    public void merge(InheritedModulesInfo inheritedModulesInfo) {
      myModuleFiles.addAll(inheritedModulesInfo.getModuleFiles());
      myModuleNames.addAll(inheritedModulesInfo.getModuleNames());
      myHasEntryPoints |= inheritedModulesInfo.hasEntryPoints();
    }

    public Object[] generateDependenciesArray(Project project) {
      Object[] dependencies = new Object[myModuleFiles.size() + 1];
      dependencies[0] = ProjectRootManager.getInstance(project);

      int i = 1;
      for (VirtualFile moduleFile : myModuleFiles) {
        dependencies[i++] = moduleFile;
      }

      return dependencies;
    }
  }

  private static CachedValue<InheritedModulesInfo> getGwtInheritedModulesCache(GwtModule gwtModule) {
    return gwtModule.getModuleXmlFile().getUserData(CACHED_GWT_INHERITED_MODULES);
  }

  private static void putGwtInheritedModulesCache(GwtModule gwtModule, CachedValue<InheritedModulesInfo> cachedValue) {
    gwtModule.getModuleXmlFile().putUserData(CACHED_GWT_INHERITED_MODULES, cachedValue);
  }
}
