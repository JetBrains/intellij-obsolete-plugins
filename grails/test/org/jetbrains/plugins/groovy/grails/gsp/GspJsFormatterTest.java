// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.lang.GroovyFormatterTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GspJsFormatterTest extends GroovyFormatterTestCase {

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/formatter/js/");
  }

  private void doTest() throws IOException {
    String jsCode = String.join("\n", Files.readAllLines(Paths.get(getTestDataPath(), getTestName(true) + ".test")));

    PsiFile a = myFixture.addFileToProject("a.gsp", "<g:javascript>\n" + jsCode + "\n</g:javascript>");
    PsiFile b = myFixture.addFileToProject("b.gsp", "<script>\n" + jsCode + "\n</script>");

    doFormat(a);
    doFormat(b);

    String textA = a.getText().replaceAll("</?g:javascript>", "");
    String textB = b.getText().replaceAll("</?script>", "");

    TestCase.assertEquals(textB, textA);
  }

  public void testT1() throws IOException { doTest(); }

  public void testT2() throws IOException { doTest(); }
}
