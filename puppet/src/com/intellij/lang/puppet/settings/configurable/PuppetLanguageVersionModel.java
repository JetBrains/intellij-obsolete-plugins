package com.intellij.lang.puppet.settings.configurable;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

public class PuppetLanguageVersionModel extends AbstractListModel implements ComboBoxModel {
  private static final PuppetLanguage.Version @NotNull [] LANGUAGES = PuppetLanguage.Version.values();
  private @NotNull PuppetLanguage.Version mySelectedLanguage = PuppetLanguage.Version.PUPPET_3;

  @Override
  public int getSize() {
    return LANGUAGES.length;
  }

  @Override
  public Object getElementAt(int index) {
    return LANGUAGES[index];
  }

  @Override
  public void setSelectedItem(Object anItem) {
    if (mySelectedLanguage != anItem && ArrayUtil.contains(anItem, LANGUAGES)) {
      mySelectedLanguage = ((PuppetLanguage.Version)anItem);
    }
    fireContentsChanged(this, -1, -1);
  }

  @Override
  public PuppetLanguage.Version getSelectedItem() {
    return mySelectedLanguage;
  }
}
