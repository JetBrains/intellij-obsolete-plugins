// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tagSupport;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;
import org.jetbrains.plugins.groovy.grails.HddGrailsTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class GspResourceTagTest extends HddGrailsTestCase {
  public void testCompletionPluginsInContextPath() {
    prepareProjectStructure();

    myFixture.addFileToProject("a.gsp", "<g:resource contextPath='/plugins/<caret>' />");
    myFixture.testCompletionVariants("a.gsp", "someplugin2-1.0", "tomcat-1.0");

    myFixture.addFileToProject("b.gsp", "<% resource(contextPath:'/plugins/<caret>') %>");
    myFixture.testCompletionVariants("b.gsp", "someplugin2-1.0", "tomcat-1.0");

    myFixture.addFileToProject("c.gsp", "<g:resource contextPath='/plugins/someplugin2-1.0/<caret>' />");
    myFixture.testCompletionVariants("c.gsp", "css");

    myFixture.addFileToProject("d.gsp", "<g:resource dir='/plugins/someplugin2-1.0/<caret>' />");
    myFixture.testCompletionVariants("d.gsp", "css");

    myFixture.addFileToProject("e.gsp", "<g:resource dir='/<caret>' />");
    myFixture.testCompletionVariants("e.gsp", "css", "js");

    myFixture.addFileToProject("f.gsp", "<g:resource dir='<caret>' />");
    myFixture.testCompletionVariants("f.gsp", "css", "js");

    myFixture.addFileToProject("g.gsp", "<g:resource dir='/js/<caret>' />");
    myFixture.testCompletionVariants("g.gsp");

    myFixture.addFileToProject("h.gsp", "<g:resource plugin='someplugin2' dir='/<caret>' />");
    myFixture.testCompletionVariants("h.gsp", "css");

    myFixture.addFileToProject("i.gsp", "<g:resource plugin='someplugin2' dir='<caret>' />");
    myFixture.testCompletionVariants("i.gsp", "css");

    myFixture.addFileToProject("j.gsp", "<g:resource contextPath='/plugins/someplugin2-1.0' dir='<caret>' />");
    myFixture.testCompletionVariants("j.gsp", "css");

    myFixture.addFileToProject("l.gsp", "<g:resource contextPath='/plugins/someplugin2-1.0' dir='css' file='<caret>' />");
    myFixture.testCompletionVariants("l.gsp", "aaa.css", "bbb.css");

    myFixture.addFileToProject("k.gsp", "<g:resource contextPath='/plugins/someplugin2-1.0' file='css/<caret>' />");
    myFixture.testCompletionVariants("k.gsp", "aaa.css", "bbb.css");

    myFixture.addFileToProject("m.gsp", "<g:resource file='/plugins/someplugin2-1.0/css/<caret>' />");
    myFixture.testCompletionVariants("m.gsp", "aaa.css", "bbb.css");

    myFixture.addFileToProject("n.gsp", "<g:resource plugin='someplugin2' file='css/<caret>' />");
    myFixture.testCompletionVariants("n.gsp", "aaa.css", "bbb.css");

    myFixture.addFileToProject("o.gsp", "<g:resource file='<caret>' />");
    myFixture.testCompletionVariants("o.gsp", "css", "js");
  }

  public void _testHighlighting() {
    myFixture.addFileToProject("web-app/css/main.css", "");
    myFixture.addFileToProject("a.gsp", """
      <g:createLinkTo contextPath='<error descr="Cannot resolve file 'css'">css</error>' />
      <g:createLinkTo contextPath='/css/<error descr="Cannot resolve file ''"></error>' />
      <g:createLinkTo contextPath='/css' file='main.css' />
      
      <g:createLinkTo dir="/css/main.css" />
      <g:createLinkTo dir='/css/' file='main.css' />
      <g:createLinkTo dir="/css/<error descr="Cannot resolve file 'djhdssfkhfgdfjk.css'">djhdssfkhfgdfjk.css</error>" />
      <g:createLinkTo contextPath="/css" dir="main.css" />
      <g:createLinkTo contextPath="/css" dir="/main.css" />
      <g:createLinkTo contextPath="/" dir="/css/main.css" />
      
      <g:createLinkTo dir="/" file="/css/main.css" />
      <g:createLinkTo dir="css" file="main.css" />
      <g:createLinkTo dir="css" file="main.css/<error descr="Cannot resolve file ''"></error>" />
      
      <g:createLinkTo file="<error descr="Cannot resolve file 'main.css'">main.css</error>" />
      """);

    myFixture.testHighlighting(true, false, true, "a.gsp");
  }

  public void testRenameFile() {
    myFixture.addFileToProject("web-app/css/main.css", "");

    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <g:createLinkTo contextPath='/css' file='main.css' />
      <g:createLinkTo contextPath='/css/main.css' />
      <g:createLinkTo dir='/css/mai<caret>n.css' />
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.renameElementAtCaret("mmm.css");

    myFixture.checkResult("""
                            <g:createLinkTo contextPath='/css' file='mmm.css' />
                            <g:createLinkTo contextPath='/css/mmm.css' />
                            <g:createLinkTo dir='/css/mmm.css' />
                            """);
  }

  public void _testRenameDir() {
    myFixture.addFileToProject("web-app/css/subdir/main.css", "");

    PsiFile fileA = myFixture.addFileToProject("a.gsp", """
      <g:createLinkTo file='css/subdir/main.css' />
      
      <g:createLinkTo dir='css' file='/subdir/main.css' />
      <g:createLinkTo dir='css/subdir' file='/main.css' />
      <g:createLinkTo dir='css/subdir/main.css' />
      
      <g:createLinkTo contextPath='css' file='/subdir/main.css' />
      <g:createLinkTo contextPath='css/subdir' file='/main.css' />
      <g:createLinkTo contextPath='css/subdir/main.css' />
      
      <g:createLinkTo contextPath='css/' dir='subdir/main.css' />
      """);

    PsiFile fileB = myFixture.addFileToProject("b.gsp", """
      <%
      createLinkTo(file: 'css/subdir/main.css')
      
      createLinkTo(dir: 'css', file: '/subdir/main.css')
      createLinkTo(dir: 'css/subdir', file: '/main.css')
      createLinkTo(dir: 'css/subdir/main.css')
      
      createLinkTo(contextPath: 'css', file: '/subdir/main.css')
      createLinkTo(contextPath: 'css/subdir', file: '/main.css')
      createLinkTo(contextPath: 'css/subdir/main.css')
      
      createLinkTo(contextPath: 'css/', dir: 'subdir/main.css')
      %>
      """);

    PsiFile fileC = myFixture.addFileToProject("c.gsp", fileB.getText().replace("'", "\""));

    PsiFile fileController = addController("""              
                                             class CccController {
                                             def index = {
                                             createLinkTo(file: 'css/subdir/main.css')
                                             
                                             createLinkTo(dir: 'css', file: '/subdir/main.css')
                                             createLinkTo(dir: 'css/subdir', file: '/main.css')
                                             createLinkTo(dir: 'css/subdir/main.css')
                                             
                                             createLinkTo(contextPath: 'css', file: '/subdir/main.css')
                                             createLinkTo(contextPath: 'css/subdir', file: '/main.css')
                                             createLinkTo(contextPath: 'css/subdir/main.css')
                                             
                                             createLinkTo(contextPath: 'css/', dir: 'subdir/main.css')
                                             }
                                             }
                                             """);

    PsiFile xxx = myFixture.configureByText("rrr.gsp", "<g:resource dir='css/subdir<caret>' />");

    ArrayList<PsiFile> allFiles = new ArrayList<>(Arrays.asList(fileA, fileB, fileC, fileController));

    LinkedHashMap<Object, Object> oldContent = new LinkedHashMap<>();

    for (PsiFile file : allFiles) {
      oldContent.put(file.getName(), file.getText());
    }

    myFixture.renameElementAtCaret("ssssss");

    TestCase.assertEquals("<g:resource dir='css/ssssss' />", xxx.getText());

    for (PsiFile file : allFiles) {
      TestCase.assertEquals(((String)oldContent.get(file.getName())).replaceAll("subdir", "ssssss"), file.getText());
    }
  }

  public void testMove() {
    myFixture.addFileToProject("web-app/js/subdir/main.js", "");

    PsiFile file = myFixture.addFileToProject("a.gsp", """  
      <g:createLinkTo file='js/subdir/main.js' />
      
      <g:createLinkTo dir='js' file='/subdir/main.js' />
      <g:createLinkTo dir='js/subdir' file='/main.js' />
      <g:createLinkTo dir='js/subdir/main.js' />
      
      <g:createLinkTo contextPath='/js' file='/subdir/main.js' />
      <g:createLinkTo contextPath='/js/subdir' file='/main.js' />
      <g:createLinkTo contextPath='/js/subdir/main.js' />
      
      <g:createLinkTo contextPath='/js' dir='subdir/main.js' />
      """);

    myFixture.moveFile("web-app/js/subdir/main.js", "web-app/js");

    TestCase.assertEquals("""  
                            <g:createLinkTo file='js/main.js' />
                            
                            <g:createLinkTo dir='js' file='/main.js' />
                            <g:createLinkTo dir='js/subdir' file='/main.js' />
                            <g:createLinkTo dir='js/main.js' />
                            
                            <g:createLinkTo contextPath='/js' file='/main.js' />
                            <g:createLinkTo contextPath='/js/subdir' file='/main.js' />
                            <g:createLinkTo contextPath='/js/main.js' />
                            
                            <g:createLinkTo contextPath='/js' dir='main.js' />
                            """, file.getText());
  }

  public void testMove2() {
    myFixture.addFileToProject("web-app/js/subdir/main.js", "");
    myFixture.addFileToProject("web-app/xxx/yyy/aaa.js", "");

    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <g:createLinkTo file='js/subdir/main.js' />
      """);

    myFixture.moveFile("web-app/js/subdir/main.js", "web-app/xxx/yyy");

    TestCase.assertEquals("""
                            <g:createLinkTo file='xxx/yyy/main.js' />
                            """, file.getText());
  }

  private void prepareProjectStructure() {
    myFixture.addFileToProject("/plugins/someplugin2-1.0/Someplugin2GrailsPlugin.groovy", "");
    myFixture.addFileToProject("/plugins/someplugin2-1.0/_template.gsp", "Template text");
    myFixture.addFileToProject("/plugins/someplugin2-1.0/web-app/aaa.js", "Template text");
    myFixture.addFileToProject("/plugins/someplugin2-1.0/web-app/css/aaa.css", "Template text");
    myFixture.addFileToProject("/plugins/someplugin2-1.0/web-app/css/bbb.css", "Template text");

    myFixture.addFileToProject("/plugins/tomcat-1.0/TomcatGrailsPlugin.groovy", "");
    myFixture.addFileToProject("/plugins/tomcat-1.0/js/sss.js", "Template text");
    myFixture.addFileToProject("/plugins/tomcat-1.0/_templateUnderPlugin.gsp", "Template text");

    myFixture.addFileToProject("web-app/css/main.css", "");
    myFixture.addFileToProject("web-app/js/jjj.js", "");

    saveProperties("""
                     plugins.someplugin2=1.0
                     plugins.tomcat=1.0
                     """);

    GrailsTestUtil.createPluginXml(myFixture, "/plugins/tomcat-1.0");
    GrailsTestUtil.createPluginXml(myFixture, "/plugins/someplugin2-1.0");

    GrailsTestUtil.createBuildConfig(myFixture, ".", Map.of("grails.project.plugins.dir", "plugins"));
  }
}
