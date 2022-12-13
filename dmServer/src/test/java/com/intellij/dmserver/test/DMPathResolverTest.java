package com.intellij.dmserver.test;

import com.intellij.dmserver.integration.PathResolver;
import com.intellij.openapi.vfs.VirtualFile;

public class DMPathResolverTest extends DMTestBase {

  public void testInnerPath2Relative() {
    final String CHILD_PATH = "sub/file.jar";

    final VirtualFile root = getTempDir().createVirtualDir();

    VirtualFile sub = createChildDirectory(root, "sub");
    VirtualFile innerFile = createChildData(sub, "file.jar");

    PathResolver pathResolver = createPathResolver(root);

    String path = innerFile.getPath();
    assertEquals(CHILD_PATH, pathResolver.path2Relative(path));
  }

  public void testChildPath2Absolute() {
    final String CHILD_PATH = "sub/file.jar";

    final VirtualFile root = getTempDir().createVirtualDir();

    PathResolver pathResolver = createPathResolver(root);

    String expectedPath = root.getPath() + "/" + CHILD_PATH;
    assertEquals(expectedPath, pathResolver.path2Absolute(CHILD_PATH));
  }

  public void testOuterPathPersists() {
    final VirtualFile root = getTempDir().createVirtualDir();

    VirtualFile dir = getTempDir().createVirtualDir();
    VirtualFile outerFile = createChildData(dir, "file.jar");

    PathResolver pathResolver = createPathResolver(root);

    String path = outerFile.getPath();
    assertEquals(path, pathResolver.path2Relative(path));
    assertEquals(path, pathResolver.path2Absolute(path));
  }

  private PathResolver createPathResolver(final VirtualFile root) {
    return new PathResolver() {

      @Override
      protected VirtualFile getBaseDir() {
        return root;
      }
    };
  }
}
