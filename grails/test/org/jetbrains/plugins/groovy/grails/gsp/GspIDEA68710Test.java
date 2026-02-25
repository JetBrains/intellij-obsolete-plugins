// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class GspIDEA68710Test extends LightJavaCodeInsightFixtureTestCase {
  public void testIDEA68710_1() {
    myFixture.configureByText("a.gsp", "${this.hashCode()}");
    myFixture.checkHighlighting(true, false, true);
  }

  public void testIDEA68710_2() {
    myFixture.configureByText("a.gsp", "<% this.hashCode() %>");
    myFixture.checkHighlighting(true, false, true);
  }
}
