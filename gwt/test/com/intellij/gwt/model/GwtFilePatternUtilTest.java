package com.intellij.gwt.model;

import junit.framework.TestCase;

import static com.intellij.gwt.module.model.impl.GwtFilePatternUtil.isExcludedByDefault;
import static com.intellij.util.containers.ContainerUtil.ar;

public class GwtFilePatternUtilTest extends TestCase {

  public void testCaseSensitiveFileNames() {
    boolean caseSensitive = true;

    assertTrue(isExcludedByDefault("CVS", caseSensitive));
    assertTrue(isExcludedByDefault(".cvsignore", caseSensitive));
    assertTrue(isExcludedByDefault("SCCS", caseSensitive));
    assertTrue(isExcludedByDefault("vssver.scc", caseSensitive));
    assertTrue(isExcludedByDefault(".svn", caseSensitive));
    assertTrue(isExcludedByDefault(".DS_Store", caseSensitive));
    assertTrue(isExcludedByDefault(".git", caseSensitive));
    assertTrue(isExcludedByDefault(".gitattributes", caseSensitive));
    assertTrue(isExcludedByDefault(".gitignore", caseSensitive));
    assertTrue(isExcludedByDefault(".gitmodules", caseSensitive));
    assertTrue(isExcludedByDefault(".hg", caseSensitive));
    assertTrue(isExcludedByDefault(".hgignore", caseSensitive));
    assertTrue(isExcludedByDefault(".hgsub", caseSensitive));
    assertTrue(isExcludedByDefault(".hgsubstate", caseSensitive));
    assertTrue(isExcludedByDefault(".hgtags", caseSensitive));
    assertTrue(isExcludedByDefault(".bzr", caseSensitive));
    assertTrue(isExcludedByDefault(".bzrignore", caseSensitive));

    assertTrue(isExcludedByDefault("folder/CVS", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.cvsignore", caseSensitive));
    assertTrue(isExcludedByDefault("folder/SCCS", caseSensitive));
    assertTrue(isExcludedByDefault("folder/vssver.scc", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.svn", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.DS_Store", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.git", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.gitattributes", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.gitignore", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.gitmodules", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.hg", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.hgignore", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.hgsub", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.hgsubstate", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.hgtags", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.bzr", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.bzrignore", caseSensitive));

    assertFalse(isExcludedByDefault("cvs", caseSensitive));
  }

  public void testCaseInsensitiveFileNames() {
    boolean caseSensitive = false;

    assertTrue(isExcludedByDefault("cvs", caseSensitive));
    assertTrue(isExcludedByDefault(".cvsignore", caseSensitive));
    assertTrue(isExcludedByDefault("sccs", caseSensitive));
    assertTrue(isExcludedByDefault("vssver.scc", caseSensitive));
    assertTrue(isExcludedByDefault(".svn", caseSensitive));
    assertTrue(isExcludedByDefault(".ds_store", caseSensitive));
    assertTrue(isExcludedByDefault(".git", caseSensitive));
    assertTrue(isExcludedByDefault(".gitattributes", caseSensitive));
    assertTrue(isExcludedByDefault(".gitignore", caseSensitive));
    assertTrue(isExcludedByDefault(".gitmodules", caseSensitive));
    assertTrue(isExcludedByDefault(".hg", caseSensitive));
    assertTrue(isExcludedByDefault(".hgignore", caseSensitive));
    assertTrue(isExcludedByDefault(".hgsub", caseSensitive));
    assertTrue(isExcludedByDefault(".hgsubstate", caseSensitive));
    assertTrue(isExcludedByDefault(".hgtags", caseSensitive));
    assertTrue(isExcludedByDefault(".bzr", caseSensitive));
    assertTrue(isExcludedByDefault(".bzrignore", caseSensitive));

    assertTrue(isExcludedByDefault("folder/cvs", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.cvsignore", caseSensitive));
    assertTrue(isExcludedByDefault("folder/sccs", caseSensitive));
    assertTrue(isExcludedByDefault("folder/vssver.scc", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.svn", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.ds_store", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.git", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.gitattributes", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.gitignore", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.gitmodules", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.hg", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.hgignore", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.hgsub", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.hgsubstate", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.hgtags", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.bzr", caseSensitive));
    assertTrue(isExcludedByDefault("folder/.bzrignore", caseSensitive));
  }

  public void testCaseSensitiveFolderNames() {
    boolean caseSensitive = true;
    assertTrue(isExcludedByDefault("CVS/file", caseSensitive));
    assertTrue(isExcludedByDefault("SCCS/file", caseSensitive));
    assertTrue(isExcludedByDefault(".svn/file", caseSensitive));
    assertTrue(isExcludedByDefault(".git/file", caseSensitive));
    assertTrue(isExcludedByDefault(".hg/file", caseSensitive));
    assertTrue(isExcludedByDefault(".bzr/file", caseSensitive));

    assertFalse(isExcludedByDefault("cvs/file", caseSensitive));
  }

  public void testCaseInsensitiveFolderNames() {
    boolean caseSensitive = false;
    assertTrue(isExcludedByDefault("cvs/file", caseSensitive));
    assertTrue(isExcludedByDefault("sccs/file", caseSensitive));
    assertTrue(isExcludedByDefault(".svn/file", caseSensitive));
    assertTrue(isExcludedByDefault(".git/file", caseSensitive));
    assertTrue(isExcludedByDefault(".hg/file", caseSensitive));
    assertTrue(isExcludedByDefault(".bzr/file", caseSensitive));
  }

  public void testSpecialFileNames() {
    for (boolean caseSensitive : ar(true, false)) {
      assertTrue(isExcludedByDefault(".#name", caseSensitive));
      assertTrue(isExcludedByDefault("._name", caseSensitive));
      assertTrue(isExcludedByDefault("name~", caseSensitive));
      assertTrue(isExcludedByDefault("#name#", caseSensitive));
      assertTrue(isExcludedByDefault("%name%", caseSensitive));

      assertFalse(isExcludedByDefault(".%name", caseSensitive));
      assertFalse(isExcludedByDefault("~name", caseSensitive));
      assertFalse(isExcludedByDefault("_name_", caseSensitive));
    }
  }
}