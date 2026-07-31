package com.intellij.gwt.model;

import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;

@SuppressWarnings({"ConstantConditions"})
public class GwtSourceTagTest extends GwtTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    try {
      WriteAction.computeAndWait(() -> addGwtModule("model/sourcesTagTestModule"));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void testIncludeTag() {
    final GwtModule module = findGwtModule("ppp.IncludeTag");
    assertInSource("a/r/b.xml", module);
    assertNotInSource("a/r/c.java", module);
    assertInSource("abcxxx/q.xml", module);
  }

  public void testIncludeAndExcludeTags() {
    final GwtModule module = findGwtModule("ppp.IncludeAndExcludeTags");
    assertInSource("a/r/b.xml", module);
    assertNotInSource("a/r/c.java", module);
    assertNotInSource("abcxxx/q.xml", module);
  }

  public void testExcludeTag() {
    final GwtModule module = findGwtModule("ppp.ExcludeTag");
    assertNotInSource("a/r/b.xml", module);
    assertInSource("a/r/c.java", module);
    assertNotInSource("abcxxx/q.xml", module);
  }

  public void testIncludesAttribute() {
    final GwtModule module = findGwtModule("ppp.IncludesAttribute");
    assertInSource("a/r/b.xml", module);
    assertNotInSource("a/r/c.java", module);
    assertNotInSource("abcxxx/q.xml", module);
  }

  public void testExcludesAttribute() {
    final GwtModule module = findGwtModule("ppp.ExcludesAttribute");
    assertNotInSource("a/r/b.xml", module);
    assertNotInSource("a/r/c.java", module);
    assertInSource("abcxxx/q.xml", module);
  }

  public void testIncludeTagAndAttribute() {
    final GwtModule module = findGwtModule("ppp.IncludeTagAndAttribute");
    assertInSource("a/r/b.xml", module);
    assertNotInSource("a/r/c.java", module);
    assertInSource("abcxxx/q.xml", module);
  }

  public void testEmpty() {
    final GwtModule module = findGwtModule("ppp.Empty");
    assertInSource("a/r/b.xml", module);
    assertInSource("a/r/c.java", module);
    assertInSource("abcxxx/q.xml", module);
  }

  public void testCaseInsensitiveIncludeTag() {
    final GwtModule module = findGwtModule("ppp.CaseInsensitiveIncludeTag");
    assertNotInSource("a/r/b.xml", module);
    assertInSource("a/r/c.java", module);
    assertNotInSource("abcxxx/q.xml", module);
  }


  private static void assertNotInSource(final String path, GwtModule module) {
    final VirtualFile directory = module.getModuleDirectory().findChild("client");
    assertFalse(module.isSourceFile(directory.findFileByRelativePath(path)));
  }

  private static void assertInSource(final String path, GwtModule module) {
    final VirtualFile directory = module.getModuleDirectory().findChild("client");
    assertTrue(module.isSourceFile(directory.findFileByRelativePath(path)));
  }

}
