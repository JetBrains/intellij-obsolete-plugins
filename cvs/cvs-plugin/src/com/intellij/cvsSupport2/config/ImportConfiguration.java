// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.cvsSupport2.config;

import com.intellij.cvsSupport2.ui.experts.importToCvs.FileExtension;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// https://stackoverflow.com/questions/64082680/intellij-plugin-cannot-init-component-state-exception
// todo: migrate to settings: https://plugins.jetbrains.com/docs/intellij/settings-guide.html#extension-points-for-settings
@State(
        name = "ImportConfiguration",
        storages = @Storage(value = "other.xml", roamingType = RoamingType.DISABLED),
        reportStatistic = false
)
public class ImportConfiguration implements PersistentStateComponent<ImportConfiguration>, NamedComponent {
  public String VENDOR;
  public String RELEASE_TAG;
  public String LOG_MESSAGE;
  public boolean CHECKOUT_AFTER_IMPORT = true;
  public String KEYWORD_SUBSTITUTION_WRAPPERS = "";
  public boolean MAKE_NEW_FILES_READ_ONLY = false;

  public static ImportConfiguration getInstance() {
    return ApplicationManager.getApplication().getService(ImportConfiguration.class);
  }

  public Collection<FileExtension> getExtensions() {
    if (StringUtil.isEmpty(KEYWORD_SUBSTITUTION_WRAPPERS)) {
      return Collections.emptyList();
    }

    final ArrayList<FileExtension> result = new ArrayList<>();
    final String[] wrappers = KEYWORD_SUBSTITUTION_WRAPPERS.split(";");
    for (String wrapper : wrappers) {
      final String[] extAndSubstitution = wrapper.split(" ");
      if (extAndSubstitution.length != 2) continue;
      result.add(new FileExtension(extAndSubstitution[0], extAndSubstitution[1]));
    }
    return result;
  }

  public void setExtensions(List<FileExtension> items) {
    final StringBuilder buffer = new StringBuilder();
    for (FileExtension extension : items) {
      buffer.append(extension.getExtension());
      buffer.append(" ");
      buffer.append(extension.getKeywordSubstitution().getSubstitution().toString());
      buffer.append(";");
    }
    KEYWORD_SUBSTITUTION_WRAPPERS = buffer.toString();
  }

  @Override
  public @NotNull String getComponentName() {
    return "ImportConfiguration";
  }

  @Override
  public @Nullable ImportConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull ImportConfiguration state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
