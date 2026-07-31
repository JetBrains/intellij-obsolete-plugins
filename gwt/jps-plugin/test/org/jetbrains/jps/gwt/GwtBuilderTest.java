package org.jetbrains.jps.gwt;

import com.google.gwt.dev.Compiler;
import com.intellij.openapi.application.PathManager;
import com.intellij.util.PathUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.BuildResult;
import org.jetbrains.jps.builders.CompileScopeTestBuilder;
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl;
import org.jetbrains.jps.cmdline.ProjectDescriptor;
import org.jetbrains.jps.gwt.build.GwtBuildTarget;
import org.jetbrains.jps.gwt.build.GwtBuildTargetType;
import org.jetbrains.jps.gwt.build.GwtBuilder;
import org.jetbrains.jps.gwt.build.JpsGwtCompilerPaths;
import org.jetbrains.jps.gwt.model.JpsGwtExtensionService;
import org.jetbrains.jps.gwt.model.impl.JpsGwtCompilerOutputPackagingElement;
import org.jetbrains.jps.gwt.model.impl.JpsGwtExtensionServiceImpl;
import org.jetbrains.jps.incremental.artifacts.ArtifactBuilderTestCase;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.java.JpsJavaLibraryType;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.module.JpsModule;

import java.nio.file.Path;

import static com.intellij.util.io.TestFileSystemBuilder.fs;
import static org.jetbrains.jps.builders.CompileScopeTestBuilder.make;
import static org.jetbrains.jps.incremental.artifacts.LayoutElementTestUtil.root;

public class GwtBuilderTest extends ArtifactBuilderTestCase {
  private static final String EMPTY_MODULE = "<module></module>";
  private static final String ENTRY_POINT = "<entry-point class='com.app.client.App'/>";
  private static final String MODULE_WITH_ENTRY_POINT = "<module>" + ENTRY_POINT + "</module>";

  public void testSimple() {
    String file = createFile("src/A.gwt.xml", MODULE_WITH_ENTRY_POINT);
    JpsArtifact a = createGwtModuleAndArtifact("m", PathUtilRt.getParentPath(file));
    rebuildGwtProject();
    assertOutput(a, fs().dir("A").file("A.html"));

    makeGwtProject().assertUpToDate();

    delete(file);
    makeGwtProject();
    assertOutput(a, fs().dir("A"));
  }

  public void testSkipCompilationIfSingleFileIsCompiled() {
    String file = createFile("src/A.gwt.xml", MODULE_WITH_ENTRY_POINT);
    JpsModule module = createGwtModule("m", PathUtilRt.getParentPath(file));
    GwtBuildTarget gwtBuildTarget = new GwtBuildTarget(JpsGwtExtensionService.getInstance().getExtension(module));
    doBuild(CompileScopeTestBuilder.recompile().file(gwtBuildTarget, file)).assertSuccessful();
    assertOutput(JpsGwtCompilerPaths.getCompilerOutputRoot(gwtBuildTarget, new BuildDataPathsImpl(myDataStorageRoot.toPath())).getPath(),
                 fs());
  }

  public void testMakeAfterChange() {
    String gwtModuleFile = createFile("src/A.gwt.xml", MODULE_WITH_ENTRY_POINT);
    createGwtModuleAndArtifact("m", PathUtilRt.getParentPath(gwtModuleFile));
    rebuildGwtProject();

    change(gwtModuleFile);
    makeGwtProject();
    assertGwtModuleRecompiled("src/A.gwt.xml");
    assertDeletedAndCopied(new String[] {
      "out/artifacts/m/A/A.html",
      getGwtOutputFilePath("m", "A"),
    }, getGwtOutputFilePath("m", "A"));
    makeGwtProject().assertUpToDate();

    String file = createFile("src/client/A.java", "package client; class A{}");
    makeGwtProject();
    assertDeletedAndCopied("out/artifacts/m/A/A.html",
                           getGwtOutputFilePath("m", "A"));
    makeGwtProject().assertUpToDate();

    delete(file);
    makeGwtProject();
    assertGwtModuleRecompiled("src/A.gwt.xml");
    makeGwtProject().assertUpToDate();

    createFile("src/server/B.java", "package server; class B{}");
    makeGwtProject();
    assertNoGwtModulesRecompiled();
  }

  public void testRemoveAllSourceFiles() {
    String moduleFile = createFile("src/A.gwt.xml", MODULE_WITH_ENTRY_POINT);
    String sourceFile = createFile("src/client/A.java", "package client; class A{}");
    JpsArtifact a = createGwtModuleAndArtifact("m", PathUtilRt.getParentPath(moduleFile));
    rebuildGwtProject();
    assertOutput(a, fs().dir("A").file("A.html"));

    delete(moduleFile);
    delete(sourceFile);
    makeGwtProject();
    assertOutput(a, fs().dir("A"));
  }

  public void testChangeInInheritedGwtModule() {
    String mainFile = createFile("src/main/A.gwt.xml", "<module>" + ENTRY_POINT + "<inherits name=\"dep.B\"/>" + "</module>");
    createFile("src/dep/B.gwt.xml", EMPTY_MODULE);
    String depFile = createFile("src/dep/client/B.java", "package dep.client; class B{}");
    JpsArtifact m = createGwtModuleAndArtifact("m", PathUtilRt.getParentPath(PathUtilRt.getParentPath(mainFile)));
    rebuildGwtProject();
    assertOutput(m, fs().dir("main.A").file("main.A.html"));

    change(depFile);
    makeGwtProject();
    assertGwtModuleRecompiled("src/main/A.gwt.xml");
    makeGwtProject().assertUpToDate();

    delete(depFile);
    makeGwtProject();
    assertGwtModuleRecompiled("src/main/A.gwt.xml");
    makeGwtProject().assertUpToDate();
  }

  public void testChangeInDependentModule() {
    String mainFile = createFile("src/A.gwt.xml", MODULE_WITH_ENTRY_POINT);
    JpsModule main = createGwtModule("m", PathUtilRt.getParentPath(mainFile));
    String depFile = createFile("dep/src/client/Dep.java", "class Dep{}");
    JpsModule dep = createGwtModule("dep", PathUtilRt.getParentPath(PathUtilRt.getParentPath(depFile)));
    main.getDependenciesList().addModuleDependency(dep);
    addArtifact("m", root().element(gwtOutput(main)));
    rebuildGwtProject();
    assertGwtModuleRecompiled("src/A.gwt.xml");

    change(depFile);
    makeGwtProject();
    assertGwtModuleRecompiled("src/A.gwt.xml");
    makeGwtProject().assertUpToDate();
  }

  public void testChangeInInheritedModuleInDependentModule() {
    String mainFile = createFile("src/main/A.gwt.xml", "<module>" + ENTRY_POINT + "<inherits name=\"dep.B\"/>" + "</module>");
    JpsModule main = createGwtModule("m", PathUtilRt.getParentPath(PathUtilRt.getParentPath(mainFile)));

    String depFile = createFile("depSrc/dep/B.gwt.xml", EMPTY_MODULE);
    JpsModule dep = createGwtModule("dep", PathUtilRt.getParentPath(PathUtilRt.getParentPath(depFile)));
    String depJavaFile = createFile("depSrc/dep/client/B.java", "package dep.client; class B{}");
    main.getDependenciesList().addModuleDependency(dep);

    JpsArtifact m = addArtifact("m", root().element(gwtOutput(main)));
    rebuildGwtProject();
    assertOutput(m, fs().dir("main.A").file("main.A.html"));

    change(depJavaFile);
    makeGwtProject();
    assertGwtModuleRecompiled("src/main/A.gwt.xml");
    makeGwtProject().assertUpToDate();
  }

  private void assertNoGwtModulesRecompiled() {
    assertGwtModuleRecompiled();
  }

  private void assertGwtModuleRecompiled(String... gwtModulePaths) {
    assertCompiled(GwtBuilder.ID, gwtModulePaths);
  }

  private static String getGwtOutputFilePath(String moduleName, String gwtModuleName) {
    return "targets/gwt/" + moduleName + "_" + Integer.toHexString(moduleName.hashCode())+ "/gwt-output/" + gwtModuleName + "/" + gwtModuleName + ".html";
  }

  private JpsArtifact createGwtModuleAndArtifact(final String name, final String srcRoot) {
    JpsModule module = createGwtModule(name, srcRoot);
    return addArtifact(name, root().element(gwtOutput(module)));
  }

  private JpsModule createGwtModule(final String moduleName, String srcRoot) {
    JpsModule m = addModule(moduleName, srcRoot);
    addCompilerLibrary(m);
    JpsGwtTestUtil.addGwtExtension(m);
    return m;
  }

  private static void addCompilerLibrary(JpsModule module) {
    Path path = PathManager.getJarForClass(Compiler.class);
    JpsLibrary library = module.addModuleLibrary("gwt-compiler", JpsJavaLibraryType.INSTANCE);
    library.addRoot(path, JpsOrderRootType.COMPILED);
    module.getDependenciesList().addLibraryDependency(library);
  }

  private static JpsGwtCompilerOutputPackagingElement gwtOutput(JpsModule m) {
    return new JpsGwtCompilerOutputPackagingElement(m.createReference(), JpsGwtCompilerOutputPackagingElement.OutputKind.REGULAR);
  }

  private void rebuildGwtProject() {
    doBuild(CompileScopeTestBuilder.rebuild().allModules().allArtifacts().targetTypes(GwtBuildTargetType.INSTANCE)).assertSuccessful();
  }

  private BuildResult makeGwtProject() {
    return doBuild(make().allModules().allArtifacts().targetTypes(GwtBuildTargetType.INSTANCE));
  }

  @Override
  protected void beforeBuildStarted(@NotNull ProjectDescriptor descriptor) {
    ((JpsGwtExtensionServiceImpl)JpsGwtExtensionService.getInstance()).clearCache();
  }
}
