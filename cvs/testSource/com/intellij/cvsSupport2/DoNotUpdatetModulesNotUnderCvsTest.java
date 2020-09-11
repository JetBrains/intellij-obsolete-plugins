package com.intellij.cvsSupport2;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.testFramework.PsiTestUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DoNotUpdatetModulesNotUnderCvsTest extends BaseCvs2TestCase {
  private final ArrayList<Module> myModulesToDispose = new ArrayList<>();

  public void test() throws Exception {

    TestObject dir1 = createDirectoryWithFile(TestObject.getProjectDirectory(), 1);
    TestObject dir11 = createDirectoryWithFile(dir1, 11);
    TestObject dir111 = createDirectoryWithFile(dir11, 111);



    final TestObject dir2 = createDirectoryWithFile(TestObject.getProjectDirectory(), 2);
    final TestObject dir21 = createDirectoryWithFile(dir2, 21);
    final TestObject dir211 = createDirectoryWithFile(dir21, 211);

    importProject();

    final Module module = WriteCommandAction.runWriteCommandAction(null,
                                                                   (Computable<Module>)() -> ModuleManager.getInstance(myProject).newModule(new File(dir2, "module2.iml").getAbsolutePath(), StdModuleTypes.JAVA.getId()));

    myModulesToDispose.add(module);

    PsiTestUtil.addContentRoot(module, LocalFileSystem.getInstance().findFileByIoFile(dir2));

    checkoutProject();

    assertTrue(FileUtil.delete(fileIn(dir2)));
    assertTrue(FileUtil.delete(fileIn(dir21)));
    assertTrue(FileUtil.delete(fileIn(dir211)));
    assertTrue(FileUtil.delete(fileIn(dir1)));
    assertTrue(FileUtil.delete(fileIn(dir11)));
    assertTrue(FileUtil.delete(fileIn(dir111)));
    assertTrue(FileUtil.delete(cvsDirIn(dir2)));
    assertTrue(FileUtil.delete(cvsDirIn(dir21)));
    assertTrue(FileUtil.delete(cvsDirIn(dir211)));

    updateProjectDirectory();

    assertTrue(fileIn(dir1).exists());
    assertTrue(fileIn(dir11).exists());
    assertTrue(fileIn(dir111).exists());
    assertFalse(fileIn(dir2).exists());
    assertFalse(fileIn(dir21).exists());
    assertFalse(fileIn(dir211).exists());
    assertFalse(cvsDirIn(dir2).exists());
    assertFalse(cvsDirIn(dir21).exists());
    assertFalse(cvsDirIn(dir211).exists());

    assertTrue(FileUtil.delete(dir21));

    updateProjectDirectory();

    assertFalse(dir21.exists());
    assertFalse(dir211.exists());
  }

  private static TestDirectory cvsDirIn(TestObject dir21) {
    return new TestDirectory(dir21, "CVS");
  }

  private static TestObject createDirectoryWithFile(TestObject parentDirectory, int number) throws IOException {
    TestObject dir1 = new TestDirectory(parentDirectory, "dir" + number);
    dir1.createInProject();
    fileIn(dir1).createInProject();
    return dir1;
  }

  private static TestFile fileIn(TestObject dir1) {
    return new TestFile(dir1, "file.txt");
  }

  @Override
  protected void tearDown() throws Exception {
    final ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    ApplicationManager.getApplication().runWriteAction(() -> {
      for (Module module : myModulesToDispose) {
        String moduleName = module.getName();
        if (moduleManager.findModuleByName(moduleName) != null) {
          moduleManager.disposeModule(module);
        }
      }
    });

    myModulesToDispose.clear();
    super.tearDown();
  }
}
