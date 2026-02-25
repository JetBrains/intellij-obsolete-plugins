// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrailsCriteriaBuilderTest extends GrailsTestCase {
  @Override
  protected boolean needGormLibrary() {
    return true;
  }

  @Override
  protected boolean needHibernate() {
    return true;
  }

  private void addDomainClass() {
    addDomain("""
                
                class Ddd {
                  String aaa
                  String bbb
                }
                """);
  }

  public void testResolveCriteriaMethods() {
    addDomainClass();

    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """
      
      import grails.orm.HibernateCriteriaBuilder
      
      class Ggg {
      
        public def getBuilder() {
          return new HibernateCriteriaBuilder();
        }
      
        public void someMethod() {
          (new HibernateCriteriaBuilder()) {
            ge1Property("aaa", "bbb")
          }
      
          (getBuilder()) {
            ge1Property("aaa", "bbb")
          }
      
          def b = Ddd.createCriteria()
          b {
            ge1Property("aaa", "bbb")
          }
      
          b.get {
            ge1Property("aaa", "bbb")
          }
      
          b.get({
            ge1Property("aaa", "bbb")
          })
      
          Ddd.withCriteria {
            geProperty("aaa", "bbb")
          }
          Ddd.withCriteria([:], {
            geProperty("aaa", "bbb")
          })
        }
      }
      """);
    String text = file.getText();
    Matcher m = Pattern.compile("geProperty").matcher(text);
    while (m.find()) {
      PsiReference ref = file.findReferenceAt(m.start());
      PsiElement resolve = ref.resolve();

      if (!(resolve instanceof PsiMethod)) {
        fail("Method is not resolved:\n" + text.substring(0, m.start()) + "!!!" + m.group() + "!!!" + text.substring(m.end()));
      }
    }
  }

  private void checkMethodCompletion(String classText) {
    addDomainClass();

    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", classText);

    checkCompletion(file, "eq", "geProperty", "order", "isNotEmpty", "between", "not", "sizeEq", "projections");
  }

  public void testMethodCompletionInCriteria() {
    checkMethodCompletion("""
                            
                            def criteria = Ddd.createCriteria()
                            criteria.list {
                                <caret>
                            }
                            """);
  }

  public void testMethodCompletionInCriteria2() {
    checkMethodCompletion("""
                            
                            def criteria = Ddd.createCriteria()
                            criteria.list {
                              and {
                                or {
                                  <caret>
                                }
                              }
                            }
                            """);
  }

  public void testMethodCompletionAfterNew() {
    checkMethodCompletion("Ddd.createCriteria().<caret>");
  }

  public void testMethodCompletionInWithCriteria() {
    checkMethodCompletion("Ddd.withCriteria({ <caret> })");
  }

  public void testStringLiteralAsMethodName() {
    addDomainClass();

    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """
      
      Ddd.withCriteria({
        "in"("aaa<caret>", ['variant1', 'variant2'])
      })
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    PsiElement elementAtCaret = myFixture.getElementAtCaret();

    UsefulTestCase.assertInstanceOf(elementAtCaret, GrField.class);
  }

  public void testReturnType() {
    addDomainClass();

    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """
      
      def criteria = Ddd.createCriteria()
      
      println( criteria.get({}).aaa )
      println( criteria.get {} .aaa )
      println( criteria.list({}).size() )
      println( criteria.list {} .get(0).aaa )
      println( criteria {} .get(0).aaa )
      println( criteria({}).get(0).aaa )
      
      Ddd.withCriteria {}.get(0).aaa
      Ddd.withCriteria (uniqueResult:false) {}.get(0).aaa
      Ddd.withCriteria(uniqueResult:true, {}).aaa
      Ddd.withCriteria(uniqueResult:true) {}.aaa
      Ddd.withCriteria([uniqueResult:true]) {}.aaa
      Ddd.withCriteria([uniqueResult:true], {}).aaa
      
      (Ddd.createCriteria()) {}.get(0).aaa
      (Ddd.createCriteria())({}).get(0).aaa
      Ddd.createCriteria().list({}).get(0).aaa
      Ddd.createCriteria().list({}, aaa: 1).get(0).aaa
      Ddd.createCriteria().list(aaa: 1, {}).get(0).aaa
      Ddd.createCriteria().list([aaa: 1], {}).get(0).aaa
      Ddd.createCriteria().get({}).aaa
      """);

    GrailsTestCase.checkResolve(file);
  }

  public void testPropertyRename() {
    addDomainClass();

    String closureBody = """
      {
        property(#NAME)
        distinct(#NAME)
        distinct(["bbb", #NAME])
        distinct(["bbb", #NAME], "aaa")
        neProperty("bbb", #NAME, "aaa")
        and {
          eq(#NAME, "aaa")
          or {
            not {
              neProperty("bbb", #NAME)
            }
          }
        }
      }
      """;
    ArrayList<String> fileTemplates = new ArrayList<>(Arrays.asList("""
                                                                      
                                                                      def criteria = Ddd.createCriteria()
                                                                      criteria #BODY
                                                                      """, "(Ddd.createCriteria()) #BODY", "Ddd.withCriteria #BODY",
                                                                    "Ddd.createCriteria().list([:]) #BODY", "Ddd.withCriteria(#BODY)",
                                                                    "Ddd.withCriteria([:]) #BODY", "Ddd.createCriteria().get #BODY",
                                                                    "Ddd.createCriteria().scroll(#BODY)",
                                                                    "Ddd.createCriteria().buildCriteria(#BODY)",
                                                                    "Ddd.createCriteria().and(#BODY)"));

    String oldBody = closureBody.replace("#NAME", "\"aaa\"");

    ArrayList<PsiFile> files = new ArrayList<>();

    PsiFile gspFile = addView("a.gsp", "<% Ddd.createCriteria().list([:]) " + oldBody + " %>");

    for (int i = 0; i < fileTemplates.size(); i++) {
      files.add(myFixture.addFileToProject("src/groovy/Ggg" + i + ".groovy", fileTemplates.get(i).replace("#BODY", oldBody)));
    }


    PsiFile fileToRename = myFixture.addFileToProject("src/groovy/ToRename.groovy", "Ddd.aaa<caret>");
    myFixture.configureFromExistingVirtualFile(fileToRename.getVirtualFile());

    myFixture.renameElementAtCaret("z3");

    String newBody = closureBody.replace("#NAME", "\"z3\"");

    TestCase.assertEquals("<% Ddd.createCriteria().list([:]) " + newBody + " %>", gspFile.getText());

    for (int i = 0; i < fileTemplates.size(); i++) {
      assertEquals(fileTemplates.get(i).replace("#BODY", newBody), files.get(i).getText());
    }
  }

  public void testCompletion() {
    addDomain("""
                
                    class Ddd {
                      static int zzz = 23
                      private int xxx
                      public String getField() {}
                      public String setField() {}
                    }
                """);

    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", "Ddd.createCriteria().get({ eq('<caret>', 'aaa') })");
    checkCompletionVariants(file, "field", "id", "version");
  }

  public void testRenameFieldDefinedByMethod() {
    configureByDomain("""
                        
                            class Ddd {
                              public String getField<caret>() {}
                              public String setField() {}
                            }
                        """);

    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", "Ddd.createCriteria().get({ eq('field', 'aaa') })");

    myFixture.renameElementAtCaret("getVvv");

    TestCase.assertEquals("Ddd.createCriteria().get({ eq('vvv', 'aaa') })", file.getText());
  }

  public void testCriteriaInDomainClass() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                               
                                 String sss
                                 String fff
                               
                                 public static def getAll() {
                                   return createCriteria().get({
                                     geProperty("<caret>", "fff")
                                   })
                                 }
                               
                               }
                               """);
    checkCompletionVariants(file, "sss", "id", "fff", "version");
  }

  public void testIncompleteParams() {
    addDomainClass();
    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", "Ddd.createCriteria().eq('<caret>')");
    checkCompletionVariants(file, "aaa", "bbb", "id", "version");
  }

  public void _testCompletionInStaticMethod() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String sss
                               
                                 public static def getAll() {
                                   return createCriteria().get({
                                     e<caret>
                                   })
                                 }
                               
                               }
                               """);
    checkCompletion(file, "eq", "eqProperty");
  }

  private void addCustomCriteriaBuilder() {
    myFixture.addFileToProject("src/groovy/CustomCriteriaBuilder.groovy", """
      
      class CustomCriteriaBuilder extends grails.orm.HibernateCriteriaBuilder {
        public CustomCriteriaBuilder(String s, int x, Class targetClass) {
      
        }
      }
      
      """);
  }

  public void testCustomBuilder() {
    addDomainClass();
    addCustomCriteriaBuilder();

    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """
      
      def builder = new CustomCriteriaBuilder("asdas", 5, Ddd.class)
      def x = builder
      x.get {
        eq("aaa", "sss")
      }
      
      new CustomCriteriaBuilder("asdas", 5, Ddd).list {
        eq("aaa<caret>", "sss")
      }
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    String oldText = file.getText();

    myFixture.renameElementAtCaret("qwer");

    TestCase.assertEquals(oldText.replace("aaa", "qwer"), file.getText());
  }

  public void testCustomCriteriaBuilderCompletion() {
    addCustomCriteriaBuilder();

    checkMethodCompletion("""
                            
                            def builder = new CustomCriteriaBuilder("asdas", 5, Ddd.class)
                            def x = builder
                            x.get {
                              and {
                                <caret>
                              }
                            }
                            """);
  }

  public void testNamedQuery() {
    PsiFile domainClass = configureByDomain("""
                                              
                                              class Ddd {
                                                String aaa<caret>
                                                String bbb
                                              
                                                static namedQueries = {
                                                  zzz0() {
                                                    eq("aaa", "aaa")
                                                    isNull("aaa")
                                                  }
                                              
                                                  zzz1 {
                                                    eqProperty("aaa", "aaa")
                                                  }
                                              
                                                  zzz2({
                                                    eqProperty("aaa", "aaa")
                                                  })
                                              
                                                  zzz2({ val ->
                                                    eq("aaa", val)
                                                  })
                                                }
                                              }
                                              """);

    myFixture.renameElementAtCaret("field");

    TestCase.assertEquals("""
                            
                            class Ddd {
                              String field
                              String bbb
                            
                              static namedQueries = {
                                zzz0() {
                                  eq("field", "aaa")
                                  isNull("field")
                                }
                            
                                zzz1 {
                                  eqProperty("field", "field")
                                }
                            
                                zzz2({
                                  eqProperty("field", "field")
                                })
                            
                                zzz2({ val ->
                                  eq("field", val)
                                })
                              }
                            }
                            """, domainClass.getText());
  }

  public void testMethodFromCriteriaCompletion() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String name;
                               
                                 static {
                                   def x = Ddd.createCriteria();
                                   x.c<caret>
                                 }
                               }
                               """);
    checkCompletion(file, "createAlias", "createCriteria");
  }

  public void testMethodFromCriteriaHighlighting() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    PsiFile file = configureByDomain("""
                                       
                                       class Ddd {
                                         String name;
                                       
                                         static {
                                           def x = Ddd.createCriteria();
                                           x.firstResult(0)
                                           x.uniqueResult()
                                           x.list()
                                         }
                                       
                                       }
                                       """);

    myFixture.checkHighlighting(true, false, true);
    GrailsTestCase.checkResolve(file);
  }

  public void testNotAPersistentProperty() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String foo;
                               
                                 def xxx;
                               
                                 static namedQueries = {
                                   aaa {
                                     eq '<caret>'
                                   }
                                 }
                               }
                               """);
    checkCompletion(file, "foo", "id", "version");
    checkNonExistingCompletionVariants("xxx");
  }

  public void testExtendNamedQuery1() throws Exception {
    addDomain("""
                
                class Ddd {
                  String foo;
                  static hasMany = [ddd: Ddd]
                
                  static namedQueries = {
                    xxx { }
                  }
                }
                """);
    configureBySimpleGroovyFile("""
                                  
                                  Ddd.xxx {
                                    <caret>
                                  }
                                  """);

    checkCompletion("eq", "projections", "ddd");
  }

  public void testExtendNamedQuery2() throws Exception {
    addDomain("""
                
                class Ddd {
                  String foo;
                  static hasMany = [ddd: Ddd]
                
                  static namedQueries = {
                    xxx { param ->
                
                    }
                  }
                }
                """);
    configureBySimpleGroovyFile("""
                                  
                                  Ddd.xxx("param") {
                                    <caret>
                                  }
                                  """);

    checkCompletion("eq", "projections", "ddd");
  }

  public void testExtendNamedQuery3() throws Exception {
    addDomain("""
                
                class Ddd {
                  String foo;
                  static hasMany = [ddd: Ddd]
                
                  static namedQueries = {
                    xxx { }
                  }
                }
                """);
    configureBySimpleGroovyFile("""
                                  
                                  Ddd.xxx.count {
                                    <caret>
                                  }
                                  """);

    checkCompletion("eq", "projections", "ddd");
  }

  public void testFetchModeResolve() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String foo;
                                 static hasMany = [ddd: Ddd]
                               
                                 static namedQueries = {
                                   xxx {
                                     fetchMode 'files', org.hibernate.FetchMode.SELECT
                                     lockMode("asdasd", org.hibernate.LockMode.FORCE)
                                     setFirstResult(1)
                                   }
                                 }
                               }
                               """);

    GrailsTestCase.checkResolve(file);
  }
}
