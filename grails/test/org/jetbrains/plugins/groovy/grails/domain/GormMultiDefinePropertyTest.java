// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import java.util.List;

public class GormMultiDefinePropertyTest extends Grails14TestCase {
  public void testMultiDefineProperty() {
    addDomain("""
                
                class WcmContent {
                }
                """);

    GroovyFile file = (GroovyFile)addDomain("""
                               
                               class Ddd {
                                 static belongsTo = [foo: WcmContent]
                               //...
                                 static hasOne = [foo: WcmContent]
                               }
                               """);

    PsiClass c = file.getClasses()[0];
    List<PsiField> fields = List.of(c.getFields());
    assertEquals(1, ContainerUtil.count(fields, f -> f.getName().equals("foo")));
  }
}
