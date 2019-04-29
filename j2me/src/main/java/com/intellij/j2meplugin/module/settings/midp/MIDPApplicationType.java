/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.midp;

import com.intellij.j2meplugin.module.J2MEModuleBuilder;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.j2meplugin.module.settings.MobileSettingsConfigurable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;

public class MIDPApplicationType extends MobileApplicationType {
  @NonNls
  public static final String MIDLET_NAME = "MIDlet-Name";
  @NonNls
  public static final String MIDLET_JAR_SIZE_NAME = "MIDlet-Jar-Size";
  @NonNls
  public static final String MIDLET_JAR_URL = "MIDlet-Jar-URL";
  @NonNls
  public static final String MIDLET_VERSION = "MIDlet-Version";
  @NonNls
  public static final String MIDLET_VENDOR = "MIDlet-Vendor";
  @NonNls
  public static final String MIDLET_CONFIGURATION = "MicroEdition-Configuration";
  @NonNls
  public static final String MIDLET_PROFILE = "MicroEdition-Profile";

  @NonNls
  public static final String MIDLET_PREFIX = "MIDlet-";
  @NonNls
  public static final String MIDLET_DESCRIPTION = "MIDlet-Description";
  @NonNls
  public static final String MIDLET_INFO_URL = "MIDlet-Info-URL";
  @NonNls
  public static final String MIDLET_DELETE_CONFIRM = "MIDlet-Delete-Confirm";
  @NonNls
  public static final String MIDLET_INSTALL_NOTIFY = "MIDlet-Install-Notify";
  @NonNls
  public static final String MIDLET_DATA_SIZE = "MIDlet-Data-Size";
  @NonNls
  public static final String MIDLET_ICON = "MIDlet-Icon";

  @NonNls
  public static final String NAME = "MIDP";

  public static MIDPApplicationType getInstance() {
    for (MobileApplicationType applicationType : MOBILE_APPLICATION_TYPE.getExtensionList()) {
      if (applicationType instanceof MIDPApplicationType) {
        return (MIDPApplicationType)applicationType;
      }
    }
    return null;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getExtension() {
    return "jad";
  }

  @Override
  public String getSeparator() {
    return ":";
  }

  @Override
  public String getJarSizeSettingName() {
    return MIDLET_JAR_SIZE_NAME;
  }

  @Override
  public String getBaseClassName() {
    return "javax.microedition.midlet.MIDlet";
  }

  @Override
  public boolean isUserParametersEnable() {
    return true;
  }

  @Override
  public String getJarUrlSettingName() {
    return MIDLET_JAR_URL;
  }

  @Override
  public boolean isUserField(String name) {
    return !name.startsWith(MIDLET_PREFIX) && !name.equals(MIDLET_PROFILE) && !name.equals(MIDLET_CONFIGURATION);
  }

  @Override
  public String createConfigurationByClass(String className) {
    return MIDLET_PREFIX + "1: " + className + ",," + className;
  }

  @Override
  public Class<? extends MobileModuleSettings> getClassType() {
    return MIDPSettings.class;
  }

  @Override
  public MobileSettingsConfigurable createConfigurable(final Project project, final Module module, final MobileModuleSettings settings) {
    return new MIDPSettingsConfigurable(module, settings, project);
  }

  @Override
  public MobileModuleSettings createTempSettings(final J2MEModuleBuilder builder) {
    final MIDPSettings settings = new MIDPSettings();
    settings.initSettings(builder);
    return settings;
  }

}
