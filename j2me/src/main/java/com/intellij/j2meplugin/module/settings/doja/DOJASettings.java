/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.doja;

import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.J2MEModuleBuilder;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

public class DOJASettings extends MobileModuleSettings {
  public DOJASettings() {}

  public DOJASettings(Module module) {
    super(module);
  }

  @Override
  public void copyTo(MobileModuleSettings mobileModuleSettings) {
    if (!(mobileModuleSettings instanceof DOJASettings)) return;
    super.copyTo(mobileModuleSettings);
  }

  @Override
  public void initSettings(J2MEModuleBuilder moduleBuilder) {
    super.initSettings(moduleBuilder);
    if (myDefaultModified) {
      putIfNotExists(DOJAApplicationType.APPLICATION_NAME, moduleBuilder.getName());
    }
    else {
      putSetting(DOJAApplicationType.APPLICATION_NAME, moduleBuilder.getName());
    }
  }

  @Override
  public void prepareJarSettings() {
    super.prepareJarSettings();
    final String lastModified = new SimpleDateFormat(J2MEBundle.message("doja.time.format")).format(new Date(new File(myJarURL).lastModified()));
    LOG.assertTrue(lastModified != null);
    if (myJarURL != null) {
      putSetting(DOJAApplicationType.LAST_MODIFIED,
                 lastModified);
    }
  }

  @Override
  @Nullable
  public File getManifest() {
    return null;
  }

  @Override
  public SortedSet<String> getMIDlets() {
    TreeSet<String> treeSet = new TreeSet<>();
    final String appClass = properties.get(DOJAApplicationType.APPLICATION_CLASS);
    if (appClass != null) {
      treeSet.add(DOJAApplicationType.APPLICATION_CLASS);
    }
    return treeSet;
  }

  @Override
  public void setMIDletClassName(final String name, final String className) {
    properties.put(name, className);
  }

  @Override
  public String getMIDletClassName(final String midletKey) {
    return properties.get(midletKey);
  }

  @Override
  public boolean isMidletKey(final String key) {
    return Comparing.strEqual(key, DOJAApplicationType.APPLICATION_CLASS);
  }

  @Override
  public MobileApplicationType getApplicationType() {
    return DOJAApplicationType.getInstance();
  }

  @Override
  public void addMidlet(final String qualifiedName) {
    final String currentApplication = properties.get(DOJAApplicationType.APPLICATION_CLASS);
    if (currentApplication != null) {
      if (Messages.showYesNoDialog(J2MEBundle.message("doja.configuration.contains.executable.class.dialog.message", currentApplication, qualifiedName),
                                   J2MEBundle.message("doja.configuration.contains.executable.class.dialog.title"), Messages.getWarningIcon()) != Messages.YES) {
        return;
      }
    }
    properties.put(DOJAApplicationType.APPLICATION_CLASS, qualifiedName);
    super.addMidlet(qualifiedName);
  }

  @Override
  public boolean containsMidlet(final String qualifiedName) {
    return Comparing.strEqual(properties.get(DOJAApplicationType.APPLICATION_CLASS).trim(), qualifiedName);
  }

}
