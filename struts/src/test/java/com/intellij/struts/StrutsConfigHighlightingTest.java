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

import com.intellij.lang.javascript.dialects.JSLanguageLevel;
import com.intellij.lang.javascript.settings.JSRootConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.builders.WebModuleFixtureBuilder;
import com.intellij.util.ThrowableRunnable;

/**
 * @author Dmitry Avdeev
 */
public class StrutsConfigHighlightingTest extends StrutsTest {

  public void testStrutsConfig() {
    myFixture.testHighlighting("/WEB-INF/struts-config.xml");
  }

  public void testStrutsConfigWildcards() {
    myFixture.testHighlighting("/WEB-INF/struts-config-wildcards.xml");
  }

  public void testTilesConfig() {
    myFixture.testHighlighting("/WEB-INF/tiles-defs.xml");
  }

  public void testValidation() {
    myFixture.testHighlighting("/WEB-INF/validation.xml");
    myFixture.testHighlighting("/WEB-INF/validation13.xml");
    myFixture.testHighlighting("/WEB-INF/validator-rules.xml");
  }

  public void testStrutsPage() {
     myFixture.testHighlighting("/index.jsp");
  }

  public void testTilesPage() {
    myFixture.testHighlighting("/tilesPage.jsp");
//    myFixture.testHighlighting("/resources/pages/tilesInsertTest.jsp");
  }

  public void testFileReferencePerformance() {
    myFixture.testHighlighting("/fileReferencePerformance.jsp");
  }

  public void testHtmlTags() {
    myFixture.testHighlighting("/html_taglib.jsp");
  }

  public void testJavascriptInjection() throws Throwable {
    testWithinLanguageLevel(JSLanguageLevel.JS_1_6, myFixture.getProject(),
                                        (ThrowableRunnable<Exception>)() -> myFixture.testHighlighting(true, false, true, "/testJSinjected.jsp"));
  }

  public void testWeirdAttribute() {
    myFixture.testHighlighting("/WEB-INF/weird-attribute.xml");
  }

  @Override
  protected void configure(WebModuleFixtureBuilder moduleBuilder) {
    super.configure(moduleBuilder);
    moduleBuilder.addSourceRoot("src");
  }

  @Override
  protected String[] getLibraries() {
    return new String[] { "struts-1.2.9.jar", "struts-el-1.2.9.jar", "sslext-1.2-0.jar", "commons-dbcp.jar", "commons-beanutils.jar"};
  }

  private static <E extends Exception> void testWithinLanguageLevel(final JSLanguageLevel level, Project project, ThrowableRunnable<E> test)
      throws E {
    if (level == JSRootConfiguration.getInstance(project).getLanguageLevel()) {
      test.run();
      return;
    }
    JSRootConfiguration configuration = JSRootConfiguration.getInstance(project);
    JSLanguageLevel previousLevel = configuration.getLanguageLevel();
    try {
      configuration.storeLanguageLevelAndUpdateCaches(level);
      test.run();
    }
    finally {
      configuration.storeLanguageLevelAndUpdateCaches(previousLevel == JSLanguageLevel.DEFAULT ? null : previousLevel);
    }
  }

}
