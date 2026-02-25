// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.grails;

import com.intellij.codeInsight.daemon.impl.analysis.HtmlUnknownTargetInspection;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GrailsHighlightingTest extends GrailsTestCase {

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/highlighting/");
  }

  public void testFileReferenceWithGspInjections() throws Throwable {
    myFixture.enableInspections(new HtmlUnknownTargetInspection());
    doTest();
  }

  public void testUnknownArgsAttribute() throws Throwable {
    doTest();
  }

  private void doTest() throws Throwable {
    myFixture.testHighlighting(true, false, false, getTestName(false) + ".gsp");
  }

  public void testJavascriptWithGspInjections() throws Throwable {
    doTest();
  }

  public void testFinderMethods() throws Throwable {
    myFixture.enableInspections(new GrUnresolvedAccessInspection());
    myFixture.enableInspections(new GroovyAssignabilityCheckInspection());
    final VirtualFile file = myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/MyDomain.groovy");
    myFixture.testHighlighting(true, false, false, file);
  }

  public void testHasManyBelongsTo() throws Throwable {
    myFixture.enableInspections(new GrUnresolvedAccessInspection());
    final VirtualFile file = myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/Ccc.groovy");
    myFixture.testHighlighting(true, false, false, file);
  }

  public void testProperties() throws Throwable {
    myFixture.enableInspections(new GrUnresolvedAccessInspection());
    final VirtualFile file = myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/MyDomain.groovy");
    PsiTestUtil.addSourceRoot(myFixture.getModule(), file.getParent());
    myFixture.testHighlighting(true, false, false, file);
  }

  public void testJspTagInGsp() throws Throwable {
    myFixture.copyFileToProject("../fmt.tld", "WEB-INF/tld/fmt.tld");
    doTest();
  }

  public void testDomainFindersWithInheritedProperties() throws Throwable {
    final VirtualFile file = myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/Domains.groovy");
    myFixture.testHighlighting(true, false, false, file);
  }

  public void testCustomTaglibWithPrivateField() throws Throwable {
    PsiFile taglib = addTaglib("""
                                 class MyTagLib {
                                   static namespace = "my"
                                   private def foo = {}
                                   def getFoo() {}
                                 }
                                 """);

    PsiFile gsp = addView("error.gsp", "<my:foo/>");
    myFixture.testHighlighting(true, false, false, gsp.getVirtualFile());
  }

  public void testHasManyProperties() {
    PsiFile file = addDomain("""
                               
                               class MyDomain {
                                 def name;
                                 static hasMany=[domains:MyDomain, hints:String]
                               
                                 static def getNewInstance() {
                                   return new MyDomain(domains:null, name:'Max');
                                 }
                               }
                               """);
    PsiTestUtil.addSourceRoot(myFixture.getModule(), file.getVirtualFile().getParent());

    myFixture.testHighlighting(true, false, false, file.getVirtualFile());
  }

  public void testMappedBy() {
    VirtualFile file = myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/MyDomain.groovy");
    myFixture.testHighlighting(true, false, false, file);
  }

  public void testMappedByErrors1() {
    VirtualFile file = myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/MyDomain.groovy");
    myFixture.testHighlighting(true, false, false, file);
  }

  public void testExplicitIdProperty() {
    PsiFile file = addDomain("""
                               
                               class MyDomain{
                                 def id
                               }
                               """);
    myFixture.testHighlighting(true, false, false, file.getVirtualFile());
  }

  public void testAdderMethodParameters() throws Exception {
    myFixture.enableInspections(new GroovyAssignabilityCheckInspection());
    myFixture.addFileToProject("grails-app/domain/Book.groovy", """
       class Book { String title }\s\
      """);
    PsiFile file = myFixture.addFileToProject("grails-app/domain/Author.groovy", """
      
      class Author{
        static hasMany=[books:Book]
      
        def foo(Author a) {
          a.addToBooks(title:'Harry Potter')
          a.addToBooks<warning descr="'addToBooks' in 'Author' cannot be applied to '(java.lang.Integer)'">(2)</warning>
        }
      }
      """);

    PsiTestUtil.addSourceRoot(myFixture.getModule(), file.getVirtualFile().getParent());

    myFixture.testHighlighting(true, false, false, file.getVirtualFile());
  }

  public void testCyclicDomainFieldReferences() throws Exception {
    PsiFile file = myFixture.addFileToProject("grails-app/domain/Author.groovy", """
      
      class Author{
        def xxx = aaa
        def isAaa() { xxx }
      
        def yyy = findByYyy(2)
      }
      """);

    myFixture.testHighlighting(true, false, false, file.getVirtualFile());
  }

  public void testAmbiguousCodeBlocks() {
    PsiFile file = myFixture.configureByText("a.gsp", """
      
      <g:select from="${users}" optionValue="${{print 2; <error descr="Ambiguous code block">{}</error>.call() }}"/>
      """);

    myFixture.testHighlighting(true, false, false);
  }

  public void testTuple() {
    myFixture.configureByText("a.groovy", """
      
      def tuple(e) {}
      def s,n
      while (true) {
          (s, n) = tuple(n)
      }
      """);
    myFixture.testHighlighting(true, false, false);
  }
}
