package com.intellij.gwt.actions;

import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.gwt.GwtMultiFileTestCase;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CreateGwtUiRendererActionTest extends GwtMultiFileTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    TemplateManagerImpl.setTemplateTesting(getTestRootDisposable());
  }

  public void testWithObjectParam() {
    doTest(() -> {});
    doTest(() -> type("Object"));
  }

  public void testWithoutParam() {
    doTest(() -> {
      backspace();
      backspace();
    });
  }

  public void testWithStringParam() {
    doTest(() -> type("String"));
  }

  private void doTest(@NotNull Runnable action) {
    doTest((rootDir, rootAfter) -> WriteCommandAction.runWriteCommandAction(getProject(), (ThrowableComputable<Void, IOException>) () -> {
      VirtualFile pkg = createChildDirectory(rootDir, "pkg");
      CreateGwtUiRendererAction.doCreate0("MyComponent", getPsiManager().findDirectory(pkg));

      setActiveEditor(FileEditorManager.getInstance(getProject()).getSelectedTextEditor());
      action.run();

      LightPlatformCodeInsightTestCase.executeAction(IdeActions.ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE, getEditor(), getProject());

      return null;
    }));
  }

  @NotNull
  @Override
  protected String getTestRoot() {
    return "/actions/createUiRenderer/";
  }
}