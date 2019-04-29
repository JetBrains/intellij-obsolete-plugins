/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.midp;

import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.emulator.Emulator;
import com.intellij.j2meplugin.emulator.MobileSdk;
import com.intellij.j2meplugin.module.J2MEModuleBuilder;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.j2meplugin.module.settings.general.UserDefinedOption;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.regex.Pattern;

public class MIDPSettings extends MobileModuleSettings {
  public MIDPSettings() {}

  public MIDPSettings(Module module) {
    super(module);
  }

  @Override
  public void copyTo(MobileModuleSettings mobileModuleSettings) {
    if (!(mobileModuleSettings instanceof MIDPSettings)) return;
    super.copyTo(mobileModuleSettings);
  }


  @Override
  public void initSettings(J2MEModuleBuilder moduleBuilder) {
    super.initSettings(moduleBuilder);
    if (myDefaultModified) {
      putIfNotExists(MIDPApplicationType.MIDLET_NAME, moduleBuilder.getName());
      putIfNotExists(MIDPApplicationType.MIDLET_VENDOR, J2MEBundle.message("module.settings.default.vendor"));
      putIfNotExists(MIDPApplicationType.MIDLET_VERSION, "1.0");
    }
    else {
      putSetting(MIDPApplicationType.MIDLET_NAME, moduleBuilder.getName());
      putSetting(MIDPApplicationType.MIDLET_VENDOR, J2MEBundle.message("module.settings.default.vendor"));
      putSetting(MIDPApplicationType.MIDLET_VERSION, "1.0");
    }
  }

  @Override
  public void prepareJarSettings() {
    super.prepareJarSettings();
    Sdk myProjectJdk = ModuleRootManager.getInstance(myModule).getSdk();
    if (MobileSdk.checkCorrectness(myProjectJdk, myModule)) {
      final String profile = ((Emulator)myProjectJdk.getSdkAdditionalData()).getProfile();
      LOG.assertTrue(profile != null);
      if (!isSynchronized()) { //refresh if user changed emulator settings
        putSetting(MIDPApplicationType.MIDLET_PROFILE,
                   profile);
      }
      final String configuration = ((Emulator)myProjectJdk.getSdkAdditionalData()).getConfiguration();
      LOG.assertTrue(configuration != null);
      if (!isSynchronized()) {
        putSetting(MIDPApplicationType.MIDLET_CONFIGURATION,
                   configuration);
      }
    }
  }

  @Override
  public File getManifest() {
    final File manifest = super.getManifest();
    if (manifest != null) return manifest;
    final String separator = MIDPApplicationType.getInstance().getSeparator();
    try {
      File temp = FileUtil.createTempFile("temp", ".mf");
      temp.deleteOnExit();
      PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(temp)));

      putManifestAttribute(printWriter, Attributes.Name.MANIFEST_VERSION.toString(), separator, "1.0");
      putManifestAttribute(printWriter, MIDPApplicationType.MIDLET_CONFIGURATION, separator,
                           getSettings().get(MIDPApplicationType.MIDLET_CONFIGURATION));

      putManifestAttribute(printWriter, MIDPApplicationType.MIDLET_NAME, separator, getSettings().get(MIDPApplicationType.MIDLET_NAME));
      putManifestAttribute(printWriter, MIDPApplicationType.MIDLET_VENDOR, separator, getSettings().get(MIDPApplicationType.MIDLET_VENDOR));

      for (String key : getMIDlets()) {
        putManifestAttribute(printWriter, key, separator, getSettings().get(key).replaceAll(",", ", "));
      }

      putManifestAttribute(printWriter, MIDPApplicationType.MIDLET_VERSION, separator,
                           getSettings().get(MIDPApplicationType.MIDLET_VERSION));

      putManifestAttribute(printWriter, MIDPApplicationType.MIDLET_PROFILE, separator,
                           getSettings().get(MIDPApplicationType.MIDLET_PROFILE));

      for (UserDefinedOption key : getUserDefinedOptions()) {
        putManifestAttribute(printWriter, key.getKey(), separator, key.getValue());
      }

      printWriter.close();
      return temp;
    }
    catch (IOException e) {
      //do nothing
    }
    return null;
  }


  /* public void readExternal(Element parentElement) throws InvalidDataException {
     super.readExternal(parentElement);
     Element userOptionsGroup = parentElement.getChild(USER_DEFINED_OPTIONS);
     if (userOptionsGroup != null) {
       for (Iterator<Element> iterator = userOptionsGroup.getChildren(USER_OPTION).getSectionsIterator(); getSectionsIterator.hasNext();) {
         Element option = iterator.next();
         UserDefinedOption userDefinedOption = new UserDefinedOption(option.getAttributeValue(USER_OPTION_KEY),
                                                                     option.getAttributeValue(USER_OPTION_VALUE));
         userDefinedOptions.add(userDefinedOption);
       }
     }

   }*/

  /* public void writeExternal(Element parentElement) throws WriteExternalException {
     Element userOptionsGroup = new Element(USER_DEFINED_OPTIONS);
     for (Iterator<UserDefinedOption> iterator = userDefinedOptions.getSectionsIterator(); getSectionsIterator.hasNext();) {
       Element userOption = new Element(USER_OPTION);
       UserDefinedOption option = iterator.next();
       userOption.setAttribute(USER_OPTION_KEY, option.getKey());
       userOption.setAttribute(USER_OPTION_VALUE, option.getValue());
       userOptionsGroup.addContent(userOption);
     }
     parentElement.addContent(userOptionsGroup);
     parentElement.addContent(userOptionsGroup);
     super.writeExternal(parentElement);

   }
 */

  @Override
  public SortedSet<String> getMIDlets() {
    SortedSet<String> midletNumbers = new TreeSet<>();
    for (String key : getSettings().keySet()) {
      if (isMidletKey(key)) {
        midletNumbers.add(key);
      }
    }
    return midletNumbers;
  }

  @Override
  public void setMIDletClassName(final String key, final String className) {
    final MIDletProperty property = new MIDletProperty(key, getSettings().get(key));
    getSettings().put(key, property.getName() + "," + property.getIcon() + "," + className);
  }

  @Override
  @Nullable
  public String getMIDletClassName(final String key) {
    final String value = getSettings().get(key);
    final MIDletProperty property;
    if (value != null) {
      property = new MIDletProperty(key, value);
    } else {
      property = null;
    }
    return property != null ? property.getClassName() : null;
  }

  @Override
  public boolean isMidletKey(final String key) {
    @NonNls final String pattern = "\\d*";
    final Pattern midlets = Pattern.compile(MIDPApplicationType.MIDLET_PREFIX + pattern);
    return midlets.matcher(key).matches();
  }

  @Override
  public MobileApplicationType getApplicationType() {
    return MIDPApplicationType.getInstance();
  }

  @Override
  public void addMidlet(final String qualifiedName) {
    final int dotIdx = qualifiedName.lastIndexOf('.');
    final String name = dotIdx > -1 ? qualifiedName.substring(dotIdx + 1) : qualifiedName;
    getSettings().put(MIDPApplicationType.MIDLET_PREFIX + (getMIDlets().size() + 1), name + ",," + qualifiedName);
    super.addMidlet(qualifiedName);
  }

  @Override
  public boolean containsMidlet(final String qualifiedName) {
    final SortedSet<String> midlets = getMIDlets();
    for (String midlet : midlets) {
      if (ArrayUtil.find(getSettings().get(midlet).split("[, ]"), qualifiedName) != -1) {
        return true;
      }
    }
    return false;
  }

  public static class MIDletProperty {
    private final String name;
    private final String icon;
    private final String className;
    private final String number;

    public MIDletProperty(String name, String icon, String className, String number) {
      this.name = name;
      this.icon = icon;
      this.className = className;
      this.number = number;
    }

    public MIDletProperty(String number, String value) {
      String[] midlet = value.split(",");
      LOG.assertTrue(midlet != null && midlet.length == 3);
      name = midlet[0];
      icon = midlet[1];
      className = midlet[2];
      this.number = number;
    }

    public String getName() {
      return name;
    }

    public String getIcon() {
      return icon;
    }

    public String getClassName() {
      return className;
    }

    public String getValueString() {
      return name + "," + icon + "," + className;
    }

    public String getNumber() {
      return number;
    }

    public boolean equals(Object o) {
      if (!(o instanceof MIDletProperty)) return false;
      return ((MIDletProperty)o).getNumber().equals(getNumber());
    }

    public int hashCode() {
      int result = getNumber().hashCode() * 29;
      return result + getValueString().hashCode() * 29;
    }
  }


}
