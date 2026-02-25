// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import junit.framework.TestCase;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.structure.GrailsApplicationProvider;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureSynchronizer;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Future;

@SuppressWarnings("GrMethodMayBeStatic")
public class GrailsProjectStructureTest extends UsefulTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    MvcModuleStructureSynchronizer.ourGrailsTestFlag = true;

    final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder =
      IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName());
    myFixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.getFixture());

    String appDir = myFixture.getTempDirPath() + "/testApp";
    new File(appDir).mkdirs();

    final JavaModuleFixtureBuilder<?> moduleFixtureBuilder = projectBuilder.addModule(JavaModuleFixtureBuilder.class);
    moduleFixtureBuilder.addSourceContentRoot(appDir);

    myFixture.setUp();
    myModule = moduleFixtureBuilder.getFixture().getModule();

    PsiTestUtil.addLibrary(myModule, "Grails", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-core-1.1.jar");
    GrailsApplicationProvider.APPLICATION_PROVIDER.getPoint()
      .registerExtension(new TestGrailsApplicationProvider(), myFixture.getTestRootDisposable());
  }

  @Override
  protected void tearDown() throws Exception {
    MvcModuleStructureSynchronizer.ourGrailsTestFlag = false;
    try {
      myFixture.tearDown();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      myFixture = null;
      myModule = null;
      super.tearDown();
    }
  }

  protected Project getProject() {
    return myFixture.getProject();
  }

  private static String defStructure(String prefix) {
    return "content:" + prefix + "\n" +
           "  source:" + prefix + "/grails-app/controllers\n" +
           "  source:" + prefix + "/grails-app/domain\n" +
           "  source:" + prefix + "/grails-app/services\n" +
           "  source:" + prefix + "/grails-app/taglib\n" +
           "  source:" + prefix + "/src/groovy\n" +
           "  source:" + prefix + "/src/java\n" +
           "  source:" + prefix + "/test/integration\n" +
           "  source:" + prefix + "/test/unit";
  }

  private void grailsApp(String prefix) throws IOException {
    GrailsTestUtil.createGrailsApplication(myFixture, prefix, false);
  }

  private void grailsPlugin(String prefix) throws IOException {
    grailsApp(prefix);
    GrailsTestUtil.createPluginXml(myFixture, prefix);
  }

  private static Module[] sort(Module[] modules) {
    Module[] res = modules.clone();
    Arrays.sort(res, moduleComparator);
    return res;
  }

  private void assertStructure(String s) {
    updateStructure();

    final VirtualFile rootFile = myFixture.getTempDirFixture().getFile("");
    StringBuilder dump = new StringBuilder("\n");
    ModuleManager moduleManager = ModuleManager.getInstance(getProject());

    for (Module module : sort(moduleManager.getModules())) {
      dump.append("module:").append(module.getName());
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      Map<Module, Boolean> dependencies = new TreeMap<>(moduleComparator);

      for (OrderEntry oe : moduleRootManager.getOrderEntries()) {
        if (oe instanceof ModuleOrderEntry) {
          dependencies.put(((ModuleOrderEntry)oe).getModule(), ((ModuleOrderEntry)oe).isExported());
        }
      }
      if (!dependencies.isEmpty()) {
        dump.append(" [");
        for (Map.Entry<Module, Boolean> dep : dependencies.entrySet()) {
          if (dump.charAt(dump.length() - 1) != '[') {
            dump.append(", ");
          }

          dump.append(dep.getKey().getName());
          if (dep.getValue()) {
            dump.append("*");
          }
        }
        dump.append("]");
      }


      dump.append("\n");
      Set<ContentEntry> contentEntries = new TreeSet<>(Comparator.comparing(it -> it.getFile().getPath()));
      contentEntries.addAll(List.of(moduleRootManager.getContentEntries()));
      for (ContentEntry entry : contentEntries) {
        dump.append(" content:").append(prettyPath(rootFile, entry.getFile())).append("\n");
        Set<SourceFolder> folders = new TreeSet<>(Comparator.comparing(it -> it.getFile().getPath()));
        folders.addAll(List.of(entry.getSourceFolders()));
        for (SourceFolder folder : folders) {
          dump.append("  source:").append(prettyPath(rootFile, folder.getFile())).append("\n");
        }
      }
    }
    TestCase.assertEquals(s, dump.toString().trim());
  }

  private void updateStructure() {
    Future<?> futureUpdate = GrailsApplicationManager.getInstance(getProject()).queueUpdate();
    PlatformTestUtil.waitForFuture((Future<?>)futureUpdate, 5 * 60 * 1000);
    MvcModuleStructureSynchronizer.forceUpdateProject(getProject());
  }

  private static String prettyPath(VirtualFile rootFile, final VirtualFile file) {
    final String s = VfsUtilCore.getRelativePath(file, rootFile, '/');
    if (StringUtil.isEmpty(s)) {
      return ".";
    }

    return s;
  }

  private String getModuleName() {
    return myModule.getName();
  }

  private void setProperties(String prefix, String text) {
    myFixture.saveText(myFixture.getTempDirFixture().getFile(prefix + "/application.properties"), text);
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
  }

  public void createModule(final String moduleName, final String path) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      Module newModule = MvcModuleStructureUtil.createAuxiliaryModule(myModule, moduleName, GrailsFramework.getInstance());
      PsiTestUtil.addContentRoot(newModule, myFixture.getTempDirFixture().getFile(path));
      MvcModuleStructureUtil.removeDependency(myModule, newModule);
    });
  }

  public void testCommonPlugins() throws IOException {
    grailsApp("testApp");

    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of("grails.project.work.dir", "../grailsWD"));

    setProperties("testApp", "plugins.zzz=1.0");
    grailsPlugin("grailsWD/plugins/zzz-1.0");

    assertStructure("module:" + getModuleName() + " [" + getModuleName() + "-grailsPlugins]\n " +
                    defStructure("testApp") + "\n" +
                    "module:" + getModuleName() + "-grailsPlugins\n " +
                    defStructure("grailsWD/plugins/zzz-1.0"));

    ApplicationManager.getApplication()
      .runWriteAction(() -> {
        try {
          myFixture.getTempDirFixture().getFile("grailsWD/plugins/zzz-1.0").delete(null);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

    setProperties("testApp", "");
    assertStructure("module:" + getModuleName() + "\n " +
                    defStructure("testApp"));
  }

  public void testCommonPluginsInCustomPlace() throws IOException {
    grailsApp("testApp");

    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of("grails.project.plugins.dir", "../myplugins"));

    setProperties("testApp", "plugins.zzz=1.2");
    grailsPlugin("myplugins/zzz-1.2");

    assertStructure("module:" + getModuleName() + " [" + getModuleName() + "-grailsPlugins]\n " +
                    defStructure("testApp") + "\n" +
                    "module:" + getModuleName() + "-grailsPlugins\n " +
                    defStructure("myplugins/zzz-1.2"));
  }

  public void testMarkingTestSourceRootAsTestSourceRoot() throws IOException {
    grailsApp("testApp");

    VirtualFile testFolder = myFixture.getTempDirFixture().findOrCreateDir("testApp/test/unit");
    ModuleRootManager rootManager = ModuleRootManager.getInstance(myFixture.getModule());

    PsiTestUtil.addSourceRoot(myFixture.getModule(), testFolder);

    updateStructure();

    assertContainsElements(List.of(rootManager.getSourceRoots(true)), testFolder);
    assertFalse(List.of(rootManager.getSourceRoots(false)).contains(testFolder));
  }

  public void testCustomGrailsWorkDir() throws Exception {
    grailsApp("testApp");

    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of("grails.work.dir", "../gwd"));

    setProperties("testApp", "plugins.zzz=4.2");
    grailsPlugin("gwd/projects/testApp/plugins/zzz-4.2");
    grailsPlugin("gwd/global-plugins/glob");

    assertStructure("module:" + getModuleName() + " [" + getModuleName() + "-grailsPlugins, GrailsGlobalPlugins]\n " +
                    defStructure("testApp") + "\n" +
                    "module:" + getModuleName() + "-grailsPlugins [GrailsGlobalPlugins]\n " +
                    defStructure("gwd/projects/testApp/plugins/zzz-4.2") + "\n" +
                    "module:GrailsGlobalPlugins\n " +
                    defStructure("gwd/global-plugins/glob"));
  }

  public void testCustomGlobalPluginsDir() throws Exception {
    grailsApp("testApp");

    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of("grails.global.plugins.dir", "../gpd"));

    grailsPlugin("gpd/glob1");
    grailsPlugin("gpd/glob2");

    assertStructure("module:" + getModuleName() + " [GrailsGlobalPlugins]\n " +
                    defStructure("testApp") + "\n" +
                    "module:GrailsGlobalPlugins\n " +
                    defStructure("gpd/glob1") + "\n " +
                    defStructure("gpd/glob2"));
  }

  public void testCustomPluginSomewhere() throws Exception {
    grailsApp("testApp");

    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of(), Map.of("paphos", "../paphos"));
    grailsApp("paphos");

    assertStructure("module:" + getModuleName() + " [paphos-inplacePlugin*]\n " +
                    defStructure("testApp") + "\n" +
                    "module:paphos-inplacePlugin\n " +
                    defStructure("paphos"));
  }

  public void testAllowConfAsSourceRoot() throws Exception {
    grailsApp("testApp");

    PsiTestUtil.addSourceRoot(myFixture.getModule(), myFixture.getTempDirFixture().getFile("testApp/grails-app/conf"));

    assertStructure("module:" + getModuleName() + "\n " +
                    defStructure("testApp").replaceFirst("\n", "\n  source:testApp/grails-app/conf\n"));
  }

  public void testCustomPluginDependencies() throws Exception {
    grailsApp("testApp");

    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of("grails.work.dir", "../gwd"), Map.of("cust", "../cust"));
    setProperties("testApp", "plugins.zzz=4.2");

    grailsPlugin("gwd/projects/testApp/plugins/zzz-4.2");
    grailsApp("cust");

    setProperties("cust", "plugins.zzz=4.2");

    assertStructure("module:" + getModuleName() + " [" + getModuleName() + "-grailsPlugins, cust-inplacePlugin*]\n " +
                    defStructure("testApp") + "\n" +
                    "module:" + getModuleName() + "-grailsPlugins\n " +
                    defStructure("gwd/projects/testApp/plugins/zzz-4.2") + "\n" +
                    "module:cust-inplacePlugin [" + getModuleName() + "-grailsPlugins]\n " +
                    defStructure("cust"));
  }

  public void testCustomPluginCustomModuleDependencies() throws Exception {
    grailsApp("testApp");

    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of("grails.work.dir", "../gwd"), Map.of("cust", "../cust"));

    grailsPlugin("gwd/projects/testApp/plugins/zzz-4.2");
    grailsApp("cust");

    createModule("Cust", "cust");

    setProperties("cust", "plugins.zzz=4.2 ");

    assertStructure("module:" + getModuleName() + " [" + getModuleName() + "-grailsPlugins, Cust*]\n " +
                    defStructure("testApp") + "\n" +
                    "module:" + getModuleName() + "-grailsPlugins\n " +
                    defStructure("gwd/projects/testApp/plugins/zzz-4.2") + "\n" +
                    "module:Cust [" + getModuleName() + "-grailsPlugins]\n " +
                    defStructure("cust"));
  }

  public void testRemoveCustomPlugin() throws IOException {
    grailsApp("testApp");

    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of("grails.project.work.dir", "../grailsWD"),
                                     Map.of("myCustomPlugin1", "../customPlugins/myCustomPlugin1",
                                            "myCustomPlugin2", "../customPlugins/my-custom-plugin-2"));

    grailsApp("customPlugins/myCustomPlugin1");
    grailsApp("customPlugins/my-custom-plugin-2");
    grailsPlugin("grailsWD/projects/testApp/plugins/zzz-4.2");

    myFixture.addFileToProject("customPlugins/myCustomPlugin1/MyCustomPlugin1GrailsPlugin.groovy", "");

    setProperties("testApp", "plugins.myCustomPlugin1=1.0\nplugins.myCustomPlugin2=1.0\nplugins.zzz=4.2");

    assertStructure("module:" + getModuleName() + " [myCustomPlugin1-inplacePlugin*, myCustomPlugin2-inplacePlugin*]\n " +
                    defStructure("testApp") + "\n" +
                    "module:myCustomPlugin1-inplacePlugin\n " +
                    defStructure("customPlugins/myCustomPlugin1") + "\n" +
                    "module:myCustomPlugin2-inplacePlugin\n " +
                    defStructure("customPlugins/my-custom-plugin-2"));

    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of("grails.project.work.dir", "../grailsWD"));

    setProperties("testApp", "plugins.zzz=4.2");

    assertStructure("module:" + getModuleName() + " [myCustomPlugin2-inplacePlugin*]\n " +
                    defStructure("testApp") + "\n" +
                    "module:myCustomPlugin1-inplacePlugin\n " +
                    defStructure("customPlugins/myCustomPlugin1") + "\n" +
                    "module:myCustomPlugin2-inplacePlugin\n " +
                    defStructure("customPlugins/my-custom-plugin-2"));
  }

  public void testRemovingCommonPluginModuleAfterRemovingMainModule() throws IOException {
    grailsApp("testApp");
    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of("grails.project.work.dir", "../grailsWD"),
                                     Map.of("myCustomPlugin1", "../customPlugins/myCustomPlugin1"));
    setProperties("testApp", "plugins.myCustomPlugin1=1.0\nplugins.zzz=1.0");

    grailsApp("customPlugins/myCustomPlugin1");
    grailsPlugin("grailsWD/plugins/zzz-1.0");

    assertStructure("module:" + getModuleName() + " [" + getModuleName() + "-grailsPlugins, myCustomPlugin1-inplacePlugin*]\n " +
                    defStructure("testApp") + "\n" +
                    "module:" + getModuleName() + "-grailsPlugins\n " +
                    defStructure("grailsWD/plugins/zzz-1.0") + "\n" +
                    "module:myCustomPlugin1-inplacePlugin [" + getModuleName() + "-grailsPlugins]\n " +
                    defStructure("customPlugins/myCustomPlugin1"));
    ApplicationManager.getApplication().runWriteAction(() -> MvcModuleStructureUtil.removeAuxiliaryModule(myModule));
    assertStructure("module:myCustomPlugin1-inplacePlugin\n " +
                    defStructure("customPlugins/myCustomPlugin1"));
  }

  public void testMultiTest() throws IOException {
    grailsApp("testApp");
    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of("grails.project.work.dir", "../grailsWD"),
                                     Map.of("plugin1", "../customPlugins/plugin1",
                                            "plugin2", "../customPlugins/plugin2"));

    grailsPlugin("grailsWD/plugins/zzz-1.0");

    grailsApp("customPlugins/plugin1");
    myFixture.addFileToProject("customPlugins/plugin1/Plugin1GrailsPlugin.groovy", "");
    grailsApp("customPlugins/plugin2");
    myFixture.addFileToProject("customPlugins/plugin2/Plugin2GrailsPlugin.groovy", "");

    setProperties("testApp", "plugins.plugin1=1.0\nplugins.plugin2=1.0\nplugins.zzz=1.0");

    assertStructure(
      "module:" + getModuleName() + " [" + getModuleName() + "-grailsPlugins, plugin1-inplacePlugin*, plugin2-inplacePlugin*]\n " +
      defStructure("testApp") + "\n" +
      "module:" + getModuleName() + "-grailsPlugins\n " +
      defStructure("grailsWD/plugins/zzz-1.0") + "\n" +
      "module:plugin1-inplacePlugin [" + getModuleName() + "-grailsPlugins]\n " +
      defStructure("customPlugins/plugin1") + "\n" +
      "module:plugin2-inplacePlugin [" + getModuleName() + "-grailsPlugins]\n " +
      defStructure("customPlugins/plugin2"));

    GrailsTestUtil.createBuildConfig(myFixture, "testApp", Map.of("grails.project.work.dir", "../grailsWD"),
                                     Map.of("plugin1", "../customPlugins/plugin1"));
    GrailsTestUtil.createBuildConfig(myFixture, "customPlugins/plugin1", Map.of(),
                                     Map.of("plugin2", "../plugin2"));

    setProperties("testApp", "plugins.plugin1=1.0\nplugins.plugin2=1.0\nplugins.zzz=1.0 ");

    assertStructure("module:" + getModuleName() + " [" + getModuleName() + "-grailsPlugins, plugin1-inplacePlugin*]\n " +
                    defStructure("testApp") + "\n" +
                    "module:" + getModuleName() + "-grailsPlugins\n " +
                    defStructure("grailsWD/plugins/zzz-1.0") + "\n" +
                    "module:plugin1-inplacePlugin [" + getModuleName() + "-grailsPlugins, plugin2-inplacePlugin*]\n " +
                    defStructure("customPlugins/plugin1") + "\n" +
                    "module:plugin2-inplacePlugin [" + getModuleName() + "-grailsPlugins]\n " +
                    defStructure("customPlugins/plugin2"));

    grailsApp("root2");
    GrailsTestUtil.createBuildConfig(myFixture, "root2", Map.of("grails.project.work.dir", "../grailsWD2"),
                                     Map.of("plugin1", "../customPlugins/plugin1"));
    createModule("Root2", "root2");

    grailsPlugin("grailsWD2/plugins/xxx-1.0");

    setProperties("testApp", " ");

    assertStructure("module:" + getModuleName() + " [" + getModuleName() + "-grailsPlugins, plugin1-inplacePlugin*]\n " +
                    defStructure("testApp") + "\n" +
                    "module:" + getModuleName() + "-grailsPlugins\n " +
                    defStructure("grailsWD/plugins/zzz-1.0") + "\n" +
                    "module:Root2 [Root2-grailsPlugins, plugin1-inplacePlugin*]\n " +
                    defStructure("root2") + "\n" +
                    "module:Root2-grailsPlugins\n " +
                    defStructure("grailsWD2/plugins/xxx-1.0") + "\n" +
                    "module:plugin1-inplacePlugin [" + getModuleName() + "-grailsPlugins, Root2-grailsPlugins, plugin2-inplacePlugin*]\n " +
                    defStructure("customPlugins/plugin1") + "\n" +
                    "module:plugin2-inplacePlugin [" + getModuleName() + "-grailsPlugins, Root2-grailsPlugins]\n " +
                    defStructure("customPlugins/plugin2"));
  }

  private JavaCodeInsightTestFixture myFixture;
  private Module myModule;
  private static final Comparator<Module> moduleComparator = Comparator.comparing(Module::getName);
}
