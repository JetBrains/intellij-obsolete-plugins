package com.intellij.dmserver.integration;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.UiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class DMServerRepositoryFolderDialog<T extends DMServerRepositoryItem> extends DialogWrapper {

  @NonNls
  protected static final String DEFAULT_WILDCARDS = "{artifact}";

  private JPanel myMainPanel;
  private JRadioButton myWatchedRadioButton;
  private JRadioButton myExternalRadioButton;
  private TextFieldWithBrowseButton myPathTextField;

  private JTextField myNameTextField;
  private JTextField myWatchedIntervalTextField;

  private T myItem;

  private final DMServerRepositoryEditor myParent;

  public DMServerRepositoryFolderDialog(DMServerRepositoryEditor parent) {
    super((Project)null);
    myParent = parent;

    setTitle(DmServerBundle.message("DMServerRepositoryFolderDialog.title"));

    UiUtil.setupDirectoryPicker(myPathTextField,
                                DmServerBundle.message("DMServerRepositoryFolderDialog.browse.title"),
                                DmServerBundle.message("DMServerRepositoryFolderDialog.browse.description"),
                                null,
                                new TextComponentAccessor<>() {

                                  @Override
                                  public String getText(JTextField component) {
                                    return getPathText(component);
                                  }

                                  @Override
                                  public void setText(JTextField component, @NotNull String text) {
                                    setPathText(component, text);
                                  }
                                });

    init();
  }

  @Override
  protected void doOKAction() {
    myItem = createItem();
    if (myItem == null) {
      return;
    }
    super.doOKAction();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  public T getItem() {
    return myItem;
  }

  protected final boolean checkEmpty(JTextField field, @Nls String errorEmptyMessage) {
    if (StringUtil.isEmpty(field.getText())) {
      setErrorText(errorEmptyMessage, field);
      return true;
    }
    else {
      return false;
    }
  }

  protected final JRadioButton getWatchedRadioButton() {
    return myWatchedRadioButton;
  }

  protected final JRadioButton getExternalRadioButton() {
    return myExternalRadioButton;
  }

  protected final JTextField getNameTextField() {
    return myNameTextField;
  }

  protected final TextFieldWithBrowseButton getPathTextField() {
    return myPathTextField;
  }

  protected final JTextField getWatchedIntervalTextField() {
    return myWatchedIntervalTextField;
  }

  protected final DMServerRepositoryEditor getParent() {
    return myParent;
  }

  protected final String getPathWithWildcardsText(JTextField component) {
    WildcardsFinder wildcardsFinder = new WildcardsFinder(component.getText());
    return getParent().getParent().getPathResolver().path2Absolute(wildcardsFinder.getPath());
  }

  protected final void setPathWithWildcardsText(JTextField component, String text) {
    WildcardsFinder wildcardsFinder = new WildcardsFinder(component.getText());
    String path = getParent().getParent().getPathResolver().path2Relative(text);
    component.setText(wildcardsFinder.hasWildcards() ? path + "/" + wildcardsFinder.getWildcards() : path + "/" + DEFAULT_WILDCARDS);
  }

  @Nullable
  protected abstract T createItem();

  public abstract void setItem(T item);

  protected abstract String getPathText(JTextField component);

  protected abstract void setPathText(JTextField component, @NotNull String text);
}
