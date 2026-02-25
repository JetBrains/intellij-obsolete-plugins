// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiVariable;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;

public class GspResolveVariablesFromModelTest extends GrailsTestCase {

  @Override
  protected boolean useGrails14() {
    return true;
  }

  public void testResolveVariablesFromModel1() {
    addController("""                  
                    class CccController {
                    
                     def index = {
                      [mvarAaa:"aaa", mvarBbb: 34, mvarCcc:sdfsdfsd]
                     }
                    
                     def x = {
                      if (1 > 2) {
                       return [mvarList:new ArrayList()]
                      }
                    
                      [mvarList: new LinkedList(), mvarSet: new TreeSet()]
                     }
                    }
                    """);
    addView("ccc/xxx.gsp", """
      
      <g:include view='ccc/index.gsp' model='[mvar111: asdasdassdasd]' />
      <g:include view='ccc/index.gsp' model='[mvar111: null]' />
      <g:include view='ccc/index.gsp' model='[mvar111: new ArrayList<Double>()]' />
      <g:include view='ccc/index.gsp' model='[mvar111: new ArrayList<Integer>()]' />
      
      <g:include view='ccc/index.gsp' model='["mvar222": "sss"]' />
      <g:include view='ccc/index.gsp' model="['mvar333': 'sss']" />
      <g:include view='ccc/index.gsp' model='[""\"mvar444""\": "sss"]' />
      ${include(view: 'ccc/index.gsp', model: ["mvar555": "sss"]) }
      '""");

    configureByView("ccc/index.gsp", "<% out << mvar<caret> %>");

    ArrayList<Object> res = new ArrayList<>();
    for (LookupElement l : myFixture.completeBasic()) {
      res.add(l.getLookupString());
      res.add(((PsiVariable)extractElementFromLookup(l)).getType().getPresentableText());
    }


    Assert.assertEquals(new ArrayList<>(
                          Arrays.asList("mvar111", "ArrayList<? extends Number & Comparable<? extends Number & Comparable<?>>>", "mvar222", "String", "mvar333",
                                        "String", "mvar444", "String", "mvar555", "String", "mvarAaa", "String", "mvarBbb", "Integer", "mvarCcc", "Object")),
                        res);
  }

  private static Object extractElementFromLookup(LookupElement l) {
    final Object o = l.getObject();
    if (o instanceof GroovyResolveResult) return ((GroovyResolveResult)o).getElement();
    return o;
  }

  public void testResolveVariablesFromModel2() {
    addController("""
                    class CccController {
                    
                     def index = {
                      [mvarAaa:"aaa", mvarBbb: 34, mvarCcc:sdfsdfsd]
                     }
                    
                     def x = {
                      if (1 > 2) {
                       return [mvarList:new A()]
                      }
                    
                      [mvarList: new B(), mvarSet: new TreeSet()]
                     }
                    }
                    
                    class P {}
                    class A extends P {}
                    class B extends P {}
                    """);

    configureByView("ccc/x.gsp", "<% out << mvar<caret> %>");

    ArrayList<Object> res = new ArrayList<>();
    for (LookupElement l : myFixture.completeBasic()) {
      res.add(l.getLookupString());
      res.add(((PsiVariable)extractElementFromLookup(l)).getType().getPresentableText());
    }

    Assert.assertEquals(new ArrayList<>(Arrays.asList("mvarList", "P", "mvarSet", "TreeSet")), res);
  }

  public void testRenameDeclaration() {
    PsiFile controllerFile = configureByController(
      """
        class CccController {
         def index = {
           if (params.xxx) {
             return [mvarAaa:"aaa"]
           }
        
           if (params.yyy) {
             return [mvarAaa<caret>:"bbb"]
           }
        
          [mvarAaa:"ccc"]
         }
        }
        """);

    PsiFile gspFile = addView("ccc/index.gsp", "<% out << mvarAaa %>");

    myFixture.renameElementAtCaret("xxx");

    Assert.assertEquals(
      """
        class CccController {
         def index = {
           if (params.xxx) {
             return [xxx:"aaa"]
           }
        
           if (params.yyy) {
             return [xxx:"bbb"]
           }
        
          [xxx:"ccc"]
         }
        }
        """, controllerFile.getText());

    Assert.assertEquals("<% out << xxx %>", gspFile.getText());
  }

  public void testRenameDeclaration2() {
    PsiFile controllerFile = configureByController(
      """
        class CccController {
         def index = {
           def model = [:];
           model << [mvarAaa<caret>: "aaa"]
           model
         }
        }
        """);

    PsiFile gspFile = addView("ccc/index.gsp", "<% out << mvarAaa %>");

    myFixture.renameElementAtCaret("xxx");

    Assert.assertEquals(
      """
        class CccController {
         def index = {
           def model = [:];
           model << [xxx: "aaa"]
           model
         }
        }
        """, controllerFile.getText());

    Assert.assertEquals("<% out << xxx %>", gspFile.getText());
  }

  public void testRenameTagDeclaration() {
    PsiFile f = configureByView("ccc/xxx.gsp", """
      <g:include view='ccc/index.gsp' model='${ [mvarAaa<caret>: 'aaa']}'>
      <g:include view='ccc/index.gsp' model='${ ["mvarAaa": 'aaa']}'>
      """);

    PsiFile gspFile = addView("ccc/index.gsp", "<% out << mvarAaa %>");

    myFixture.renameElementAtCaret("xxx");

    Assert.assertEquals("""
                          <g:include view='ccc/index.gsp' model='${ [xxx: 'aaa']}'>
                          <g:include view='ccc/index.gsp' model='${ [xxx: 'aaa']}'>
                          """, f.getText());

    Assert.assertEquals("<% out << xxx %>", gspFile.getText());
  }

  public void testRenameGroovyDeclaration() {
    PsiFile f = configureByView("ccc/xxx.gsp", "${include(view:'ccc/index.gsp', model:[mvarAaa<caret>: 'aaa'])}");

    PsiFile gspFile = addView("ccc/index.gsp", "<% out << mvarAaa %>");

    myFixture.renameElementAtCaret("xxx");

    Assert.assertEquals("${include(view:'ccc/index.gsp', model:[xxx: 'aaa'])}", f.getText());

    Assert.assertEquals("<% out << xxx %>", gspFile.getText());
  }

  public void testRenameUsage() {
    PsiFile controllerFile = addController(
      """
        class CccController {
        
        def index = {
           if (params.xxx) {
             return [mvarAaa:"aaa"]
           }
        
           if (params.yyy) {
             return [mvarAaa:"bbb"]
           }
        
          [mvarAaa:"ccc"]
        }
        }
        """);
    PsiFile otherView = addView("ccc/otherView.gsp", "<g:include view='/ccc/index.gsp' model='[mvarAaa: 13]' />");

    configureByView("ccc/index.gsp", "<% out << mvarAaa<caret> %>");

    myFixture.renameElementAtCaret("xxx");

    Assert.assertEquals(
      """
        class CccController {
        
        def index = {
           if (params.xxx) {
             return [xxx:"aaa"]
           }
        
           if (params.yyy) {
             return [xxx:"bbb"]
           }
        
          [xxx:"ccc"]
        }
        }
        """, controllerFile.getText());

    Assert.assertEquals("<g:include view='/ccc/index.gsp' model='[xxx: 13]' />", otherView.getText());

    myFixture.checkResult("<% out << xxx %>");
  }

  public void testInitModelInManyPlaces() {
    addController(
      """
        class CccController {
        
        def zzz
        
        def index = {
           def model1 = [mvarCcc1: 'ccc'];
           if (params.xxx) {
             model1 = [mvarAaa1:"aaa"]
           }
           if (params.yyy) {
             model1 = [mvarXxx1: 'xxx']
             model1 = [mvarBbb1:"bbb"]
             model1 << [mvarsSss1: "bbb"]
           }
           if (params.zzz) return zzz;
        
           if (a == 1) return model1
        
           model1 = [mvarXxx2: 'xxx']
        
           def model2 = [mvarCcc2: 'ccc2'];
           if (params.xxx) {
             model2 = [mvarAaa2:"aaa"]
           }
           if (params.yyy) {
             model2 = [mvarBbb2:"bbb"]
           }
        
           model2
        }
        }
        """);
    configureByView("ccc/index.gsp", "<% out << mvar<caret> %>");

    checkCompletion("mvarAaa1", "mvarBbb1", "mvarCcc1", "mvarAaa2", "mvarBbb2", "mvarCcc2", "mvarsSss1");
    checkNonExistingCompletionVariants("mvarXxx1", "mvarXxx2");
  }

  public void testModelPassesInGsp() {
    addController("""
                    class CccController {
                      def index1 = { render(template: '/t', model: [ccc111: 1]) }
                      def index2 = { render([template: '/t', model: [ccc222: 2]]) }
                    }
                    """);

    addView("a.gsp", """
      
      ${render(template: '/t', model: [ggg: 1])}
      <g:render template='/t' contextPath='xxxxx' model='${[xxx:'aaa']}' />
      <g:render template='/t' model='${   [ccc:'aaa']  }' />
      <g:render template='/t' model='[ddd:"aaa"]' />
      <g:render template='/t' model='${[aaa:'aaa', bbb: 'bbb']}' />
      
      <tmpl:/t rrr1='r' rrr2='3' />
      <tmpl:/trr xxx2='x' />
      
      """);
    configureByView("_t.gsp", "${<caret>}");

    checkCompletion("aaa", "bbb", "ccc", "ddd", "ggg", "ccc111", "ccc222", "rrr1", "rrr2");
    checkNonExistingCompletionVariants("xxx", "xxx2");
  }

  public void testRenameTemplate() {
    configureByController("""
                            class CccController {
                              def index = {
                                render(template: "ttt", model: [aaa<caret>: 'aaa'])
                              }
                            }
                            """);

    PsiFile gsp = addView("ccc/index.gsp", "<tmpl:ttt aaa='bbb'/>");

    PsiFile t = addView("ccc/_ttt.gsp", "${ aaa }");

    myFixture.renameElementAtCaret("z");

    Assert.assertEquals("<tmpl:ttt z='bbb'/>", gsp.getText());
    Assert.assertEquals("${ z }", t.getText());
  }

  public void testMethodAsAction() {
    configureByController(
      """
        class CccController {
          def index(String param1, String param2) {
            if (1 == 2) {
              return [aaa<caret>: 1]
            }
        
            [aaa: '2']
          }
        }
        """);

    PsiFile gsp = addView("ccc/index.gsp", "${aaa}");

    myFixture.renameElementAtCaret("zzz111");

    myFixture.checkResult(
      """
        class CccController {
          def index(String param1, String param2) {
            if (1 == 2) {
              return [zzz111: 1]
            }
        
            [zzz111: '2']
          }
        }
        """);
    Assert.assertEquals("${zzz111}", gsp.getText());
  }
}
