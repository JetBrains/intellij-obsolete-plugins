// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.junit.Assert;

public class GspResolveIndexesTest extends LightJavaCodeInsightFixtureTestCase {
  private PsiElement getElementAtCaret() {
    int caret = myFixture.getEditor().getCaretModel().getOffset();
    return myFixture.getFile().getViewProvider().findElementAt(caret, GroovyLanguage.INSTANCE);
  }

  private void assertType(String type) {
    PsiElement element = getElementAtCaret();

    PsiReference ref = element.getReference();
    while (ref == null) {
      element = element.getParent();
      ref = element.getReference();
    }


    PsiVariable var = (PsiVariable)ref.resolve();
    Assert.assertEquals(type, var.getType().getCanonicalText());
  }

  public void testEach() {
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:each in='["aaa", "bbb", "ccc"]'>
        ${i<caret>t}
      </g:each>
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("java.lang.String");
    UsefulTestCase.assertInstanceOf(myFixture.getElementAtCaret().getNavigationElement(), XmlAttributeValue.class);
  }

  public void testEachStatus() {
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:each in='["aaa", "bbb", "ccc"]' status='iii'>
        ${i<caret>ii}
      </g:each>
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("java.lang.Integer");
    UsefulTestCase.assertInstanceOf(myFixture.getElementAtCaret().getNavigationElement(), XmlAttributeValue.class);
  }

  public void testHighlighting() {
    myFixture.addFileToProject("grails-app/views/test.gsp", """
      ${<warning descr="Cannot resolve symbol 'v2'">v2</warning>} ${<warning descr="Cannot resolve symbol 'v1'">v1</warning>}
      <g:set var="v1" value="123"/>
      ${<warning descr="Cannot resolve symbol 'v2'">v2</warning>} ${v1}
      <g:set var="v2" value="123"/>
      ${<warning descr="Cannot resolve symbol 'zzz'">zzz</warning>} ${v2} ${v1}
      
      <g:each in="[1,2,3]">
        <g:each in="['a', 'b', 'c']" var="zzz">
          ${it} ${zzz}
        </g:each>${<warning descr="Cannot resolve symbol 'zzz'">zzz</warning>}
      </g:each>
      
      <g:each in="[1,2,3]">
        <g:each in="['a', 'b', 'c']">
          ${it}
        </g:each>
      </g:each>
      """);
    myFixture.enableInspections(new GrUnresolvedAccessInspection());
    myFixture.testHighlighting("grails-app/views/test.gsp");
  }

  public void testEachVar() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each in='["aaa", "bbb", "ccc"]' var="iii">
        ${i<caret>ii}
      </g:each>
      """);
    assertType("java.lang.String");
    UsefulTestCase.assertInstanceOf(myFixture.getElementAtCaret().getNavigationElement(), XmlAttributeValue.class);
  }

  public void testEach2() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <% def aList = [new Long(2), new Long(342)] %>
      <g:each in='${aList}'>
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.lang.Long");
  }

  public void testEach3() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <% def aList = new ArrayList<Map>() %>
      <g:each in='${aList}'>
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.util.Map");
  }

  public void testEach4() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each in="dffsdfsdfdsf">
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.lang.String");
  }

  public void testEach5() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each in=" ${[3, 4, 5]}">
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.lang.String");
  }

  public void testEach6() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each in='${[3, 4, 5]} '>
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.lang.String");
  }

  public void testEach7() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each in='${new Integer(34)}'>
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.lang.Integer");
  }

  public void testRenameVariableEachVar() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each var="v" in="[1,2,3]">
        <g:if test="${v > 0}">
          <%= v<caret>.byteValue() %>
          <% out << v + 10 %>
          ${v}
        </g:if> v
      </g:each>
      ${v}
      """);
    myFixture.renameElementAtCaret("vvv");
    Assert.assertEquals(
      """
        <g:each var="vvv" in="[1,2,3]">
          <g:if test="${vvv > 0}">
            <%= vvv.byteValue() %>
            <% out << vvv + 10 %>
            ${vvv}
          </g:if> v
        </g:each>
        ${v}
        """, file.getText());

    PsiReference ref = file.findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());
    Assert.assertNotNull(ref.resolve());
  }

  public void testRenameVariableEachStatus() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each var="i" status="v" in="[1,2,3]">
        <g:if test="${v > 0}">
          <%= v<caret>.byteValue() %>
          <% out << v + 10 %>
          ${v}
        </g:if> v
      </g:each>
      ${v}
      """);
    myFixture.renameElementAtCaret("vvv");
    Assert.assertEquals(
      """
        <g:each var="i" status="vvv" in="[1,2,3]">
          <g:if test="${vvv > 0}">
            <%= vvv.byteValue() %>
            <% out << vvv + 10 %>
            ${vvv}
          </g:if> v
        </g:each>
        ${v}
        """, file.getText());

    PsiReference ref = file.findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());
    Assert.assertNotNull(ref.resolve());
  }

  public void testRenameVariableDef() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      ${v}
      <g:set var="v" value="v"/>
      <g:if test="${v<caret> > 0}">
        <%=v.byteValue()%>
        <% out << v + 10 %>
        ${v}
      </g:if>
      """);
    myFixture.renameElementAtCaret("vvv");
    Assert.assertEquals("""
                          ${v}
                          <g:set var="vvv" value="v"/>
                          <g:if test="${vvv > 0}">
                            <%=vvv.byteValue()%>
                            <% out << vvv + 10 %>
                            ${vvv}
                          </g:if>
                          """, file.getText());

    PsiReference ref = file.findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());
    Assert.assertNotNull(ref.resolve());
  }

  public void _testResolveIndexes() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <% def it = new Object(); %>
      <g:each in="['a', 'b', 'c']">
      ${it.substring(1)}
      <g:findAll in="[1, 2,3,4,5]" expr="it.byteValue() == 0" />
      </g:each>
      """);
    GrailsTestCase.checkResolve(file);
  }

  public void testResolveItOfUnknownType() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <g:collect in="${unresolved}" expr="it.hashCode() != 0">${it}</g:collect>
      <g:findAll in="${unresolved}" expr="it.hashCode() != 0">${it}</g:findAll>
      <g:each in="${unresolved}">${it}</g:each>
      <g:each in="${unresolved}" var="iii">${iii}</g:each>
      <g:grep in="${unresolved}" filter="sdsdfs">${it}</g:grep>
      """);
    GrailsTestCase.checkResolve(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "unresolved", "unresolved", "unresolved",
                                "unresolved", "unresolved");
  }

  public void testNullSafeEach() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <% def ccc = [1, 2, 3] %>
      
      <g:collect in="${ccc?}" expr="it.byteValue() != 1">${it}</g:collect> <br>
      <g:findAll in="${ccc?}" expr="it.byteValue() != 1">${it}</g:findAll> <br>
      <g:each in="${ccc?}">${it.byteValue()}</g:each> <br>
      <g:grep in="${ccc?}" filter="${/[a-z]/}">${it.byteValue()}</g:grep> <br>
      
      <g:link in="${ccc?<error descr="<expression> expected">}</error>" />
      <g:each zzz="${ccc?<error descr="<expression> expected">}</error>" in="[1,2,3]"/>
      
      ${ccc?<error descr="<expression> expected">}</error>
      
      <% ccc?<error descr="<expression> expected, got '%>'"> </error>%>
      <% unresolver %>
      """);

    myFixture.testHighlighting(true, false, true, file.getVirtualFile());

    GrailsTestCase.checkResolve(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "unresolver");
  }

  public void testCompletion() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <g:each in='${[3, 4, 5]}' var="g">
        ${g.<caret>}
      </g:each>
      """);
    GrailsTestCase.checkCompletionStatic(myFixture, file, "byteValue", "intValue", "longValue");
  }
}
