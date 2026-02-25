// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tagSupport;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.usageView.UsageInfo;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;
import org.jetbrains.plugins.groovy.grails.HddGrailsTestCase;

import java.util.Collection;
import java.util.Map;

public class GspRenderTagTest extends HddGrailsTestCase {
  public void testHighlighting() {
    myFixture.addFileToProject("grails-app/views/_template.gsp", "Template text");
    PsiFile file = myFixture.addFileToProject("grails-app/views/page.gsp",
                                              """
                                                <g:render template="/template"/>
                                                <g:render template=\"""" + error("/_template.gsp") + """
                                                "/>
                                                <g:render template="/grails-app/views/template"/>
                                                """);
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testCompletion() {
    myFixture.addFileToProject("grails-app/views/_template1.gsp", "Template text");
    myFixture.addFileToProject("grails-app/views/_template2.gsp", "Template text");
    myFixture.addFileToProject("grails-app/views/dir/_ttt.gsp", "Template text");
    myFixture.addFileToProject("grails-app/views/dir/somepage.gsp", "Template text");

    PsiFile page = myFixture.addFileToProject("grails-app/views/page.gsp", "<g:render template=\"/t<caret>\"/>");
    checkCompletionVariants(page, "template1", "template2", "test");// "test" is %ROOT%/test directory.

    PsiFile file = configureByView("page2.gsp", "<g:render template=\"/dir/t<caret>\"/>");
    myFixture.complete(CompletionType.BASIC);
    TestCase.assertEquals("<g:render template=\"/dir/ttt\"/>", file.getText());
  }

  public void testFindUsages() {
    PsiFile tmplFile = myFixture.addFileToProject("grails-app/views/_template.gsp", "Template text");

    String path = "grails-app/views/page.gsp";
    myFixture.addFileToProject(path, "<g:render template='/template'/> <g:rrr attr='/template'>");

    Collection<UsageInfo> usages = myFixture.findUsages(tmplFile);

    UsefulTestCase.assertSize(1, usages);
  }

  public void testRename() {
    PsiFile tmplFile = addView("_template.gsp", "Template text");

    PsiFile controllerFile = addController("class CccController { def index = { render(template: '/template') } }");

    PsiFile file = configureByView("page.gsp", "<g:render template='/grails-app/views/template<caret>'/>");
    PsiFile file2 = addView("page2.gsp", "<g:render template='/template'/>");
    PsiFile file3 = addView("page3.gsp", "<tmpl:/template />");

    myFixture.renameElementAtCaret("_ttt.gsp");
    TestCase.assertEquals("_ttt.gsp", tmplFile.getName());
    TestCase.assertEquals("<g:render template='/grails-app/views/ttt'/>", file.getText());
    TestCase.assertEquals("<g:render template='/ttt'/>", file2.getText());
    TestCase.assertEquals("<tmpl:/ttt/>", file3.getText());
    TestCase.assertEquals("class CccController { def index = { render(template: '/ttt') } }", controllerFile.getText());
  }

  public void testRenameUnderController() {
    PsiFile controllerFile = addController("""
                                             class CccController {
                                             def index = {
                                                 render(template: 'template')
                                                 render(template: '/ccc/template')
                                             }
                                             }
                                             """);
    addView("ccc/_template.gsp", "Template text");

    PsiFile file = configureByView("ccc/page.gsp", "<g:render template='template<caret>'/> <tmpl:template/>");
    PsiFile file2 = addView("ccc/page2.gsp", "<g:render template='/ccc/template'/>");
    PsiFile file3 = addView("ccc/page3.gsp", "<tmpl:/ccc/template /> <tmpl:template />");

    myFixture.renameElementAtCaret("_ttt.gsp");
    TestCase.assertEquals("<g:render template='ttt'/> <tmpl:ttt/>", file.getText());
    TestCase.assertEquals("<g:render template='/ccc/ttt'/>", file2.getText());
    TestCase.assertEquals("<tmpl:/ccc/ttt/> <tmpl:ttt/>", file3.getText());
    TestCase.assertEquals("""
                            class CccController {
                            def index = {
                                render(template: 'ttt')
                                render(template: '/ccc/ttt')
                            }
                            }
                            """, controllerFile.getText());
  }

  public void testRenameUnderPlugin() {
    GrailsTestUtil.createBuildConfig(myFixture, ".", Map.of("grails.project.plugins.dir", "plugins"));

    PsiFile tmplFile = myFixture.addFileToProject("/plugins/someplugin-1.0/grails-app/views/shared/_template.gsp", "Template text");
    myFixture.addFileToProject("/plugins/someplugin-1.0/SomepluginGrailsPlugin.groovy", "");
    saveProperties("plugins.someplugin=1.0");

    GrailsTestUtil.createPluginXml(myFixture, "/plugins/someplugin-1.0");

    PsiFile controllerFile = addController("""
                                             
                                             class CccController {
                                             def index = {
                                                 render(template: '/shared/template', plugin: 'someplugin')
                                                 render(template: '/plugins/someplugin/grails-app/views/shared/template')
                                                 render(template: '/shared/template', contextPath: '/plugins/someplugin')
                                                 render(template: '/template', contextPath: '/plugins/someplugin/grails-app/views/shared')
                                             }
                                             }
                                             """);

    PsiFile file1 = myFixture.addFileToProject("grails-app/views/ccc/page1.gsp", """
      
      <g:render template='/shared/template' plugin='someplugin'/>
      ${render(template: '/shared/template', plugin: 'someplugin')}
      """);
    PsiFile file2 = myFixture.addFileToProject("grails-app/views/ccc/page2.gsp",
                                               "<g:render template='/shared/template<caret>' contextPath='/plugins/someplugin/grails-app/views' />");

    myFixture.configureFromExistingVirtualFile(file2.getVirtualFile());
    myFixture.renameElementAtCaret("_ttt.gsp");
    TestCase.assertEquals("_ttt.gsp", tmplFile.getName());
    TestCase.assertEquals("""
                            
                            <g:render template='/shared/ttt' plugin='someplugin'/>
                            ${render(template: '/shared/ttt', plugin: 'someplugin')}
                            """, file1.getText());
    TestCase.assertEquals("<g:render template='/shared/ttt' contextPath='/plugins/someplugin/grails-app/views' />", file2.getText());
    TestCase.assertEquals("""
                            
                            class CccController {
                            def index = {
                                render(template: '/shared/ttt', plugin: 'someplugin')
                                render(template: '/plugins/someplugin/grails-app/views/shared/ttt')
                                render(template: '/shared/ttt', contextPath: '/plugins/someplugin')
                                render(template: '/ttt', contextPath: '/plugins/someplugin/grails-app/views/shared')
                            }
                            }
                            """, controllerFile.getText());
  }

  private void prepareProjectStructure() {
    addController("class CccController {}");

    myFixture.addFileToProject("grails-app/views/_templateUnderView.gsp", "Template text");
    myFixture.addFileToProject("grails-app/views/_templateUnderView2.gsp", "Template text");
    myFixture.addFileToProject("grails-app/views/notAtemplate.gsp", "Template text");
    myFixture.addFileToProject("grails-app/views/ccc/subfolder/_templateUnderCccSubfolder.gsp", "Template text");
    myFixture.addFileToProject("grails-app/views/ccc/subfolder/notAtemplate.gsp", "Template text");
    myFixture.addFileToProject("grails-app/views/ccc/_templateUnderCcc.gsp", "Template text");
    myFixture.addFileToProject("grails-app/views/ccc/notAtemplate.gsp", "Template text");
    myFixture.addFileToProject("grails-app/views/shared/_templateUnderShared.gsp", "Template text");
    myFixture.addFileToProject("grails-app/views/shared/notAtemplate.gsp", "Template text");

    myFixture.addFileToProject("/plugins/someplugin2-1.0/_template.gsp", "Template text");
    myFixture.addFileToProject("/plugins/someplugin2-1.0/Someplugin2GrailsPlugin.groovy", "");
    myFixture.addFileToProject("/plugins/tomcat-1.0/grails-app/views/ccc/subfolder/_templateUnderCccSubfolderPlugin.gsp", "Template text");
    myFixture.addFileToProject("/plugins/tomcat-1.0/TomcatGrailsPlugin.groovy", "");
    myFixture.addFileToProject("/plugins/tomcat-1.0/grails-app/views/ccc/_templateUnderCccPlugin.gsp", "Template text");
    myFixture.addFileToProject("/plugins/tomcat-1.0/grails-app/views/ccc/subfolder/notAtemplate.gsp", "Template text");
    myFixture.addFileToProject("/plugins/tomcat-1.0/grails-app/views/shared/_sharedPlugin.gsp", "Template text");
    myFixture.addFileToProject("/plugins/tomcat-1.0/_templateUnderPlugin.gsp", "Template text");
    myFixture.addFileToProject("/plugins/tomcat-1.0/grails-app/views/_templateUnderPlugin.gsp", "Template text");

    saveProperties("""
                     plugins.someplugin2=1.0
                     plugins.tomcat=1.0
                     """);

    GrailsTestUtil.createPluginXml(myFixture, "/plugins/someplugin2-1.0");
    GrailsTestUtil.createPluginXml(myFixture, "/plugins/tomcat-1.0");

    GrailsTestUtil.createBuildConfig(myFixture, ".", Map.of("grails.project.plugins.dir", "plugins"));
  }

  public void testPageWithoutController() {
    prepareProjectStructure();

    PsiFile file = myFixture.addFileToProject("grails-app/views/page.gsp", """
                                                                             
                                                                             <g:render template="templateUnderView" />
                                                                             <g:render template="fgdfnsfngfd/sdfsdfnsdjf/sdfsdfnsdfj" />
                                                                             <g:render template="/templateUnderView" />
                                                                             <g:render template="/ccc/templateUnderCcc"/>
                                                                             <g:render template="/shared/templateUnderShared"/>
                                                                             <g:render template="/ccc/subfolder/templateUnderCccSubfolder"/>
                                                                             
                                                                             <g:render template="/plugins/tomcat/templateUnderPlugin" />
                                                                             <g:render template="/plugins/tomcat/grails-app/views/ccc/templateUnderCccPlugin" />
                                                                             <g:render template="/tomcat/grails-app/views/ccc/templateUnderCccPlugin" contextPath="/plugins" />
                                                                             <g:render template=\"""" +
                                                                           error("/tomcat/grails-app/views/ccc/templateUnderCccPlugin") +
                                                                           "\" contextPath=\"/plugins/" +
                                                                           error("") +
                                                                           """
                                                                             " />
                                                                             <g:render template="/grails-app/views/ccc/templateUnderCccPlugin" contextPath="/plugins/tomcat" />
                                                                             <g:render template="/grails-app/views/ccc/templateUnderCccPlugin" contextPath="/plugins/tomcat/" />
                                                                             <g:render template="/templateUnderCccPlugin" contextPath="/plugins/tomcat/grails-app/views/ccc" />
                                                                             
                                                                             """);

    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testPageUnderController() {
    prepareProjectStructure();

    PsiFile file = myFixture.addFileToProject("grails-app/views/ccc/page.gsp",
                                              """
                                                
                                                <g:render template="subfolder/templateUnderCccSubfolder" />
                                                <g:render template="subfolder/templateUnderCccSubfolder" contextPath="" />
                                                <g:render template="subfolder/templateUnderCccSubfolder" contextPath="/" />
                                                <g:render template="/subfolder/templateUnderCccSubfolder" contextPath="/grails-app/views/ccc" />
                                                <g:render template=\"""" +
                                              error("templateUnderCccSubfolder") +
                                              """
                                                " contextPath="/grails-app/views/ccc/subfolder" />
                                                <g:render template="/templateUnderCccSubfolder" contextPath="/grails-app/views/ccc/subfolder" />
                                                
                                                <g:render template="/ccc/subfolder/templateUnderCccSubfolder" contextPath="/grails-app/views" />
                                                <g:render template=\"""" +
                                              error("ccc/subfolder/templateUnderCccSubfolder") +
                                              """
                                                " contextPath="/grails-app/views" />
                                                <g:render template="templateUnderCcc" contextPath="/grails-app/views" />
                                                
                                                <g:render template="/templateUnderView" />
                                                <g:render template="/shared/templateUnderShared" />
                                                
                                                Test EL
                                                <g:render template="dfsdfsdfsdfsdfdfsfsf" contextPath="${'/grails-app/view'}" />
                                                <g:render template="dfsdfsgdfafgafgadfg" plugin="${'tomcat'}" />
                                                <g:render template=\"""" +
                                              error("notATemplate") +
                                              """
                                                " contextPath="${'/grails-app/view'}" plugin="tomcat" />
                                                <g:render template="sansdjf${dfsf}" />
                                                
                                                Test template in plugin
                                                <g:render template="templateUnderCccPlugin" plugin="tomcat" />
                                                <g:render template=\"""" +
                                              error("templateUnderCcc") +
                                              "\" plugin=\"" +
                                              "<error descr=\"Cannot resolve symbol '" + "tomcat-1.0" + "'\">" + "tomcat-1.0" + "</error>" +
                                              """
                                                " />
                                                <g:render template=\"""" +
                                              error("templateUnderCccPlugin") +
                                              "\" plugin=\"" +
                                              "<error descr=\"Cannot resolve symbol '" + "tomcat-1.0" + "'\">" + "tomcat-1.0" + "</error>" +
                                              """
                                                " />
                                                <g:render template=\"""" +
                                              error("/templateUnderCccPlugin") +
                                              """
                                                " plugin="tomcat" />
                                                <g:render template=\"""" +
                                              error("templateUnderCcc") +
                                              """
                                                " ContextPath="/grails-app/views" plugin="tomcat" />
                                                <g:render template="/templateUnderPlugin" plugin="tomcat" />
                                                <g:render template="/plugins/tomcat/templateUnderPlugin" />
                                                <g:render template="/plugins/tomcat/grails-app/views/ccc/templateUnderCccPlugin" />
                                                <g:render template="/tomcat/grails-app/views/ccc/templateUnderCccPlugin" contextPath="/plugins" />
                                                <g:render template=\"""" +
                                              error("/tomcat/grails-app/views/ccc/templateUnderCccPlugin") +
                                              "\" contextPath=\"/plugins/" +
                                              error("") +
                                              """
                                                " />
                                                <g:render template="/grails-app/views/ccc/templateUnderCccPlugin" contextPath="/plugins/tomcat" />
                                                <g:render template="/grails-app/views/ccc/templateUnderCccPlugin" contextPath="/plugins/tomcat-1.0/" />
                                                <g:render template="/templateUnderCccPlugin" contextPath="/plugins/tomcat/grails-app/views/ccc" />
                                                """);

    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testPluginCompletion() {
    prepareProjectStructure();
    PsiFile page = myFixture.addFileToProject("grails-app/views/page.gsp", "<g:render plugin='<caret>'");
    checkCompletionVariants(page, "someplugin2", "tomcat");
  }

  public void testPluginCompletion2() {
    prepareProjectStructure();
    PsiFile page = myFixture.addFileToProject("grails-app/views/page.gsp", "<g:render contextPath='/plugins/<caret>'");
    checkCompletionVariants(page, "someplugin2", "tomcat");
  }

  public void testTemplateInWebapp() {
    myFixture.addFileToProject("grails-app/views/_ttt2.gsp", "");
    myFixture.addFileToProject("web-app/_ttt1.gsp", "");

    PsiFile file = addView("a.gsp", "<g:render template='/<caret>' ");
    checkCompletion(file, "ttt1", "ttt2");
  }

  private static String error(String text) {
    int index = text.lastIndexOf("/");
    final String file = text.substring(index + 1);
    String path = text.substring(0, index + 1);

    String formattedPath = path.replaceAll("[\\w\\-_.]+", "<error descr=\"Cannot resolve directory '$0'\">$0</error>");

    if (file.isEmpty()) {
      return formattedPath + "<error descr=\"Cannot resolve file ''\"></error>";
    }

    return formattedPath + "<error descr=\"Cannot resolve template '_" + file + ".gsp'\">" + file + "</error>";
  }
}
