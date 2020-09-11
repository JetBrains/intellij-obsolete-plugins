package com.intellij.cvsSupport2;


import com.intellij.openapi.util.io.FileUtil;

public class EntriesStaticTest extends BaseCvs2TestCase {
  public void test() throws Exception {
    TestObject dir1 = new TestDirectory(TestObject.getProjectDirectory(), "dir1");
    dir1.createInProject();
    TestObject file1 = new TestFile(dir1, "file.txt");
    file1.createInProject();

    TestObject dir2 = new TestDirectory(TestObject.getProjectDirectory(), "dir2");
    dir2.createInProject();
    TestObject file2 = new TestFile(dir2, "file.txt");
    file2.createInProject();

    importProject();

    assertTrue(FileUtil.delete(TestObject.getProjectDirectory()));


    TestObject.getProjectDirectory().createInProject();

    checkoutModuleTo(TestObject.getProjectDirectory().getParentFile(), "VcsTestProject/dir1");

    assertTrue(dir1.isDirectory());
    assertTrue(file1.isFile());
    assertFalse(dir2.isDirectory());
    assertFalse(file2.isFile());

    boolean oldValue = myConfiguration.CREATE_NEW_DIRECTORIES;
    myConfiguration.CREATE_NEW_DIRECTORIES = false;

    try {
      updateProjectDirectory();
    }
    finally {
      myConfiguration.CREATE_NEW_DIRECTORIES = oldValue;
    }

    assertTrue(dir1.isDirectory());
    assertTrue(file1.isFile());
    assertFalse(dir2.isDirectory());
    assertFalse(file2.isFile());
  }

}
