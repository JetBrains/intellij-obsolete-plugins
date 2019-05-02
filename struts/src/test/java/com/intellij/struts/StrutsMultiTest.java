/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

import com.intellij.testFramework.builders.WebModuleFixtureBuilder;
import org.jetbrains.annotations.NonNls;

/**
 * @author Dmitry Avdeev
 */
public class StrutsMultiTest  extends StrutsTest {

  public void testWebXml() {
    myFixture.testHighlighting("/WEB-INF/web.xml");
  }

  public void testStrutsConfig() {
    myFixture.testHighlighting("/WEB-INF/struts-config.xml");
  }

  public void testSecondConfig() {
    myFixture.testHighlighting("/WEB-INF/struts-config-second.xml");
  }

  public void testStrutsPage() {
    myFixture.testHighlighting("/index.jsp");
  }

  public void testSecondPage() {
    myFixture.testHighlighting("/second/index.jsp");
  }

  @Override
  protected void configure(WebModuleFixtureBuilder moduleBuilder) {
    super.configure(moduleBuilder);
    moduleBuilder.addSourceRoot("src");
  }

  @Override
  @NonNls
  public String getBasePath() {
    return "/multiModules/";
  }
}
