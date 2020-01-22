package com.jetbrains.plugins.compass.ruby;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.plugins.compass.CompassSettings;
import com.jetbrains.plugins.compass.CompassUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CompassImportPathRegistrationWatcherRMTest extends SassExtensionsBaseTest {
  private VirtualFile myConfigFile;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myConfigFile = VfsUtil.findFileByIoFile(FileUtil.createTempFile("compassConfig", ".rb"), true);
    assertNotNull(myConfigFile);
    CompassSettings.getInstance(myFixture.getModule()).setCompassConfigPath(myConfigFile.getCanonicalPath());
    CompassUtil.getCompassExtension().stopActivity(myFixture.getModule());
    CompassUtil.getCompassExtension().startActivity(myFixture.getModule());
    UIUtil.dispatchAllInvocationEvents();
  }

  @Override
  public void tearDown() throws Exception {
    try {
      deleteConfigFile();
      CompassSettings.getInstance(myFixture.getModule()).setCompassConfigPath("");
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  public void testAddLibraryForEachAddImportPathCall() throws Exception {
    final VirtualFile importPath1 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath1", ""), true);
    final VirtualFile importPath2 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath2", ""), true);
    assertNotNull(importPath1);
    assertNotNull(importPath2);

    assertSettingsAndLibraryRoots();
    updateConfigFile("add_import_path '" + importPath1.getPath() + "'\n" +
                     "add_import_path '" + importPath2.getPath() + "'\n");
    assertSettingsAndLibraryRoots(importPath1, importPath2);
  }

  @Test
  public void testRemoveImportPathsAfterConfigRemove() throws Exception {
    final VirtualFile importPath = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath", ""), true);
    assertNotNull(importPath);

    assertSettingsAndLibraryRoots();
    updateConfigFile("add_import_path '" + importPath.getPath() + "'");
    assertSettingsAndLibraryRoots(importPath);

    deleteConfigFile();
    assertSettingsAndLibraryRoots();
  }

  @Test
  public void testAddLibraryForLastAdditionalImportPathsAssignment() throws Exception {
    final VirtualFile importPath1 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath1", ""), true);
    final VirtualFile importPath2 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath2", ""), true);
    assertNotNull(importPath1);
    assertNotNull(importPath2);

    assertSettingsAndLibraryRoots();
    updateConfigFile("additional_import_paths = ['" + importPath1.getPath() + "']\n" +
                     "additional_import_paths = ['" + importPath2.getPath() + "']\n");
    assertSettingsAndLibraryRoots(importPath2);
  }

  @Test
  public void testCombined() throws Exception {
    final VirtualFile importPath1 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath1", ""), true);
    final VirtualFile importPath2 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath2", ""), true);
    assertNotNull(importPath1);
    assertNotNull(importPath2);

    assertSettingsAndLibraryRoots();
    updateConfigFile("additional_import_paths = ['" + importPath1.getPath() + "']\n" +
                     "add_import_path '" + importPath2.getPath() + "'\n");
    assertSettingsAndLibraryRoots(importPath1, importPath2);
  }

  @Test
  public void testRenameConfigFile() throws Exception {
    String oldName = myConfigFile.getName();
    final VirtualFile importPath = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath", ""), true);
    assertNotNull(importPath);

    updateConfigFile("add_import_path '" + importPath.getPath() + "'\n");
    assertSettingsAndLibraryRoots(importPath);

    renameConfigFile("config_inappropriate_name.rb");
    assertSettingsAndLibraryRoots();

    renameConfigFile(oldName);
    assertSettingsAndLibraryRoots(importPath);
  }

  private void assertSettingsAndLibraryRoots(VirtualFile... expectedLibraryRoots) {
    UIUtil.dispatchAllInvocationEvents();
    final String[] expectedImportPaths = ArrayUtil.prepend(getCompassStylesheetPath(),
                                                           ContainerUtil.map(expectedLibraryRoots, VirtualFile::getPath,
                                                                             ArrayUtilRt.EMPTY_STRING_ARRAY)
    );
    assertSameElements(CompassSettings.getInstance(myFixture.getModule()).getImportPaths(), expectedImportPaths);

    final ModifiableModelsProvider modelsProvider = ModifiableModelsProvider.SERVICE.getInstance();
    final ModifiableRootModel model = modelsProvider.getModuleModifiableModel(myFixture.getModule());
    try {
      final LibraryOrderEntry compassLibraryEntry = OrderEntryUtil.findLibraryOrderEntry(model, CompassUtil.COMPASS_LIBRARY_NAME);
      if (expectedLibraryRoots.length == 0) {
        if (compassLibraryEntry != null) {
          assertEquals(0, compassLibraryEntry.getRootFiles(OrderRootType.CLASSES).length);
        }
      }
      else {
        assertNotNull(compassLibraryEntry);
        final List<VirtualFile> rootFiles = Arrays.asList(compassLibraryEntry.getRootFiles(OrderRootType.CLASSES));
        assertSameElements(rootFiles, Arrays.asList(expectedLibraryRoots));
      }
    }
    finally {
      modelsProvider.disposeModuleModifiableModel(model);
    }
  }

  private String getCompassStylesheetPath() {
    return COMPASS_STYLESHEET_PATH;
  }

  private void renameConfigFile(final String newName) {
    WriteCommandAction.runWriteCommandAction(getProject(), new Runnable() {
      @Override
      public void run() {
        try {
          myConfigFile.rename(this, newName);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  private void deleteConfigFile() {
    WriteCommandAction.runWriteCommandAction(myFixture.getProject(), () -> {
      try {
        if (myConfigFile != null) {
          myConfigFile.delete(null);
          myConfigFile = null;
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void updateConfigFile(@NotNull String newText) {
    myFixture.saveText(myConfigFile, newText);
    PsiDocumentManager.getInstance(myFixture.getProject()).commitAllDocuments();
  }
}

