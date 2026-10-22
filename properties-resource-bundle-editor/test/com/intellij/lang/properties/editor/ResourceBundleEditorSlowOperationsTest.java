// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.properties.editor;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.AsyncFileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.SlowOperations;

/**
 * IDEA-395129: opening the editor must not run slow operations, like loading the stubs of the bundle files, on EDT.
 */
public class ResourceBundleEditorSlowOperationsTest extends BasePlatformTestCase {
  public void testOpeningEditorDoesNotRunSlowOperationsOnEdt() throws Exception {
    // created through VFS only, so that their PSI is not loaded yet and reading the properties has to load the stubs
    VirtualFile defaultFile = myFixture.getTempDirFixture().createFile("p.properties", "a=a_default\nb=b_default\n");
    myFixture.getTempDirFixture().createFile("p_en.properties", "a=a_en\nb=b_en\n");

    FileEditorProvider provider = new ResourceBundleEditorProvider();
    assertTrue(provider.accept(getProject(), defaultFile));

    SlowOperations.reportKnownIssues();
    Ref<FileEditor> editor = new Ref<>();
    PlatformTestUtil.withSystemProperty(SlowOperations.FORBID_SLOW_OPS_PROPERTY, "true", () -> editor.set(createEditor(provider, defaultFile)));
    Disposer.register(getTestRootDisposable(), editor.get());
    assertInstanceOf(editor.get(), ResourceBundleEditor.class);
  }

  /**
   * Creates the editor the way the platform does: {@link AsyncFileEditorProvider} prepares it in a background read action and builds it
   * on EDT, other providers create it on EDT.
   */
  private FileEditor createEditor(FileEditorProvider provider, VirtualFile file) throws Exception {
    if (provider instanceof AsyncFileEditorProvider asyncProvider) {
      AsyncFileEditorProvider.Builder builder = ApplicationManager.getApplication()
        .executeOnPooledThread(() -> ReadAction.compute(() -> asyncProvider.createEditorAsync(getProject(), file)))
        .get();
      try (AccessToken ignored = SlowOperations.startSection(SlowOperations.FORCE_ASSERT)) {
        return builder.build();
      }
    }
    try (AccessToken ignored = SlowOperations.startSection(SlowOperations.FORCE_ASSERT)) {
      return provider.createEditor(getProject(), file);
    }
  }
}
