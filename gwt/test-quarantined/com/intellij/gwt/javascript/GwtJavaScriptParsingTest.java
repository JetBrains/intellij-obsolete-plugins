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

package com.intellij.gwt.javascript;

import com.intellij.gwt.jsinject.parser.GwtParserDefinition;
import com.intellij.javascript.testFramework.MockInjectedLanguageManager;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.javascript.JavascriptParserDefinition;
import com.intellij.openapi.application.PathManager;
import com.intellij.testFramework.ParsingTestCase;

import static com.intellij.lang.javascript.JSElementTypeServiceHelper.registerJSElementTypeServices;

public class GwtJavaScriptParsingTest extends ParsingTestCase {
  public GwtJavaScriptParsingTest() {
    super("", GwtJsTestUtil.GWT_FILE_EXTENSION, new GwtParserDefinition(), new JavascriptParserDefinition());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    registerJSElementTypeServices(getApplication(), getTestRootDisposable());
    project.registerService(InjectedLanguageManager.class, new MockInjectedLanguageManager());
  }

  @Override
  protected String getTestDataPath() {
    return PathManager.getHomePath() + "/plugins/JavaScriptLanguage/tests/unitTests/testData/psi";
  }

  public void testGwtDialect() {
    doTest(true);
  }

  public void testGwtAtSignFinishedByParenthesis() {
    doTest(true);
  }

  public void testGwtAtSignFinishedByBracket() {
    doTest(true);
  }
}
