// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrormEmbeddedFieldTest extends GrailsTestCase {
  public void testCompletion() throws Exception {
    addSimpleGroovyFile("""
                          
                          class Zzz {
                            int x,y;
                          }
                          """);

    PsiFile domain = addDomain("""
                                 
                                 class Ddd {
                                   int xxx
                                   String name
                                   Zzz zzz1
                                   Zzz zzz3
                                   Zzz zzzTransient
                                 
                                   Set<Integer> sss;
                                 
                                   static hasMany = [vvv: Integer, sss: Integer];
                                   static transients = ['zzzTransient']
                                 
                                   static embedded = ['zzz3', "<caret>"]
                                 
                                   public Zzz getZzz2() {}
                                 }
                                 """);

    checkCompletionVariants(domain, "zzz1", "zzz2");
  }
}
