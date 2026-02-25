// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.completion;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.util.List;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class TaglibClassMethodsCompletionTest extends GrailsTestCase {
  public void testTaglibCompletion() {
    configureByTaglib("""
                        class MyTagLib {
                          def customTag = {
                            re<caret>
                          }
                        }
                        """);

    myFixture.completeBasic();
    assertTrue(myFixture.getLookupElementStrings().containsAll(
      List.of("remoteField", "remoteFunction", "remoteLink", "render", "renderErrors", "request", "resolveStrategy", "resource",
              "response", "return")));
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/oldCompletion/taglib/");
  }

}
