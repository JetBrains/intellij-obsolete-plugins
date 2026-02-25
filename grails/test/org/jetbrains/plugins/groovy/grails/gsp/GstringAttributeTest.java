// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GstringAttributeTest extends LightJavaCodeInsightFixtureTestCase {
  public void testHighlight() {
    myFixture.configureByText("a.gsp", """
      <g:link action="\\$" />
      <g:link action="$<error descr="Identifier expected">"</error> />
      <g:link action='\\$' />
      <g:link action='$<error descr="Identifier expected">'</error> />
      <g:link action='abc\\$1' />
      <g:link action='abc$<error descr="Identifier expected">1</error>' />
      <g:link action="abc$<error descr="Identifier expected">1</error>" />
      """);

    myFixture.testHighlighting("a.gsp");
  }

  public void testCompletion() {
    myFixture.addFileToProject("a.gsp", """
          <% def xxx1 = 1, xxx2 = 2 %>
          <g:each in="$xx<caret>" />
      """);
    myFixture.testCompletionVariants("a.gsp", "xxx1", "xxx2");

    myFixture.addFileToProject("b.gsp", """
          <% def xxx1 = 1, xxx2 = 2 %>
          <g:each in="abc$xx<caret>" />
      """);
    myFixture.testCompletionVariants("b.gsp", "xxx1", "xxx2");
  }

  public void testRename() {
    myFixture.configureByText("a.gsp", """      
          <% def xxx<caret> = 1 %>
          <g:each in="$xxx" />
          <g:link action="abc$xxx;dasdasdasdasd" />
          <g:link action="$xxx dasdasd" />
      """);

    myFixture.renameElementAtCaret("abc123");// new name is more then old name

    myFixture.checkResult("""
                                <% def abc123 = 1 %>
                                <g:each in="$abc123" />
                                <g:link action="abc$abc123;dasdasdasdasd" />
                                <g:link action="$abc123 dasdasd" />
                            """);

    myFixture.renameElementAtCaret("ttt");// new name is less then old name

    myFixture.checkResult("""
                                <% def ttt = 1 %>
                                <g:each in="$ttt" />
                                <g:link action="abc$ttt;dasdasdasdasd" />
                                <g:link action="$ttt dasdasd" />
                            """);
  }

  public void testEach() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <% def xxx = [1,2,3] %>
      <g:each in="$xxx">
        ${it.substring(1)}
        ${it.getInteger()}
      </g:each>
      """);

    GrailsTestCase.checkResolve(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "getInteger");
  }
}
