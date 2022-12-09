package com.intellij.dmserver.integration;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

public class DMServerRepositoryFolderDialog20 extends DMServerRepositoryFolderDialog<DMServerRepositoryItem20Base> {

  private final List<Behavior<?>> myBehaviors;

  private Behavior<?> myBehavior;

  private DMServerRepositoryItem20Base myInitialItem;

  public DMServerRepositoryFolderDialog20(DMServerRepositoryEditor parent) {
    super(parent);

    ButtonGroup radioGroup = new ButtonGroup();
    myBehaviors = Arrays.asList(new WatchedBehavior(), new ExternalBehavior());
    for (final Behavior behavior : myBehaviors) {
      radioGroup.add(behavior.getRadioButton());
      behavior.getRadioButton().addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          doSetBehavior(behavior);
        }
      });
    }
    setBehavior(myBehaviors.get(0));
  }

  @Override
  @Nullable
  protected DMServerRepositoryItem20Base createItem() {
    return myBehavior.createItem();
  }

  @Override
  public void setItem(DMServerRepositoryItem20Base item) {
    myInitialItem = item;
    for (Behavior behavior : myBehaviors) {
      if (behavior.load(item)) {
        setBehavior(behavior);
        break;
      }
    }
  }

  private void setBehavior(Behavior behavior) {
    behavior.getRadioButton().setSelected(true);
    doSetBehavior(behavior);
  }

  private void doSetBehavior(Behavior behavior) {
    myBehavior = behavior;
    myBehavior.activate();
  }

  private boolean checkNameNotUnique() {
    String name = getNameTextField().getText();
    for (DMServerRepositoryItem item : getParent().getRepositoryItems()) {
      if (item instanceof DMServerRepositoryItem20Base) {
        DMServerRepositoryItem20Base item20 = (DMServerRepositoryItem20Base)item;
        if (item20 != myInitialItem && name.equals(item20.getName())) {
          setErrorText(DmServerBundle.message("DMServerRepositoryFolderDialog20.warning.name.not.unique"), getNameTextField());
          return true;
        }
      }
    }
    return false;
  }

  @Override
  protected String getPathText(JTextField component) {
    return myBehavior.getText(component);
  }

  @Override
  protected void setPathText(JTextField component, @NotNull String text) {
    myBehavior.setText(component, text);
  }

  private abstract static class Behavior<T extends DMServerRepositoryItem20Base> implements TextComponentAccessor<JTextField> {

    public abstract void activate();

    @Nullable
    public abstract T createItem();

    public abstract JRadioButton getRadioButton();

    public boolean load(DMServerRepositoryItem20Base item) {
      if (getItemClass().isInstance(item)) {
        doLoad(getItemClass().cast(item));
        return true;
      }
      else {
        return false;
      }
    }

    protected abstract Class<T> getItemClass();

    protected abstract void doLoad(T item);
  }

  private class WatchedBehavior extends Behavior<DMServerRepositoryWatchedItem> {

    @Override
    public void activate() {
      String path = getPathTextField().getText();
      if (!StringUtil.isEmpty(path)) {
        WildcardsFinder wildcardsFinder = new WildcardsFinder(getPathTextField().getTextField().getText());
        if (wildcardsFinder.hasWildcards()) {
          getPathTextField().setText(wildcardsFinder.getPath());
        }
      }
      getWatchedIntervalTextField().setEnabled(true);
    }

    @Override
    @Nullable
    public DMServerRepositoryWatchedItem createItem() {
      if (checkEmpty(getNameTextField(), DmServerBundle.message("DMServerRepositoryFolderDialog20.warning.empty.name"))
          || checkNameNotUnique()
          || checkEmpty(getPathTextField().getTextField(), DmServerBundle.message("DMServerRepositoryFolderDialog20.warning.empty.path"))) {
        return null;
      }

      DMServerRepositoryWatchedItem result = new DMServerRepositoryWatchedItem();
      result.setName(getNameTextField().getText());
      result.setPath(getPathTextField().getText());
      result.setWatchedInterval(StringUtil.nullize(getWatchedIntervalTextField().getText()));
      return result;
    }

    @Override
    public JRadioButton getRadioButton() {
      return getWatchedRadioButton();
    }

    @Override
    public String getText(JTextField component) {
      return getParent().getParent().getPathResolver().path2Absolute(component.getText());
    }

    @Override
    public void setText(JTextField component, @NotNull String text) {
      component.setText(getParent().getParent().getPathResolver().path2Relative(text));
    }

    @Override
    protected Class<DMServerRepositoryWatchedItem> getItemClass() {
      return DMServerRepositoryWatchedItem.class;
    }

    @Override
    protected void doLoad(DMServerRepositoryWatchedItem item) {
      getNameTextField().setText(item.getName());
      getPathTextField().setText(item.getPath());
      getWatchedIntervalTextField().setText(item.getWatchedInterval());
    }
  }

  private class ExternalBehavior extends Behavior<DMServerRepositoryExternalItem> {

    @Override
    public void activate() {
      String path = getPathTextField().getText();
      if (!StringUtil.isEmpty(path)) {
        WildcardsFinder wildcardsFinder = new WildcardsFinder(getPathTextField().getTextField().getText());
        if (!wildcardsFinder.hasWildcards()) {
          getPathTextField().setText(path + "/" + DEFAULT_WILDCARDS);
        }
      }

      getWatchedIntervalTextField().setEnabled(false);
      getWatchedIntervalTextField().setText("");
    }

    @Override
    @Nullable
    public DMServerRepositoryExternalItem createItem() {
      if (checkEmpty(getNameTextField(), DmServerBundle.message("DMServerRepositoryFolderDialog20.warning.empty.name"))
          || checkNameNotUnique()
          || checkEmpty(getPathTextField().getTextField(), DmServerBundle.message("DMServerRepositoryFolderDialog20.warning.empty.path"))) {
        return null;
      }

      DMServerRepositoryExternalItem result = new DMServerRepositoryExternalItem();
      result.setName(getNameTextField().getText());
      result.setPath(getPathTextField().getText());
      return result;
    }

    @Override
    public JRadioButton getRadioButton() {
      return getExternalRadioButton();
    }

    @Override
    public String getText(JTextField component) {
      return getPathWithWildcardsText(component);
    }

    @Override
    public void setText(JTextField component, @NotNull String text) {
      setPathWithWildcardsText(component, text);
    }

    @Override
    protected Class<DMServerRepositoryExternalItem> getItemClass() {
      return DMServerRepositoryExternalItem.class;
    }

    @Override
    protected void doLoad(DMServerRepositoryExternalItem item) {
      getNameTextField().setText(item.getName());
      getPathTextField().setText(item.getPath());
    }
  }
}
