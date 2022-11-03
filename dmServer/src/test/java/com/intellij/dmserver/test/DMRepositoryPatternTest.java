package com.intellij.dmserver.test;

import com.intellij.dmserver.integration.DMServerRepositoryItem;
import com.intellij.dmserver.integration.PathResolver;
import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.HashSet;
import java.util.Set;

public class DMRepositoryPatternTest extends DMTestBase {

  public void testAnyFilePattern() {
    doTestAnyFilePattern("*");
    doTestAnyFilePattern("{artifact}");
  }

  public void testJarFilePattern() {
    VirtualFile root = getTempDir().createVirtualDir();

    VirtualFile bundle = createChildData(root, "bundle.jar");
    VirtualFile library = createChildData(root, "library.libd");

    String searchPattern = root.getPath() + "/" + "*.jar";

    RepositoryPattern pattern = createPattern(searchPattern);

    assertEquals(root, pattern.findBaseDir());
    assertEquals(false, pattern.hasDirPatterns());
    Set<VirtualFile> files = new HashSet<>(pattern.collectFiles());
    assertEquals(1, files.size());
    assertTrue(files.contains(bundle));
  }

  public void testSubFoldersPattern() {
    VirtualFile root = getTempDir().createVirtualDir();

    VirtualFile d1 = createChildDirectory(root, "sub1");
    VirtualFile bundle1 = createChildData(d1, "bundle1.jar");
    VirtualFile d2 = createChildDirectory(root, "sub2");
    VirtualFile bundle2 = createChildData(d2, "bundle2.jar");
    VirtualFile bundle2ignore = createChildData(root, "bundle2ignore.jar");

    String searchPattern = root.getPath() + "/" + "{sub}/{artifact}";

    RepositoryPattern pattern = createPattern(searchPattern);

    assertEquals(root, pattern.findBaseDir());
    assertEquals(true, pattern.hasDirPatterns());
    Set<VirtualFile> files = new HashSet<>(pattern.collectFiles());
    assertEquals(2, files.size());
    assertTrue(files.contains(bundle1));
    assertTrue(files.contains(bundle2));
  }

  public void testRecursiveFoldersPattern() {
    VirtualFile root = getTempDir().createVirtualDir();

    VirtualFile bundle1 = createChildData(root, "bundle1.jar");
    VirtualFile subFolder1 = createChildDirectory(root, "sub1");
    VirtualFile bundle2 = createChildData(subFolder1, "bundle2.jar");
    VirtualFile subFolder2 = createChildDirectory(root, "sub2");
    VirtualFile bundle3 = createChildData(subFolder2, "bundle3.jar");

    String searchPattern = root.getPath() + "/" + "**/*";

    RepositoryPattern pattern = createPattern(searchPattern);

    assertEquals(root, pattern.findBaseDir());
    assertEquals(true, pattern.hasDirPatterns());
    Set<VirtualFile> files = new HashSet<>(pattern.collectFiles());
    assertEquals(3, files.size());
    assertTrue(files.contains(bundle1));
    assertTrue(files.contains(bundle2));
    assertTrue(files.contains(bundle3));
  }

  private void doTestAnyFilePattern(String anyFilePattern) {
    VirtualFile root = getTempDir().createVirtualDir();

    VirtualFile bundle = createChildData(root, "bundle.jar");
    VirtualFile library = createChildData(root, "library.libd");
    VirtualFile folder2ignore = createChildDirectory(root, "sub");

    String searchPattern = root.getPath() + "/" + anyFilePattern;

    RepositoryPattern pattern = createPattern(searchPattern);

    assertEquals(root, pattern.findBaseDir());
    assertEquals(false, pattern.hasDirPatterns());
    Set<VirtualFile> files = new HashSet<>(pattern.collectFiles());
    assertEquals(2, files.size());
    assertTrue(files.contains(bundle));
    assertTrue(files.contains(library));
  }

  private RepositoryPattern createPattern(String searchPattern) {
    MockDMServerRepositoryItem repositoryItem = new MockDMServerRepositoryItem();

    RepositoryPattern pattern = RepositoryPattern.create(repositoryItem, searchPattern);

    assertEquals(repositoryItem, pattern.getSource());

    return pattern;
  }

  private static class MockDMServerRepositoryItem implements DMServerRepositoryItem {

    @Override
    public String getPath() {
      return null;
    }

    @Override
    public void setPath(String path) {
    }

    @Override
    public RepositoryPattern createPattern(PathResolver pathResolver) {
      return null;
    }
  }
}
