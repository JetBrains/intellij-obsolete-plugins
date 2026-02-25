// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.junit.Assert;

public class GspLanguageInjectionTest extends LightJavaCodeInsightFixtureTestCase {
  public void testInjectGroovyUrl1() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:link url="[controller: re<caret>]" />""");
    Assert.assertTrue(myFixture.completeBasic().length > 0);
    myFixture.testHighlighting(true, false, true);
  }

  public void testInjectGroovyUrl2() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:link url=" [controller: re<caret>] " />""");
    Assert.assertEquals(0, myFixture.completeBasic().length);
    myFixture.testHighlighting(true, false, true);
  }

  public void testInjectGroovyUrl3() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:link url="http://jetbrains.<caret>com" />""");
    Assert.assertEquals(0, myFixture.completeBasic().length);
    myFixture.checkHighlighting(true, false, true);
  }

  public void testInjectGroovyExpr1() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:findAll expr="123 == re<caret>" />""");
    Assert.assertTrue(myFixture.completeBasic().length > 0);
    myFixture.testHighlighting(true, false, true);
  }

  public void testInjectGroovyExpr2() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:findAll expr='${re<caret>}' />""");
    Assert.assertTrue(myFixture.completeBasic().length > 0);
    myFixture.testHighlighting(true, false, true);
  }

  public void testInjectGroovyExpr3() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:findAll expr="<caret>" />""");
    Assert.assertTrue(myFixture.completeBasic().length > 0);
    myFixture.testHighlighting(true, false, true);
  }

  public void testInjectJavascript1() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:remoteLink onComplete="<caret>" />""");
    Assert.assertTrue(myFixture.completeBasic().length > 0);
  }

  public void testInjectJavascript2() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:remoteLink before="<caret>" />""");
    Assert.assertTrue(myFixture.completeBasic().length > 0);
  }

  public void testInjectJavascript3() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:javascript>
        a<caret>
      </g:javascript>
      """);
    Assert.assertTrue(myFixture.completeBasic().length > 0);
  }

  public void testInjectJavascript4() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <r:script>
        a<caret>
      </r:script>
      """);
    Assert.assertTrue(myFixture.completeBasic().length > 0);
  }
}
