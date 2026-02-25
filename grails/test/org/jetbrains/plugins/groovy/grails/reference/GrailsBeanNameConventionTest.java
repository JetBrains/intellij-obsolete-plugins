// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsBeanNameConventionTest extends GrailsTestCase {
  public void doTestActions(String oldName, String oldPropertyName, String newName, String newPropertyName) {
    PsiFile www = addController("class WwwController {def " + oldName + "<caret> = {} }");
    PsiFile tagLib = addTaglib("""
                                 class MyTagLib {
                                   def customTag = { out << link(action: \"""" + oldPropertyName + """
                                 ", controller: 'www') }
                                 }
                                 """);
    PsiFile someLib = addTaglib("""
                                  class SomeTagLib {
                                    def customTag = { out << link(action: \"""" + oldName + """
                                  ", controller: 'www') }
                                  }
                                  """);
    PsiFile gsp1 = myFixture.addFileToProject("grails-app/views/www/g1.gsp", "<g:link url=\"[action: '" + oldPropertyName + "']\" />");
    PsiFile gsp2 = myFixture.addFileToProject("grails-app/views/www/g2.gsp", "<g:link url='[action: \"" + oldPropertyName + "\"]' />");
    PsiFile gsp3 = myFixture.addFileToProject("grails-app/views/www/g3.gsp", "<% g.link(action: '" + oldPropertyName + "') %>");
    PsiFile gsp4 = myFixture.addFileToProject("grails-app/views/www/g4.gsp", "<% g.link(action: \"" + oldPropertyName + "\") %>");
    PsiFile gsp5 = myFixture.addFileToProject("grails-app/views/www/g5.gsp",
                                              "<% g.link(action: \"" + oldName + "\") g.link(action: \"" + oldPropertyName + "\") %>");
    PsiFile gsp6 = myFixture.addFileToProject("grails-app/views/www/g6.gsp", "<g:link action='" + oldPropertyName + "' />");
    PsiFile gsp7 = myFixture.addFileToProject("grails-app/views/www/g7.gsp", "<g:link action='" + oldName + "' />");

    PsiFile ccc =
      addController("class CccController {\n" +
                    "  def action = { redirect(action: '" + oldPropertyName + "', controller: \"www\") }\n}\n");
    myFixture.configureFromExistingVirtualFile(www.getVirtualFile());

    myFixture.renameElementAtCaret(newName);

    if (oldName.equals(oldPropertyName)) oldName = newPropertyName;

    assertEquals("class WwwController {def " + newName + " = {} }", www.getText());

    assertEquals("<g:link url=\"[action: '" + newPropertyName + "']\" />", gsp1.getText());
    assertEquals("<g:link url='[action: \"" + newPropertyName + "\"]' />", gsp2.getText());
    assertEquals("<% g.link(action: '" + newPropertyName + "') %>", gsp3.getText());
    assertEquals("<% g.link(action: \"" + newPropertyName + "\") %>", gsp4.getText());
    assertEquals("<% g.link(action: \"" + oldName + "\") g.link(action: \"" + newPropertyName + "\") %>", gsp5.getText());
    assertEquals("<g:link action='" + newPropertyName + "' />", gsp6.getText());
    assertEquals("<g:link action='" + oldName + "' />", gsp7.getText());

    assertEquals(
      "class MyTagLib {\n" +
      "  def customTag = { out << link(action: \"" + newPropertyName + "\", controller: 'www') }\n}\n", tagLib.getText());

    assertEquals("""
                            class SomeTagLib {
                              def customTag = { out << link(action: \"""" + oldName + """
                            ", controller: 'www') }
                            }
                            """, someLib.getText());

    assertEquals(
      "class CccController {\n" +
      "  def action = { redirect(action: '" + newPropertyName + "', controller: \"www\") }\n}\n", ccc.getText());
  }

  public void testAction1() {
    doTestActions("Ttt", "ttt", "bbb", "bbb");
  }

  public void testAction2() {
    doTestActions("TTT", "TTT", "bbb", "bbb");
  }

  public void testAction3() {
    doTestActions("T100", "t100", "bbb", "bbb");
  }

  public void testAction4() {
    doTestActions("aB", "aB", "bbb", "bbb");
  }

  public void testTagLib() {
    PsiFile tagLib = addTaglib("class MyTagLib {  def Ctag<caret> = {} }");

    PsiFile gsp1 = myFixture.addFileToProject("grails-app/views/www/g1.gsp", "<g:ctag />");
    PsiFile gsp2 = myFixture.addFileToProject("grails-app/views/www/g2.gsp", "<g:Ctag /> <% Ctag() %> <g:ctag />");
    PsiFile gsp3 = myFixture.addFileToProject("grails-app/views/www/g3.gsp", "<% ctag() %>");

    myFixture.configureFromExistingVirtualFile(tagLib.getVirtualFile());

    String propertyName = "bbb";
    String fieldName = "bbb";

    myFixture.renameElementAtCaret(fieldName);

    assertEquals("class MyTagLib {  def " + fieldName + " = {} }", tagLib.getText());
    assertEquals("<g:" + propertyName + " />", gsp1.getText());
    assertEquals("<g:Ctag /> <% Ctag() %> <g:" + propertyName + " />", gsp2.getText());
    assertEquals("<% " + propertyName + "() %>", gsp3.getText());
  }
}
