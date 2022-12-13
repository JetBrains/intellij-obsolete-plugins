package com.intellij.frameworks.jboss.seam.highlighting;

import com.intellij.facet.FacetManager;
import com.intellij.facet.impl.FacetUtil;
import com.intellij.frameworks.jboss.seam.BasicSeamTestCase;
import com.intellij.frameworks.jboss.seam.JavaeeTestUtil;
import com.intellij.javaee.ejb.facet.EjbFacetType;
import com.intellij.javaee.facet.JavaeeFacet;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.seam.SeamApplicationComponent;
import com.intellij.seam.facet.SeamFacet;
import com.intellij.seam.facet.SeamFacetType;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder.MavenLib;
import com.intellij.testFramework.fixtures.*;
import com.intellij.util.descriptors.ConfigFileMetaData;

public abstract class SeamHighlightingTestCase <T extends JavaModuleFixtureBuilder> extends BasicSeamTestCase {

  protected CodeInsightTestFixture myFixture;
  protected ModuleFixture myModuleTestFixture;
  protected Project myProject;
  protected Module myModule;

  protected Class<T> getModuleFixtureBuilderClass() {
    return (Class<T>)JavaModuleFixtureBuilder.class;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder = JavaTestFixtureFactory.createFixtureBuilder(getName());

    final T moduleBuilder = projectBuilder.addModule(getModuleFixtureBuilderClass());

    myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.getFixture());

    myFixture.setTestDataPath(getTestDataPath());

    configureModule(moduleBuilder);

    myFixture.setUp();
    myFixture.enableInspections(SeamApplicationComponent.getInspectionClasses());

    myProject = myFixture.getProject();
    myModuleTestFixture = moduleBuilder.getFixture();
    myModule = myModuleTestFixture.getModule();

    createFacet();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      myFixture.tearDown();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      myFixture = null;
      myModuleTestFixture = null;
      myProject = null;
      myModule = null;
      super.tearDown();
    }
  }

  protected SeamFacet createFacet() {
    return WriteCommandAction.runWriteCommandAction(myProject, (Computable<SeamFacet>)() ->
       FacetManager.getInstance(myModule).addFacet(SeamFacetType.getInstance(), SeamFacetType.getInstance().getPresentableName(), null));
  }

  protected void configureModule(final T moduleBuilder) throws Exception {
    moduleBuilder.addContentRoot(myFixture.getTempDirPath());
    moduleBuilder.addSourceRoot("");
    moduleBuilder.setMockJdkLevel(JavaModuleFixtureBuilder.MockJdkLevel.jdk15);
  }

  protected void addSeamJar(JavaModuleFixtureBuilder<?> moduleBuilder) {
    moduleBuilder.addMavenLibrary(new MavenLib("org.jboss.seam:jboss-seam:2.0.0.GA"));
  }

  protected void addJavaeeSupport() {
    WriteCommandAction.runWriteCommandAction(myProject, () -> {
      FacetUtil.addFacet(myModule, EjbFacetType.getInstance());
    });

    ModuleRootModificationUtil.updateModel(myModule, model -> {
      MavenDependencyUtil.addFromMaven(model, "javax:javaee-api:8.0.1");
    });
  }

  protected void configureEjbDescriptor() {
    JavaeeFacet facet = JavaeeTestUtil.getJavaeeFacet(myModule);
    final ConfigFileMetaData metaData = JavaeeTestUtil.getMainMetaData(facet.getTypeId());

    facet.getDescriptorsContainer().getConfiguration().addConfigFile(metaData,
                                                                     VfsUtilCore.pathToUrl(getTestDataPath() + "META-INF/ejb-jar.xml"));
  }

  protected void allowTreeAccessForFile(String fileName, boolean copy) {
    if (copy) myFixture.copyFileToProject(fileName);

    VirtualFile file = getFile(myFixture.getTempDirPath() + "/" + fileName);
    assertNotNull(file);

    myFixture.allowTreeAccessForFile(file);
  }
}

