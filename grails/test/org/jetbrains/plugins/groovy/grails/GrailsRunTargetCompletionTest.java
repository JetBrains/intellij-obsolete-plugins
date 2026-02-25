// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.TestCase;
import org.jetbrains.plugins.grails.commands.GrailsCommandCompletionUtil;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;

import java.util.Collection;
import java.util.List;

public class GrailsRunTargetCompletionTest extends GrailsTestCase {
  private List<String> complete(String s) {
    String prefix = s.substring(s.lastIndexOf(" ") + 1);
    GrailsApplication application = GrailsApplicationManager.getInstance(getProject()).getApplications().iterator().next();
    Collection<LookupElement> variants = GrailsCommandCompletionUtil.collectVariants(application, s, s.length(), prefix);
    return ContainerUtil.map(variants, LookupElement::getLookupString);
  }

  public void testCompletionSimple() {
    myFixture.addFileToProject("scripts/ProjectScript.groovy", "");
    List<String> variants = complete("");

    TestCase.assertTrue(variants.containsAll(List.of("project-script", "create-domain-class", "add-proxy")));
    TestCase.assertFalse(variants.contains("-Dgrails.home"));
  }

  public void testCompleteSystemProperties() {
    List<String> variants = complete("-D");

    TestCase.assertTrue(variants.contains("-Divy.default.ivy.user.dir"));
    TestCase.assertFalse(variants.contains("create-domain-class"));
  }

  public void testCompletionAfterEnv() {
    List<String> variants = complete("dev ");

    TestCase.assertTrue(variants.containsAll(List.of("create-domain-class", "add-proxy")));
  }

  public void testCompletionPackageNames() {
    addDomain("""
                package com.xxx.domains;
                class Ddd {
                }
                """);
    addController("""
                    package com.xxx.controllers;
                    class CccController {
                    }
                    """);

    List<String> variants = complete("dev create-controller ");

    TestCase.assertTrue(variants.containsAll(List.of("com.xxx.domains", "com.xxx.controllers")));
    TestCase.assertFalse(variants.contains("com"));
    TestCase.assertFalse(variants.contains("com.xxx"));
    TestCase.assertFalse(variants.contains("com.xxx.domains.Ddd"));
  }

  public void testCompletionClassNames() {
    addDomain("""
                package com.xxx.domains;
                class Ddd {
                }
                """);

    List<String> variants = complete("dev create-controller com.xxx.domains.");

    TestCase.assertTrue(variants.contains("com.xxx.domains.Ddd"));
  }

  public void testIgnoringGrailsPrefix() {
    List<String> variants = complete("grails ");
    TestCase.assertTrue(variants.containsAll(List.of("clean", "create-domain-class", "add-proxy")));
  }

  @Override
  protected boolean needScripts() {
    return true;
  }
}
