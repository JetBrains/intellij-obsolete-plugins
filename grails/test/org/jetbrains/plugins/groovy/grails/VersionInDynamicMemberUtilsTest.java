// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

public class VersionInDynamicMemberUtilsTest extends GrailsTestCase {

  public void testCompletion() {
    configureByDomain(
      """
        class Ddd {
          String name;
          static {
            new Ddd().g<caret>
          }
        }
        """);

    if (useGrails14()) {
      checkCompletion("getDomainClass");
    }
    else {
      checkCompletion("getName");
      checkNonExistingCompletionVariants("getDomainClass");
    }
  }
}
