// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.grails.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.statistics.StatisticsManager;
import com.intellij.psi.statistics.impl.StatisticsManagerImpl;
import com.intellij.testFramework.UsefulTestCase;
import groovy.lang.IntRange;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.dsl.GroovyDslFileIndex;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GrailsCompletionTest extends GrailsTestCase {

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/completion/");
  }

  public void testDomainClassNonStaticMethods() {
    final String path = "grails-app/domain/Foo.groovy";
    myFixture.copyFileToProject(getTestName(false) + ".groovy", path);
    Set<String> complites = new HashSet<>(Set.of("dirty", "dirtyPropertyNames", "attached"));
    myFixture.getCompletionVariants(path).forEach(complites::remove);

    assertTrue(complites.toString(), complites.isEmpty());
  }

  public void testFinderMethodsInDomainClass() {
    final String path = "grails-app/domain/Foo.groovy";
    myFixture.copyFileToProject(getTestName(false) + ".groovy", path);
    doTestCompletionVariants(path, "findByAge", "findByFamily", "findById", "findByName", "findByVersion");
  }

  public void testAllFinderMethodsInDomainClass() {
    final String path = "grails-app/domain/Foo.groovy";
    myFixture.copyFileToProject(getTestName(false) + ".groovy", path);
    doTestCompletionVariants(path, "findAllByAge", "findAllByFamily", "findAllById", "findAllByName", "findAllByVersion");
  }

  public void testFindByMethodInDomainClass() {
    final String path = "grails-app/domain/Foo.groovy";
    myFixture.copyFileToProject(getTestName(false) + ".groovy", path);
    doTestCompletionVariants(path, "findByNameAndAge", "findByNameAndId", "findByNameAndName", "findByNameAndVersion");
  }

  public void testFindByMethodWithThreeArgsInDomainClass() {
    final String path = "grails-app/domain/Foo.groovy";
    myFixture.copyFileToProject(getTestName(false) + ".groovy", path);
    doTestCompletionVariants(path);
    myFixture.checkResultByFile(getTestName(false) + ".groovy");
  }

  public void testFinderMethodsInGsp() {
    myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/Foo.groovy");
    doTestCompletionVariants(getTestName(false) + ".gsp", "findByAge", "findById", "findByName", "findByVersion");
  }

  public void testUnpackagedDomainClassInGsp() {
    myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/FooBar.groovy");
    myFixture.testCompletion(getTestName(false) + ".gsp", getTestName(false) + "_after.gsp");
  }

  @Ignore
  public void testGspInjectedJavaScript() {
    myFixture.testCompletionTyping(getTestName(false) + ".gsp", "", getTestName(false) + "_after.gsp");
  }

  public void testAttributeNameFinishWithEq() {
    myFixture.configureByFile(getTestName(false) + ".gsp");
    TestCase.assertTrue(myFixture.completeBasic().length > 4);
    myFixture.type('=');
    myFixture.checkResultByFile(getTestName(false) + "_after.gsp");
  }

  public void testOverrideStandardTag() {
    addTaglib("""
                
                class MyTagLib {
                  def createLink = {attr, body ->
                    out << attr.xxx
                  }
                }
                """);

    myFixture.configureByFile(getTestName(false) + ".gsp");
    LookupElement[] items = myFixture.completeBasic();
    TestCase.assertEquals(1, items.length);
    TestCase.assertEquals("xxx", items[0].getLookupString());
  }

  public void testJspTagCompletion() {
    myFixture.copyFileToProject("../fmt.tld", "WEB-INF/tld/fmt.tld");
    myFixture.testCompletion(getTestName(false) + ".gsp", getTestName(false) + "_after.gsp");
  }

  public void testDomainClassStaticProperties() {
    final String path = "grails-app/domain/Foo.groovy";
    myFixture.copyFileToProject(getTestName(false) + ".groovy", path);
    final List<String> stringList = new ArrayList<>(myFixture.getCompletionVariants(path));
    stringList.removeIf(s -> PsiJavaParserFacadeImpl.getPrimitiveType(s) != null);

    TestCase.assertTrue(stringList.containsAll(Set.of("belongsTo", "constraints", "embedded", "hasMany", "optionals", "transients")));
    myFixture.type('b');
    myFixture.type('e');
    myFixture.type('l');
    myFixture.type('o');
    myFixture.type('\n');
    myFixture.checkResultByFile(getTestName(false) + "_after.groovy");
  }

  public void testModifiersInGspGroovyDeclaration() {
    final List<String> data = myFixture.getCompletionVariants(getTestName(false) + ".gsp");
    TestCase.assertTrue(data.toString(), data.containsAll(Arrays.asList("final", "public", "protected", "private", "static", "int")));
  }

  public void testConstrainsPropertyInDomainClass() {
    myFixture.copyFileToProject(getTestName(false) + "Constraints.groovy", "src/java/DomainConstraints.groovy");
    myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/Domain.groovy");
    myFixture.testCompletionTyping("grails-app/domain/Domain.groovy", "\n", getTestName(false) + "_after.groovy");
  }

  public void testFindersWithBelongsTo() {
    myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/Domain.groovy");
    doTestCompletionVariants("grails-app/domain/Domain.groovy", "findByAuthor", "findById", "findByName", "findByVersion");
  }

  public void doTestCompletionVariants(String file, String... variants) {
    try {
      myFixture.testCompletionVariants(file, variants);
    }
    catch (Throwable t) {
      System.out.println("file: " + myFixture.getFile().getName());
      Editor editor = myFixture.getEditor();
      int offset = editor.getCaretModel().getOffset();
      StringBuilder text = new StringBuilder(editor.getDocument().getText());
      text.insert(offset, "<caret>");
      System.out.println("'" + text + "'");
      throw t;
    }
  }

  public void testFindersWithHasMany() {
    myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/Domain.groovy");
    doTestCompletionVariants("grails-app/domain/Domain.groovy", "findByBooks", "findById", "findByVersion");
  }

  public void testDomainClassDynamicMethods() {
    final String path = "grails-app/domain/Foo.groovy";
    myFixture.copyFileToProject(getTestName(false) + ".groovy", path);
    myFixture.configureByFile(path);
    myFixture.completeBasic();
    myFixture.assertPreferredCompletionItems(0, "id", "ident", "isDirty", "isDirty");
  }

  public void testGdslInGspScript() {
    final PsiFile file = myFixture.addFileToProject("src/groovy/aaa.gdsl", """
      
      contributor(context(scope: scriptScope(), filetypes: [".gsp"])) {
         property name: "foooo", type: String
      }
      """);
    GroovyDslFileIndex.activate(file.getVirtualFile());

    myFixture.configureFromExistingVirtualFile(myFixture.addFileToProject("views/index.gsp", "${foo<caret>}").getVirtualFile());
    myFixture.completeBasic();
    myFixture.type("\n");
    myFixture.checkResult("${foooo<caret>}");
  }

  public void testTmplNamespace() {
    addView("_ttt.gsp", "Template Text");

    configureByView("file.gsp", "<tmpl:t<caret>");

    LookupElement[] elements = myFixture.completeBasic();
    TestCase.assertNotNull(elements);
    UsefulTestCase.assertEmpty(elements);
  }

  public void testTmplNamespace1a() {
    addController("class CccController {}");

    addView("ccc/_ttt.gsp", "Template Text");

    configureByView("ccc/file.gsp", "<tmpl:t<caret>");

    LookupElement[] elements = myFixture.completeBasic();
    TestCase.assertNull(elements);
    myFixture.checkResult("<tmpl:ttt<caret>");
  }

  public void testTmplNamespace2() {
    addController("class XxxController {}");

    addView("_ttt.gsp", "Template Text");
    addView("xxx/_xxx.gsp", "Template Text");
    addView("xxx/_xxx2.gsp", "Template Text");

    addView("xxx/file.gsp", "<tmpl:<caret>");

    doTestCompletionVariants("grails-app/views/xxx/file.gsp", "xxx", "xxx2");
  }

  public void testGspClosingJavascriptTag() {
    myFixture.configureByText("a.gsp", "<g:javascript></<caret>g:javascript>");
    myFixture.completeBasic();
    UsefulTestCase.assertSameElements(myFixture.getLookupElementStrings(), "g:javascript");
  }

  public void testGspWithTagLibGCompletion() {
    myFixture.configureByText("a.gsp", """
      
      <%@ taglib prefix="g" uri="/web-app/WEB-INF/tld/grails.tld" %>
      <g:createLi<caret>
      """);
    myFixture.completeBasic();
    UsefulTestCase.assertSameElements(myFixture.getLookupElementStrings(), "createLink", "createLinkTo");
  }

  public void testHtmlGspTagsStatisticalConflict() {
    ((StatisticsManagerImpl)StatisticsManager.getInstance()).enableStatistics(myFixture.getTestRootDisposable());
    String text = """
      
      <%@ taglib prefix="g" uri="/web-app/WEB-INF/tld/grails.tld" %>
      <form<caret>x
      """;

    for (Integer i : new IntRange(0, 5)) {
      myFixture.configureByText("a" + i + ".gsp", text);
      myFixture.completeBasic();
      assertEquals(List.of("form", "g:form", "g:formatDate", "g:formatNumber", "g:formRemote", "g:uploadForm"),
                   myFixture.getLookupElementStrings());
      getLookup().setCurrentItem(getLookup().getItems().get(1));
      myFixture.type(" ");
    }
  }

  private LookupImpl getLookup() {
    return ((LookupImpl)(LookupManager.getActiveLookup(myFixture.getEditor())));
  }
}
