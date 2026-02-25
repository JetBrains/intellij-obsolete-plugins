// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.codeInsight.daemon.impl.analysis.HtmlUnknownTargetInspection;
import com.intellij.codeInsight.daemon.impl.analysis.XmlPathReferenceInspection;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspCreateTemplateQuickFixTest extends GrailsTestCase {
  public void testCreateForControllerRender() {
    addController("""
                    class CccController {
                    }
                    """);

    PsiFile file = addView("ccc/a.gsp", "<g:render template='ttt<caret>'");
    runIntention(file, "Create template", true);
    TestCase.assertNotNull(file.getVirtualFile().getParent().findChild("_ttt.gsp"));
  }

  public void testCreateByAbsoluteUriRender() {
    addController("""
                    class CccController {
                    }
                    """);

    PsiFile file = addView("ccc/a.gsp", "<g:render template='/ttt<caret>'");
    runIntention(file, "Create template", true);
    TestCase.assertNotNull(file.getVirtualFile().getParent().getParent().findChild("_ttt.gsp"));
  }

  public void testCreateForControllerTmpl() {
    addController("""
                    class CccController {
                    }
                    """);

    PsiFile file = addView("ccc/a.gsp", "<tmpl:ttt<caret> />");
    runIntention(file, "Create template", true);
    TestCase.assertNotNull(file.getVirtualFile().getParent().findChild("_ttt.gsp"));
  }

  public void testCreateByAbsoluteUriTmpl() {
    addController("""
                    class CccController {
                    }
                    """);

    PsiFile file = addView("ccc/a.gsp", "<tmpl:/ttt<caret> />");
    runIntention(file, "Create template", true);
    TestCase.assertNotNull(file.getVirtualFile().getParent().getParent().findChild("_ttt.gsp"));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new XmlPathReferenceInspection(), new HtmlUnknownTargetInspection());
  }
}
