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

/**
 * @author Dmitry Avdeev
 */
public class StrutsConfigCompletionTest extends StrutsTest {

  public void testStrutsConfig() {
    myFixture.testCompletion("/completion/action-name.xml", "/completion/action-name_after.xml");
  }

  public void testActionUrl() {
    myFixture.testCompletion("/completion/action-url.xml", "/completion/action-url_after.xml");
  }

  public void testTilesUrl() {
    myFixture.testCompletion("/completion/tiles-url.xml", "/completion/tiles-url_after.xml");
  }

  public void testActionRename() {
    myFixture.testRename("/rename/action-url.xml", "/rename/action-url_after.xml", "/action_new");
  }
}
