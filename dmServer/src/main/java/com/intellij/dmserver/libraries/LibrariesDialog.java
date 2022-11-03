package com.intellij.dmserver.libraries;

import com.intellij.CommonBundle;
import com.intellij.dmserver.editor.AvailableBundlesProvider;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LibrariesDialog extends DialogWrapper {

  private final ServerLibrariesUI myUI;
  private final Project myProject;

  public LibrariesDialog(ServerLibrariesContext context) {
    super(context.getProject(), true);
    myUI = new ServerLibrariesUI(context);
    myProject = context.getProject();
    setTitle(DmServerBundle.message("LibrariesDialog.title"));
    setOKButtonText(CommonBundle.getCloseButtonText());
    init();
  }

  @Override
  public void show() {
    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
      () -> AvailableBundlesProvider.getInstance(myProject).resetRepositoryIndex(), DmServerBundle.message("LibrariesDialog.progress.updating.index"), true, myProject)) {
      return;
    }
    super.show();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myUI;
  }

  @Override
  protected Action @NotNull [] createActions() {
    return new Action[]{getOKAction()};
  }

  public void initSearch(String packageName) {
    myUI.initSearch(packageName);
  }

  @Override
  protected void dispose() {
    super.dispose();
    Disposer.dispose(myUI);
  }

  @Nullable
  @NonNls
  @Override
  protected String getDimensionServiceKey() {
    return getClass().getName();
  }
}
