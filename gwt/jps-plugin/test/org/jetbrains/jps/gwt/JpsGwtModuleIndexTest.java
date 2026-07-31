package org.jetbrains.jps.gwt;

import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl;
import org.jetbrains.jps.gwt.index.JpsGwtModule;
import org.jetbrains.jps.gwt.index.JpsGwtModuleIndex;
import org.jetbrains.jps.gwt.index.impl.JpsGwtModuleIndexImpl;
import org.jetbrains.jps.gwt.model.JpsGwtModuleExtension;
import org.jetbrains.jps.gwt.model.impl.GwtModuleExtensionProperties;
import org.jetbrains.jps.gwt.model.impl.GwtModulePackagingProperties;
import org.jetbrains.jps.model.JpsModelTestCase;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaModuleType;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JpsGwtModuleIndexTest extends JpsModelTestCase {
  private static final String TEST_DATA_PATH = "plugins/GwtStudio/jps-plugin/testData/modules";

  public void testModule() {
    JpsGwtModuleExtension appExt = addModuleWithGwt("app", "src");
    JpsGwtModuleIndex index = createIndex();
    assertSameElements(getModuleNames(index.getModulesToCompile(appExt, false)), "com.app.App", "com.app.LibDep");
    JpsGwtModule app = assertOneElement(index.getModulesByName("com.app.App"));
    assertFalse(app.isInTests());
    assertTrue(app.hasEntryPoints());
    assertEquals("com.app.App", app.getOutputName());
    File sourceRoot = assertOneElement(app.getSourceRoots(false));
    assertTrue(FileUtil.filesEqual(sourceRoot, findFileUnderProjectHome("/src/com/app/client")));
    assertSameElements(app.getInheritedNames(), "com.google.gwt.user.User", "com.lib.Lib");

    JpsGwtModule module2 = assertOneElement(index.getModulesByName("com.app.NoEntryPoints"));
    assertEquals("MyOutput", module2.getOutputName());
    assertTrue(FileUtil.filesEqual(assertOneElement(module2.getSourceRoots(false)), findFileUnderProjectHome("/src/com/app/my-client")));
  }

  public void testDisableModule() {
    JpsModule module = addModuleWithGwt("app", "src").getModule();
    GwtModuleExtensionProperties properties = new GwtModuleExtensionProperties();
    GwtModulePackagingProperties packagingProperties = new GwtModulePackagingProperties();
    packagingProperties.myName = "com.app.App";
    packagingProperties.myEnabled = false;
    properties.myPackagingStates.add(packagingProperties);
    JpsGwtModuleExtension appExt = JpsGwtTestUtil.addGwtExtension(module, properties);

    JpsGwtModuleIndexImpl index = createIndex();
    assertSameElements(getModuleNames(index.getModulesToCompile(appExt, false)), "com.app.LibDep");
  }

  public void testRootsFromDepModule() {
    addTwoModules();

    JpsGwtModuleIndex index = createIndex();
    JpsGwtModule app = assertOneElement(index.getModulesByName("com.app.App"));
    assertSameElements(app.getSourceRoots(false), findFileUnderProjectHome("/src/com/app/client"), findFileUnderProjectHome("/dep/src/com/app/client"));
    assertSameElements(app.getSourceRoots(true), findFileUnderProjectHome("/src/com/app/client"), findFileUnderProjectHome("/dep/src/com/app/client"),
                       findFileUnderProjectHome("/dep/testSrc/com/app/client"));
  }

  public void testInheritedModules() {
    addTwoModules();
    JpsGwtModuleIndexImpl index = createIndex();
    Collection<JpsGwtModule> inherited = index.getInheritedModules(assertOneElement(index.getModulesByName("com.app.App")));
    assertEquals("com.lib.Lib", assertOneElement(inherited).getQualifiedName());
  }

  private void addTwoModules() {
    JpsGwtModuleExtension appExt = addModuleWithGwt("app", "src");
    JpsGwtModuleExtension depExt = addModuleWithGwt("dep", "dep/src");
    depExt.getModule().addSourceRoot(getUrlByRelativePathToProjectHome(TEST_DATA_PATH + "/dep/testSrc"), JavaSourceRootType.TEST_SOURCE);
    appExt.getModule().getDependenciesList().addModuleDependency(depExt.getModule());
  }

  private JpsGwtModuleExtension addModuleWithGwt(final String name, final String srcPath) {
    JpsModule module = myProject.addModule(name, JpsJavaModuleType.INSTANCE);
    module.addSourceRoot(getUrlByRelativePathToProjectHome(TEST_DATA_PATH + "/" + srcPath), JavaSourceRootType.SOURCE);
    return JpsGwtTestUtil.addGwtExtension(module);
  }

  private JpsGwtModuleIndexImpl createIndex() {
    return new JpsGwtModuleIndexImpl(myModel, new BuildDataPathsImpl(Path.of(".")));
  }

  private File findFileUnderProjectHome(final String relativePath) {
    return PathManagerEx.findFileUnderProjectHome(TEST_DATA_PATH + relativePath, getClass());
  }

  private static List<String> getModuleNames(Collection<JpsGwtModule> modules) {
    List<String> result = new ArrayList<>();
    for (JpsGwtModule module : modules) {
      result.add(module.getQualifiedName());
    }
    return result;
  }

  private String getUrlByRelativePathToProjectHome(final String path) {
    File file = PathManagerEx.findFileUnderProjectHome(path, getClass());
    return JpsPathUtil.pathToUrl(FileUtil.toSystemIndependentName(file.getAbsolutePath()));
  }
}
