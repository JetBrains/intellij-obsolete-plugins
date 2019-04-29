/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator;

import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.emulator.ui.MobileSdkConfigurable;
import com.intellij.j2meplugin.module.J2MEModuleProperties;
import com.intellij.j2meplugin.module.J2MEModuleType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.impl.JavaDependentSdkType;
import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import icons.J2meIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;

public class MobileSdk extends JavaDependentSdkType implements JavaSdkType {
  private static final Logger LOG = Logger.getInstance("#com.intellij.j2meplugin");

  public MobileSdk() {
    super("MobileSDK");
  }

  @Override
  @NotNull
  public String getPresentableName() {
    return J2MEBundle.message("jdk.type.name");
  }

  @Override
  public Icon getIcon() {
    return J2meIcons.Sdk_closed;
  }

  @Override
  @NotNull
  public Icon getIconForAddAction() {
    return J2meIcons.Add_sdk;
  }

  public static MobileSdk getInstance() {
    return SdkType.findInstance(MobileSdk.class);
  }

  public static boolean checkCorrectness(Sdk projectJdk, Module module) {
    if (projectJdk == null) return false;
    if (!projectJdk.getSdkType().equals(getInstance())) return false;
    if (!(projectJdk.getSdkAdditionalData() instanceof Emulator)) return false;
    final Emulator emulator = (Emulator)projectJdk.getSdkAdditionalData();
    if (emulator.getHome() == null) {
      emulator.setHome(projectJdk.getHomePath());
    }
    final EmulatorType emulatorType = emulator.getEmulatorType();
    if (emulatorType == null) return false;
    final Sdk javaSdk = emulator.getJavaSdk();
    if (javaSdk == null || !javaSdk.getSdkType().equals(JavaSdk.getInstance())) {
      return false;
    }
    if (module != null) {
      if (!ModuleType.get(module).equals(J2MEModuleType.getInstance()) ||
           J2MEModuleProperties.getInstance(module).getMobileApplicationType() == null) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  public static EmulatorType getEmulatorType(Sdk jdk, Module module) {
    if (checkCorrectness(jdk, module)) {
      return ((Emulator)jdk.getSdkAdditionalData()).getEmulatorType();
    }
    return null;
  }

  @Override
  public AdditionalDataConfigurable createAdditionalDataConfigurable(@NotNull SdkModel sdkModel, @NotNull SdkModificator sdkModificator) {
    return new MobileSdkConfigurable(sdkModel, sdkModificator);
  }

  @Override
  public void saveAdditionalData(@NotNull SdkAdditionalData additionalData, @NotNull Element additional) {
    if (!(additionalData instanceof Emulator)) return;
    try {
      ((Emulator)additionalData).writeExternal(additional);
    }
    catch (WriteExternalException e) {
      LOG.error(e);
    }
  }

  @Override
  public SdkAdditionalData loadAdditionalData(@NotNull Element additional) {
    Emulator emulator = new Emulator();
    try {
      emulator.readExternal(additional);
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }
    return emulator;
  }

  @Override
  public String getBinPath(@NotNull Sdk sdk) {
    if (!checkCorrectness(sdk, null)) return null;
    Sdk mySdk = ((Emulator)sdk.getSdkAdditionalData()).getJavaSdk();
    LOG.assertTrue(mySdk != null);
    return ((JavaSdk)mySdk.getSdkType()).getBinPath(mySdk);
  }

  @Override
  public String getToolsPath(@NotNull Sdk sdk) {
    if (!checkCorrectness(sdk, null)) return null;
    Sdk mySdk = ((Emulator)sdk.getSdkAdditionalData()).getJavaSdk();
    LOG.assertTrue(mySdk != null);
    return ((JavaSdk)mySdk.getSdkType()).getToolsPath(mySdk);
  }

  @Override
  public String getVMExecutablePath(@NotNull Sdk sdk) {
    if (!checkCorrectness(sdk, null)) return null;
    Sdk mySdk = ((Emulator)sdk.getSdkAdditionalData()).getJavaSdk();
    LOG.assertTrue(mySdk != null);
    return ((JavaSdk)mySdk.getSdkType()).getVMExecutablePath(mySdk);
  }

  @Override
  public String suggestHomePath() {
    return null;
  }

  @Override
  public boolean isValidSdkHome(String path) {
    return EmulatorUtil.getValidEmulatorType(path) != null;
  }

  @Override
  public String getVersionString(String sdkHome) {
    return "1.3";
  }

  @NotNull
  @Override
  public String suggestSdkName(@Nullable String currentSdkName, String sdkHome) {
    final EmulatorType emulatorType = EmulatorUtil.getValidEmulatorType(sdkHome);
    if (emulatorType != null && emulatorType.suggestName(sdkHome) != null) {
      String name = emulatorType.suggestName(sdkHome);
      if (name != null) return name;
    }
    return "Mobile SDK";
  }


  @Override
  public void setupSdkPaths(@NotNull Sdk sdk) {
    VirtualFile[] classes = null;
    final File mobileJdkHome = new File(sdk.getHomePath());
    final EmulatorType emulatorType = EmulatorUtil.getValidEmulatorType(sdk.getHomePath());
    LOG.assertTrue(emulatorType != null);
    final String[] apiClasses = emulatorType.getApi(sdk.getHomePath());
    if (apiClasses != null) {
      classes = MobileSdkUtil.findApiClasses(apiClasses);
    }
    if (classes == null || classes.length == 0) {
      classes = MobileSdkUtil.findApiClasses(mobileJdkHome);
    }
    ArrayList<VirtualFile> docs = new ArrayList<>();
    @NonNls final String docsString = "docs";
    @NonNls final String apiString = "api";
    @NonNls final String docString = "doc";
    final File api = new File(new File(mobileJdkHome, docsString), apiString).exists() ? new File(new File(mobileJdkHome, docsString), apiString) :
                     new File(new File(mobileJdkHome, docString), apiString).exists() ? new File(new File(mobileJdkHome, docString), apiString) : null;
    if (api != null) {
      MobileSdkUtil.findDocs(api, docs);
    }
    else {
      MobileSdkUtil.findDocs(mobileJdkHome, docs);
    }

    final SdkModificator sdkModificator = sdk.getSdkModificator();
    for (int i = 0; classes != null && i < classes.length; i++) {
      sdkModificator.addRoot(classes[i], OrderRootType.CLASSES);
    }

    for (final VirtualFile doc : docs) {
      sdkModificator.addRoot(doc, JavadocOrderRootType.getInstance());
    }

    sdkModificator.setSdkAdditionalData(new Emulator(emulatorType, null, EmulatorUtil.findFirstJavaSdk(), sdk.getHomePath()));

    sdkModificator.commitChanges();

  }
}
