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

package com.intellij.struts;

import com.intellij.codeInsight.daemon.impl.analysis.XmlPathReferenceInspection;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.struts.highlighting.StrutsInspection;
import com.intellij.struts.highlighting.TilesInspection;
import com.intellij.struts.highlighting.ValidatorInspection;
import com.intellij.struts.inplace.inspections.ValidatorFormInspection;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.builders.WebModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Dmitry Avdeev
 */
public abstract class StrutsTest extends UsefulTestCase {

  protected CodeInsightTestFixture myFixture;
  protected WebModuleTestFixture myWebModuleTestFixture;

  private Module myModule;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder = JavaTestFixtureFactory.createFixtureBuilder(getName());

    final WebModuleFixtureBuilder moduleBuilder = projectBuilder.addModule(WebModuleFixtureBuilder.class);
    moduleBuilder.addJdk(System.getProperty("java.home"));


    myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.getFixture());

    final String root = getTestDataPath();
    myFixture.setTestDataPath(root);

    final String tempDirPath = myFixture.getTempDirPath();
    moduleBuilder.addContentRoot(tempDirPath);
    moduleBuilder.addWebRoot(tempDirPath, "/");

    configure(moduleBuilder);

    myFixture.setUp();
    myFixture.enableInspections(new ValidatorFormInspection(),
                                new StrutsInspection(),
                                new TilesInspection(),
                                new ValidatorInspection(),
                                new XmlPathReferenceInspection());

    myWebModuleTestFixture = moduleBuilder.getFixture();
    myModule = myWebModuleTestFixture.getModule();
  }


  @Override
  protected void tearDown() throws Exception {
    myWebModuleTestFixture = null;
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

  @NonNls
  protected String getBasePath() {
    return "/struts/";
  }

  protected void configure(WebModuleFixtureBuilder moduleBuilder) {
    final String root = getTestDataPath();
    moduleBuilder.addContentRoot(root);
    moduleBuilder.addWebRoot(root, "/");
    moduleBuilder.setWebXml(root + "/WEB-INF/web.xml");
    addStrutsJar(moduleBuilder);
  }

  protected void addStrutsJar(final WebModuleFixtureBuilder moduleBuilder) {
    String libDir = new File(getTestDataRoot(), "lib").getPath() + "/";
    moduleBuilder.addLibraryJars("struts", libDir, getLibraries());
  }

  protected String[] getLibraries() {
    return new String[]{"struts-1.2.9.jar", "commons-beanutils.jar"};
  }

  @NonNls
  protected String getTestDataPath() {
    return getTestDataRoot().getPath() + getBasePath();
  }

  @NotNull
  public static File getTestDataRoot() {
    return new File(PathManager.getResourceRoot(StrutsTest.class, "/root.txt"));
  }

  protected Module getModule() {
    return myModule;
  }
}
