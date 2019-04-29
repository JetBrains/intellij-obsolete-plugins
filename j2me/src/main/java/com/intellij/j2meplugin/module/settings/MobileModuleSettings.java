/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings;

import com.intellij.j2meplugin.compiler.MobileMakeUtil;
import com.intellij.j2meplugin.emulator.MobileSdk;
import com.intellij.j2meplugin.module.J2MEModuleBuilder;
import com.intellij.j2meplugin.module.J2MEModuleProperties;
import com.intellij.j2meplugin.module.settings.general.UserDefinedOption;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

public abstract class MobileModuleSettings implements JDOMExternalizable {
  protected static final Logger LOG = Logger.getInstance("#com.intellij.j2meplugin");

  protected HashSet<UserDefinedOption> userDefinedOptions = new HashSet<>();
  protected TreeMap<String,String> properties = new TreeMap<>();
  protected String myMobileDescriptionPath = "";
  protected String myJarURL = "";
  protected Module myModule;
  protected boolean mySynchronized = false;
  protected boolean myUseUserManifest = false;
  protected String myUserManifestPath;

  @NonNls
  protected static final String DESCRIPTION_PATH = "descriptionPath";
  @NonNls
  protected static final String DESCRIPTION_PATH_VALUE = "path";

  @NonNls
  protected static final String SETTING = "setting";
  @NonNls
  protected static final String SETTING_NAME = "name";
  @NonNls
  protected static final String SETTING_VALUE = "module.settings.user.defined.value";

  @NonNls
  protected static final String USER_DEFINED_OPTIONS = "userDefinedOptions";
  @NonNls
  protected static final String USER_OPTION = "option";
  @NonNls
  protected static final String USER_OPTION_KEY = "module.settings.user.defined.key";
  @NonNls
  protected static final String USER_OPTION_VALUE = "module.settings.user.defined.value";

  private boolean isModified = false;
  @NonNls
  private static final String USE_USER_DESCRIPTOR = "useUserDescriptor";
  @NonNls
  private static final String USE_USER_MANIFEST = "useUserManifest";
  @NonNls
  private static final String MANIFEST = "manifest";
  @NonNls
  private static final String PATH = "path";
  @NonNls
  private static final String JAR_URL_NAME = "jarUrlName";
  @NonNls
  private static final String JAR_URL_SETTING = "jarUrl";

  protected boolean myDefaultModified = false;
  @NonNls private static final String JAR_FILE_TYPE = ".jar";

  public boolean isDefaultModified() {
    return myDefaultModified;
  }

  public void setDefaultModified(boolean defaultModified) {
    myDefaultModified = defaultModified;
  }

  protected MobileModuleSettings() {
  }

  public MobileModuleSettings(Module module) {
    myModule = module;
  }

  @Nullable
  public static MobileModuleSettings getInstance(Module module) {
    final J2MEModuleProperties properties = J2MEModuleProperties.getInstance(module);
    if (properties == null) return null;
    final MobileApplicationType applicationType = properties.getMobileApplicationType();
    if (applicationType == null) return null;
    return ModuleServiceManager.getService(module, applicationType.getClassType());
  }

  public TreeMap<String,String> getSettings() {
    return properties;
  }

  private void setSettings(TreeMap<String, String> properties) {
    this.properties = properties;
  }

  public HashSet<UserDefinedOption> getUserDefinedOptions() {
    return userDefinedOptions;
  }

  public void addUserDefinedOption(UserDefinedOption userDefinedOption) {
    userDefinedOptions.add(userDefinedOption);
  }

  protected void setUserDefinedOptions(HashSet<UserDefinedOption> userDefinedOptions) {
    this.userDefinedOptions = userDefinedOptions;
  }

  public Module getModule() {
    return myModule;
  }

  public boolean isSynchronized() {
    return mySynchronized;
  }

  public void setSynchronized(boolean aSynchronized) {
    mySynchronized = aSynchronized;
  }

  public boolean isUseUserManifest() {
    return myUseUserManifest;
  }

  public void setUseUserManifest(boolean useUserManifest) {
    myUseUserManifest = useUserManifest;
  }

  public String getUserManifestPath() {
    return myUserManifestPath;
  }

  public void setUserManifestPath(String userManifestPath) {
    myUserManifestPath = userManifestPath;
  }

  public String getJarURL() {
    return myJarURL;
  }

  public void setJarURL(String jarURL) {
    myJarURL = jarURL;
  }

  public void copyTo(MobileModuleSettings mobileModuleSettings) {
    mobileModuleSettings.setSettings(properties);
    mobileModuleSettings.setMobileDescriptionPath(getMobileDescriptionPath());
    mobileModuleSettings.setUserDefinedOptions(userDefinedOptions);
    mobileModuleSettings.setJarURL(myJarURL);
    mobileModuleSettings.setSynchronized(mySynchronized);
    mobileModuleSettings.setUserManifestPath(myUserManifestPath);
    mobileModuleSettings.setUseUserManifest(myUseUserManifest);
  }

  public String getMobileDescriptionPath() {
    return myMobileDescriptionPath;
  }

  public void setMobileDescriptionPath(String mobileDescriptionPath) {
    myMobileDescriptionPath = mobileDescriptionPath;
  }

  public boolean putSetting(String key, String value) {
    if (value != null && value.length() != 0 && key != null) {
      properties.put(key, value);
      return true;
    }
    return false;
  }

  @Override
  public void readExternal(Element parentElement) throws InvalidDataException {
    readSetting(parentElement);
  }

  public void readSetting(Element parentElement) {
    for (Element setting : parentElement.getChildren(SETTING)) {
      putSetting(setting.getAttributeValue(SETTING_NAME), setting.getAttributeValue(SETTING_VALUE));
    }

    Element path = parentElement.getChild(DESCRIPTION_PATH);
    if (path != null) {
      final String descriptorPath = path.getAttributeValue(DESCRIPTION_PATH_VALUE);
      if (descriptorPath != null) {
        myMobileDescriptionPath = descriptorPath.replace('/', File.separatorChar);
      }
      final String useUserDescriptor = path.getAttributeValue(USE_USER_DESCRIPTOR);
      if (useUserDescriptor != null) {
        mySynchronized = useUserDescriptor.equals(Boolean.TRUE.toString());
      }
    }

    Element manifest = parentElement.getChild(MANIFEST);
    if (manifest != null) {
      myUseUserManifest = manifest.getAttributeValue(USE_USER_MANIFEST).equals(Boolean.TRUE.toString());
      final String manifestPath = manifest.getAttributeValue(PATH);
      if (manifestPath != null) {
        myUserManifestPath = manifestPath.replace('/', File.separatorChar);
      }
    }

    Element jarUrl = parentElement.getChild(JAR_URL_SETTING);
    if (jarUrl != null && jarUrl.getAttributeValue(JAR_URL_NAME) != null) {
      myJarURL = jarUrl.getAttributeValue(JAR_URL_NAME).replace('/', File.separatorChar);
    }


    Element userOptionsGroup = parentElement.getChild(USER_DEFINED_OPTIONS);
    if (userOptionsGroup != null) {
      for (Element option : userOptionsGroup.getChildren(USER_OPTION)) {
        UserDefinedOption userDefinedOption = new UserDefinedOption(option.getAttributeValue(USER_OPTION_KEY),
                                                                    option.getAttributeValue(USER_OPTION_VALUE));
        userDefinedOptions.add(userDefinedOption);
      }
    }
  }

  @Override
  public void writeExternal(Element parentElement) throws WriteExternalException {
    writeSetting(parentElement);

  }

  public void writeSetting(Element parentElement) {
    for (String key : properties.keySet()) {
      Element setting = new Element(SETTING);
      setting.setAttribute(SETTING_NAME, key);
      setting.setAttribute(SETTING_VALUE, properties.get(key));
      parentElement.addContent(setting);
    }

    if (myJarURL != null) {
      Element jarUrl = new Element(JAR_URL_SETTING);
      jarUrl.setAttribute(JAR_URL_NAME, myJarURL.replace(File.separatorChar, '/'));
      parentElement.addContent(jarUrl);
    }

    if (myMobileDescriptionPath != null && !myMobileDescriptionPath.equals("")) {
      Element description = new Element(DESCRIPTION_PATH);
      description.setAttribute(DESCRIPTION_PATH_VALUE, myMobileDescriptionPath.replace(File.separatorChar, '/'));
      description.setAttribute(USE_USER_DESCRIPTOR, Boolean.toString(mySynchronized));
      parentElement.addContent(description);
    }

    Element manifest = new Element(MANIFEST);
    manifest.setAttribute(USE_USER_MANIFEST, Boolean.toString(myUseUserManifest));
    if (myUserManifestPath != null) {
      manifest.setAttribute(PATH, myUserManifestPath.replace(File.separatorChar, '/'));
    }
    parentElement.addContent(manifest);

    if (!userDefinedOptions.isEmpty()) {
      Element userOptionsGroup = new Element(USER_DEFINED_OPTIONS);
      for (UserDefinedOption option : userDefinedOptions) {
        Element userOption = new Element(USER_OPTION);
        userOption.setAttribute(USER_OPTION_KEY, option.getKey());
        userOption.setAttribute(USER_OPTION_VALUE, option.getValue());
        userOptionsGroup.addContent(userOption);
      }
      parentElement.addContent(userOptionsGroup);
    }
  }

  protected void putIfNotExists(String key, String value) {
    if (value != null && key != null) {
      if (properties.containsKey(key) && (properties.get(key) == null || properties.get(key).isEmpty())) {
        properties.put(key, value);
      }
      else if (!properties.containsKey(key)) {
        properties.put(key, value);
      }
    }
  }

  public void initSettings(J2MEModuleBuilder moduleBuilder) {
    if (moduleBuilder.getModuleFileDirectory() != null) {
      if (!myDefaultModified || myJarURL == null || myJarURL.isEmpty()) {
        myJarURL = FileUtil.toSystemDependentName(moduleBuilder.getModuleFileDirectory()) + File.separator + moduleBuilder.getName() + JAR_FILE_TYPE;
      }
      if (!myDefaultModified || myMobileDescriptionPath == null || myMobileDescriptionPath.isEmpty()) {
        myMobileDescriptionPath = FileUtil.toSystemDependentName(moduleBuilder.getModuleFileDirectory()) + File.separator + moduleBuilder.getName() + "." + moduleBuilder.getMobileApplicationType().getExtension();
      }
      if (!myDefaultModified) {
        putSetting(moduleBuilder.getMobileApplicationType().getJarUrlSettingName(), moduleBuilder.getName() + JAR_FILE_TYPE);
      }
      else {
        putIfNotExists(moduleBuilder.getMobileApplicationType().getJarUrlSettingName(), moduleBuilder.getName() + JAR_FILE_TYPE);
      }
      isModified = true;
    }
  }

  public void prepareJarSettings() {
    //can do nothing without right jdk
    if (!MobileSdk.checkCorrectness(ModuleRootManager.getInstance(myModule).getSdk(), myModule)) return;
    MobileApplicationType mobileApplicationType = J2MEModuleProperties.getInstance(myModule).getMobileApplicationType();
    if (myJarURL != null) {
      properties.put(mobileApplicationType.getJarSizeSettingName(), Long.toString(new File(myJarURL).length()));
    }
  }

  @Nullable
  public File getManifest() {
    if (myUseUserManifest && myUserManifestPath != null) {
      return new File(myUserManifestPath);
    }
    return null;
  }

  protected static void putManifestAttribute(PrintWriter pw, String key, String separator, String value) {
    if (value != null && !value.isEmpty()) {
      pw.println(key + separator + " " + value);
    }
  }

  public boolean isModified() {
    return isModified;
  }

  public void setModified(boolean modified) {
    isModified = modified;
  }

  public SortedSet<String> getMIDlets() {
    return new TreeSet<>();
  }

  public abstract void setMIDletClassName(String name, String className);

  @Nullable
  public abstract String getMIDletClassName(String midletKey);

  public abstract boolean isMidletKey(String key);

  public abstract MobileApplicationType getApplicationType();

  public void addMidlet(final String qualifiedName) {
    if (isSynchronized()) {
      try {
        MobileMakeUtil.makeJad(this, false);
      }
      catch (Exception e) {
        //can't be
      }
    }
  }

  public abstract boolean containsMidlet(final String qualifiedName);
}
