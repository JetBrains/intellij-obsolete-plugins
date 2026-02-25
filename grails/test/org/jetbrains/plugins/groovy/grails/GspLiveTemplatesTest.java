// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.grails;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.codeInsight.template.impl.actions.ListTemplatesAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class GspLiveTemplatesTest extends LightJavaCodeInsightFixtureTestCase {

  public void testHtmlTemplatesWorkInGsp() {
    myFixture.configureByText("a.gsp", "c<caret>");
    expandTemplate(myFixture.getEditor());
    myFixture.checkResult("<!-- <caret> -->");
  }

  private static void expandTemplate(final Editor editor) {
    new ListTemplatesAction().actionPerformedImpl(editor.getProject(), editor);
    ((LookupImpl) LookupManager.getActiveLookup(editor)).finishLookup(Lookup.NORMAL_SELECT_CHAR);
  }
}