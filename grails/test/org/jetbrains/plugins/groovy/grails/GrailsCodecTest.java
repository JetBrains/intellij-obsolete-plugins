// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.psi.PsiFile;
import com.intellij.usageView.UsageInfo;

import java.util.Collection;

public class GrailsCodecTest extends GrailsTestCase {
  public void testHighlighting() {
    myFixture.addFileToProject("grails-app/utils/ccc/MmmCodec.groovy", """
      
      package ccc
      
      import javax.swing.JButton
      import javax.swing.JPanel
      
      class MmmCodec {
      
       static encode = { str ->
        return new JButton();
       }
      
       public static decode(Object target) {
           if (target != null) {
               return new JPanel();
           }
           return null;
       }
      
      }
      """);

    myFixture.addFileToProject("grails-app/controllers/Foo.java", "public class Foo { static byte[] aaa; }");

    PsiFile file = addController("""
                                   
                                   class CccController {
                                   
                                    def index = {
                                     "a".encodeAsMmm().getBackground();
                                     [1,2,3].encodeAsMmm().getBackground();
                                     "a".decodeMmm().getUI();
                                     [1,2,3].decodeMmm().getUI();
                                   
                                     "a".encodeAsHTML().substring(1)
                                     [1,2,3].encodeAsHTML().substring(1)
                                     "a".encodeAsMD5Bytes().length
                                     [1,2,3].decodeMD5Bytes()
                                   
                                     Foo.aaa.encodeAsHTML().substring(1)
                                   
                                     CccController.encodeAsMD5()
                                    }
                                   
                                   }
                                   """);
    GrailsTestCase.checkResolve(file, "decodeMD5Bytes", "encodeAsMD5");
  }

  public void testDontEncodeStaticContextCompletion() {
    configureByController("""
                            
                            class CccController {
                              def index = {
                                CccController.<caret>
                              }
                            }
                            """);

    checkCompletion("log");
    checkNonExistingCompletionVariants("encodeAsMD5", "decodeMD5Bytes", "encodeAsHTML");
  }

  public void testDontEncodeNamespacePrefix() {
    PsiFile c = addController("""
                                
                                class CccController {
                                  def index = {
                                    g.encodeAsHtml()
                                    g.link().encodeAsMD5Bytes()
                                  }
                                }
                                """);

    GrailsTestCase.checkResolve(c, "encodeAsHtml");
  }

  public void testFindUsages() {
    addController("""
                    
                    class Ccc1Controller {
                      def index = {
                       render("aaa".encodeAsHTML())
                      }
                    }
                    """);
    PsiFile c = addController("""
                                
                                class Ccc2Controller {
                                  def index = {
                                   render("bbb".encodeAsHTML<caret>())
                                  }
                                }
                                """);

    Collection<UsageInfo> usages = myFixture.testFindUsages(getFilePath(c));
    assert usages.size() == 2;
  }
}
