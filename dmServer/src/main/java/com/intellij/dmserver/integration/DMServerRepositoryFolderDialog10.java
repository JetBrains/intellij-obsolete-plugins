package com.intellij.dmserver.integration;

import com.intellij.dmserver.util.DmServerBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DMServerRepositoryFolderDialog10 extends DMServerRepositoryFolderDialog<DMServerRepositoryItem10> {

  public DMServerRepositoryFolderDialog10(DMServerRepositoryEditor parent) {
    super(parent);
    getWatchedRadioButton().setEnabled(false);
    getExternalRadioButton().setEnabled(false);
    getNameTextField().setEnabled(false);
    getWatchedIntervalTextField().setEnabled(false);
  }

  @Override
  protected String getPathText(JTextField component) {
    return getPathWithWildcardsText(component);
  }

  @Override
  protected void setPathText(JTextField component, @NotNull String text) {
    setPathWithWildcardsText(component, text);
  }

  @Override
  @Nullable
  protected DMServerRepositoryItem10 createItem() {
    if (checkEmpty(getPathTextField().getTextField(), DmServerBundle.message("DMServerRepositoryFolderDialog10.warning.empty.path"))) {
      return null;
    }

    DMServerRepositoryItem10 result = new DMServerRepositoryItem10();
    result.setPath(getPathTextField().getText());
    return result;
  }

  @Override
  public void setItem(DMServerRepositoryItem10 item) {
    getPathTextField().setText(item.getPath());
  }
}
