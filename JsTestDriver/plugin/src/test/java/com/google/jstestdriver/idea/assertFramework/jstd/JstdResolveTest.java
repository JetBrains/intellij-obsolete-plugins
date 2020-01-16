package com.google.jstestdriver.idea.assertFramework.jstd;

import com.google.jstestdriver.idea.assertFramework.jstd.jsSrc.JstdDefaultAssertionFrameworkSrcMarker;
import com.google.jstestdriver.idea.assertFramework.library.JstdLibraryUtil;
import com.google.jstestdriver.idea.util.VfsUtils;
import com.intellij.lang.javascript.library.JSLibraryManager;
import com.intellij.lang.javascript.library.JSLibraryMappings;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase;
import com.intellij.util.ArrayUtil;
import com.intellij.webcore.libraries.ScriptingLibraryManager;
import com.intellij.webcore.libraries.ScriptingLibraryModel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class JstdResolveTest extends CodeInsightFixtureTestCase {

  private static final boolean ADD_LIBRARY = true;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    if (ADD_LIBRARY) {
      VfsRootAccess.allowRootAccess(getTestRootDisposable(),
              PathManager.getResourceRoot(JstdDefaultAssertionFrameworkSrcMarker.class, "/com/google/jstestdriver/idea/assertFramework/jstd/jsSrc/Asserts.js"));
      Collection<VirtualFile> jstdLibSourceFiles = VfsUtils.findVirtualFilesByResourceNames(
        JstdDefaultAssertionFrameworkSrcMarker.class,
        new String[]{"Asserts.js", "TestCase.js"}
      );
      addJstdLibrary(getProject(), jstdLibSourceFiles);
    }
  }

  @Override
  public void tearDown() throws Exception {
    try {
      if (ADD_LIBRARY) {
        removeLibrary(getProject());
      }
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  private static void removeLibrary(@NotNull final Project project) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      ScriptingLibraryManager libraryManager = ServiceManager.getService(project, JSLibraryManager.class);
      ScriptingLibraryModel model = libraryManager.getLibraryByName(JstdLibraryUtil.LIBRARY_NAME);
      assert model != null;
      libraryManager.removeLibrary(model);
      libraryManager.commitChanges();
    });
  }

  private static void addJstdLibrary(@NotNull final Project project,
                                     @NotNull final Collection<VirtualFile> libSourceFiles) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      JSLibraryManager jsLibraryManager = ServiceManager.getService(project, JSLibraryManager.class);
      ScriptingLibraryModel libraryModel = jsLibraryManager.createLibrary(
        JstdLibraryUtil.LIBRARY_NAME,
        VfsUtilCore.toVirtualFileArray(libSourceFiles),
        VirtualFile.EMPTY_ARRAY,
        ArrayUtil.EMPTY_STRING_ARRAY,
        ScriptingLibraryModel.LibraryLevel.GLOBAL,
        false
      );
      JSLibraryMappings jsLibraryMappings = ServiceManager.getService(project, JSLibraryMappings.class);
      jsLibraryMappings.associate(null, libraryModel.getName());
      jsLibraryManager.commitChanges();
    });
  }

  public void testResolveTestCaseFunction() {
    String fileText = "Test<caret>Case('', {});";
    myFixture.configureByText("sample.js", fileText);
    final PsiElement resolved = myFixture.getElementAtCaret();
    assertTrue(resolved instanceof JSFunction);
  }
}
