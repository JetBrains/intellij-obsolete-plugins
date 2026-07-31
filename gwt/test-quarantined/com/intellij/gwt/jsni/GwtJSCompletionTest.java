/*
 * Copyright (c) 2000-2007 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.gwt.jsni;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.javascript.GwtJsTestUtil;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.lang.javascript.BaseJSCompletionTestCase;
import com.intellij.lang.javascript.JSCompletionTestHelper;
import com.intellij.lang.javascript.JSCompletionTestHelperSimple;
import com.intellij.lang.javascript.JSTestOption;
import com.intellij.lang.javascript.JSTestOptions;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.Nullable;

import static com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase.JAVA_1_7;

public class GwtJSCompletionTest extends BaseJSCompletionTestCase {
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return JAVA_1_7;
  }

  @Override
  protected String getExtension() {
    return GwtJsTestUtil.GWT_FILE_EXTENSION;
  }
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    GwtJsTestUtil.setUpGwtDialect();
  }

  @Override
  protected JSCompletionTestHelper helper() {
    return new JSCompletionTestHelperSimple(
      getTestName(false),
      myFixture,
      getExtension(),
      useAssertOnRecursionPreventionByDefault(),
      getClass(),
      getTestDataPath(),
      mySmartCompletionTests
    ) {
      @Override
      public LookupElement @Nullable [] complete() {
        getFixture().complete(CompletionType.BASIC, getTestName().contains("Second") ? 2 : 1);
        if (getTestName().contains("Tab")) {
          getFixture().getLookup().setCurrentItem(getFixture().getLookupElements()[0]);
          getFixture().type('\t');
        }

        return getFixture().getLookupElements();
      }
    };
  }

  public void testGwtClassInPackage() {
    doTest();
  }

  @JSTestOptions(value = JSTestOption.ClassNameCompletion)
  public void testClassName() {
    GwtTestCase.addGwtFacet(getModule(), GwtVersionImpl.VERSION_2_5);
    doTest();
  }

  public void testSecondClassName() {
    GwtTestCase.addGwtFacet(getModule(), GwtVersionImpl.VERSION_2_5);
    doTest();
  }

  @JSTestOptions(value = JSTestOption.ClassNameCompletion)
  public void testClassNameBeforeMethod() {
    GwtTestCase.addGwtFacet(getModule(), GwtVersionImpl.VERSION_2_5);
    doTest();
  }

  public void testGwtField() {
    doTest();
  }

  public void testGwtMethod() {
    GwtTestCase.addGwtFacet(getModule(), GwtVersionImpl.VERSION_2_0);
    doTest();
  }

  public void testGwtMethodWildcard() {
    GwtTestCase.addGwtFacet(getModule(), GwtVersionImpl.VERSION_2_6);
    doTest();
  }

  public void testGwtMethodWithGenerics() {
    GwtTestCase.addGwtFacet(getModule(), GwtVersionImpl.VERSION_2_0);
    doTest();
  }

  public void testGwtNewExpression() {
    doTest();
  }

  public void testGwtMethodTab() {
    GwtTestCase.addGwtFacet(getModule(), GwtVersionImpl.VERSION_2_0);
    doTest();
  }

  public void testGwtMethodTab2() {
    GwtTestCase.addGwtFacet(getModule(), GwtVersionImpl.VERSION_2_0);
    doTest();
  }

  public void testDoNotCompleteWait() {
    GwtTestCase.addGwtFacet(getModule(), null);
    doTest();
  }

  private void doTest() {
    doTest("");
  }

  @Override
  protected String getBasePath() {
    return "/jsni/completion/js/";
  }

  @Override
  protected String getTestDataPath() {
    return GwtTestCase.getGwtTestDataPath() + getBasePath();
  }
}
