package org.jetbrains.jps.gwt.index.impl;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtilRt;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.index.GwtModuleXmlConstants;
import org.jetbrains.jps.gwt.index.JpsGwtModule;
import org.jetbrains.jps.incremental.artifacts.impl.JpsArtifactPathUtil;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaDependenciesEnumerator;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JpsGwtModuleImpl implements JpsGwtModule {
  private final JpsModule myModule;
  private final Path moduleFile;
  private final boolean myInTests;
  private final List<String> myPublicPaths;
  private final List<String> mySourcePaths;
  private final List<String> myInheritedNames;
  private boolean myHasEntryPoints;
  private final String myOutputName;
  private final String myQualifiedName;
  private final String myModuleRootRelativePath;

  public JpsGwtModuleImpl(Path moduleFile, JpsModule module, File sourceRoot, boolean inTests) throws JDOMException, IOException {
    myModule = module;
    this.moduleFile = moduleFile;
    myInTests = inTests;
    myPublicPaths = new ArrayList<>();
    mySourcePaths = new ArrayList<>();
    myInheritedNames = new ArrayList<>();
    String relativePath = FileUtil.toSystemIndependentName(FileUtil.getRelativePath(sourceRoot, moduleFile.toFile()));
    myModuleRootRelativePath = PathUtilRt.getParentPath(relativePath);
    myQualifiedName = StringUtil.trimEnd(relativePath, GwtModuleXmlConstants.GWT_XML_SUFFIX).replace('/', '.');
    Element root = JDOMUtil.load(moduleFile);
    String renameTo = root.getAttributeValue("rename-to");
    myOutputName = renameTo != null ? renameTo : myQualifiedName;
    for (Element child : root.getChildren()) {
      String name = child.getName();
      if (name.equals("public")) {
        ContainerUtil.addIfNotNull(myPublicPaths, child.getAttributeValue("path"));
      }
      else if (name.equals("source") || name.equals("super-source")) {
        ContainerUtil.addIfNotNull(mySourcePaths, child.getAttributeValue("path"));
      }
      else if (name.equals("entry-point")) {
        myHasEntryPoints = true;
      }
      else if (name.equals("inherits")) {
        ContainerUtil.addIfNotNull(myInheritedNames, child.getAttributeValue("name"));
      }
    }
  }

  @Override
  public boolean isInTests() {
    return myInTests;
  }

  @Override
  public boolean hasEntryPoints() {
    return myHasEntryPoints;
  }

  @Override
  public String getOutputName() {
    return myOutputName;
  }

  @Override
  public String getQualifiedName() {
    return myQualifiedName;
  }

  @Override
  public @NotNull Path getModuleFile() {
    return moduleFile;
  }

  @Override
  public @NotNull JpsModule getModule() {
    return myModule;
  }

  private List<File> getRoots(List<String> originalRelativePaths, String defaultPath, final boolean includeTests) {
    final List<String> relativePaths = originalRelativePaths.isEmpty() ? Collections.singletonList(defaultPath) : originalRelativePaths;
    JpsJavaDependenciesEnumerator enumerator = JpsJavaExtensionService.dependencies(myModule);
    if (!includeTests) {
      enumerator.productionOnly();
    }
    final List<File> roots = new ArrayList<>();
    enumerator.recursively().forEachModule(module -> {
      for (JpsModuleSourceRoot sourceRoot : module.getSourceRoots()) {
        if (sourceRoot.getRootType().equals(JavaSourceRootType.SOURCE) || includeTests && sourceRoot.getRootType().equals(JavaSourceRootType.TEST_SOURCE)) {
          for (String relativePath : relativePaths) {
            String pathFromSourceRoot = JpsArtifactPathUtil.appendToPath(myModuleRootRelativePath, relativePath);
            File root = new File(sourceRoot.getFile(), FileUtil.toSystemDependentName(pathFromSourceRoot));
            if (root.isDirectory()) {
              roots.add(root);
            }
          }
        }
      }
    });
    return roots;
  }

  @Override
  public List<String> getInheritedNames() {
    return myInheritedNames;
  }

  @Override
  public @NotNull List<File> getPublicRoots(boolean includeTests) {
    return getRoots(myPublicPaths, GwtModuleXmlConstants.DEFAULT_PUBLIC_PATH, includeTests);
  }

  @Override
  public @NotNull List<File> getSourceRoots(boolean includeTests) {
    return getRoots(mySourcePaths, GwtModuleXmlConstants.DEFAULT_SOURCE_PATH, includeTests);
  }
}
