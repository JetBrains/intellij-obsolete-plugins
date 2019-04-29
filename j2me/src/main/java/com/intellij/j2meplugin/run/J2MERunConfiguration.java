/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.emulator.Emulator;
import com.intellij.j2meplugin.emulator.EmulatorType;
import com.intellij.j2meplugin.emulator.MobileSdk;
import com.intellij.j2meplugin.module.J2MEModuleType;
import com.intellij.j2meplugin.module.MobileModuleUtil;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.j2meplugin.module.settings.general.UserDefinedOption;
import com.intellij.j2meplugin.run.ui.J2MERunConfigurationEditor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class J2MERunConfiguration extends LocatableConfigurationBase implements ModuleRunProfile {

  public String TARGET_DEVICE_NAME;

  public String COMMAND_LINE_PARAMETERS;

  @NonNls
  private static final String NAME = "name";
  @NonNls
  private static final String VALUE = "value";

  //private static final String DEFAULT_DEBUG_PORT = "2812";

  private final Project myProject;
  private Module myModule;
  @NonNls
  private static final String USER_KEYS = "USER_OPTIONS";
  @NonNls
  private static final String OPTION = "option";
  @NonNls
  private static final String MODULE = "module";
  public String MAIN_CLASS_NAME = "";
  public String JAD_NAME = "";
  public boolean IS_CLASSES = false;
  public boolean IS_OTA = false;

  public String INSTALL;
  public String REMOVE;
  public String RUN;
  public String TRANSIENT;
  public String FORCE;

  public String TO_START;
  public int SELECTION;

  public ArrayList<UserDefinedOption> userParameters = new ArrayList<>();
  private String myModuleName;
  private static final Logger LOG = Logger.getInstance(J2MERunConfiguration.class);

  public J2MERunConfiguration(final String name, final Project project, ConfigurationFactory configurationFactory) {
    super(project, configurationFactory, name);
    myProject = project;
  }

  @Nullable
  public Module getModule() {
    if (myModule == null && myModuleName != null) {
      myModule = ModuleManager.getInstance(getProject()).findModuleByName(myModuleName);
    }
    return myModule != null && !myModule.isDisposed() && ModuleType.get(myModule).equals(J2MEModuleType.getInstance()) ? myModule : null;
  }

  @Override
  public RunProfileState getState(@NotNull final Executor executor, @NotNull final ExecutionEnvironment env) throws ExecutionException {
    Sdk jdk = getProjectJdk();
    if (!MobileSdk.checkCorrectness(jdk, getModule())) {
      //just to throw execution exception
      return new J2MERunnableState(env.getRunnerSettings(), this, getProject(), jdk);
    }
    final EmulatorType emulatorType = ((Emulator)jdk.getSdkAdditionalData()).getEmulatorType();
    LOG.assertTrue(emulatorType != null);
    return emulatorType.getJ2MERunnableState(executor, env.getRunnerSettings(), this, getProject(), jdk);
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new J2MERunConfigurationEditor(getProject(), this);
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
    Element module = element.getChild(MODULE);
    if (module != null) {
      myModuleName = module.getAttributeValue(NAME);
    }
    Element userOptions = element.getChild(USER_KEYS);
    if (userOptions != null) {
      userParameters.clear();
      for (final Object o : userOptions.getChildren(OPTION)) {
        Element option = (Element)o;
        userParameters.add(new UserDefinedOption(option.getAttributeValue(NAME), option.getAttributeValue(VALUE)));
      }
    }
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    Element moduleElement = new Element(MODULE);
    moduleElement.setAttribute(NAME, ApplicationManager.getApplication().runReadAction(new Computable<String>() {
      @Override
      public String compute() {
        final Module module = getModule();
        return module != null ? module.getName() : "";
      }
    }));
    element.addContent(moduleElement);
    Element userOptions = new Element(USER_KEYS);
    for (final UserDefinedOption userParameter : userParameters) {
      Element userOption = new Element(OPTION);
      userOption.setAttribute(NAME, userParameter.getKey());
      userOption.setAttribute(VALUE, userParameter.getValue());
      userOptions.addContent(userOption);
    }
    element.addContent(userOptions);
    DefaultJDOMExternalizer.writeExternal(this, element);

  }

  @Override
  public String suggestedName() {
    if (StringUtil.isEmpty(MAIN_CLASS_NAME)) {
      return null;
    }
    return IS_CLASSES ? MAIN_CLASS_NAME : JAD_NAME;
  }

  public void setMainClassName(final String qualifiedName) {
    final boolean generatedName = isGeneratedName();
    MAIN_CLASS_NAME = qualifiedName;
    if (generatedName) setGeneratedName();
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    final Module module = getModule();
    if (module == null) {
      throw new RuntimeConfigurationException(J2MEBundle.message("run.configuration.module.not.specified"));
    }
    Sdk jdk = getProjectJdk();
    if (jdk == null) {
      throw new RuntimeConfigurationException(J2MEBundle.message("run.configuration.jdk.misconfigured"));
    }
    if (!MobileSdk.checkCorrectness(jdk, module)) {
      throw new RuntimeConfigurationException(J2MEBundle.message("compiler.jdk.is.invalid", jdk.getName(),
                                                                 ReadAction
                                                                   .compute(() -> module.getName())));
    }

    final EmulatorType emulatorType = ((Emulator)jdk.getSdkAdditionalData()).getEmulatorType();
    LOG.assertTrue(emulatorType != null);
    final MobileApplicationType mobileApplicationType = MobileModuleUtil.getMobileApplicationTypeByName(emulatorType.getApplicationType());
    if (!IS_OTA) {
      if (IS_CLASSES) {
        if (MAIN_CLASS_NAME == null || MAIN_CLASS_NAME.trim().length() == 0){
          throw new RuntimeConfigurationWarning(J2MEBundle.message("run.configuration.no.class.specified", mobileApplicationType.getPresentableClassName()));
        }
        PsiClass psiClass = JavaPsiFacade.getInstance(myProject).findClass(MAIN_CLASS_NAME, GlobalSearchScope.moduleScope(module));
        if (psiClass == null) {
          throw new RuntimeConfigurationWarning(J2MEBundle.message("run.configuration.class.not.found", MAIN_CLASS_NAME, module.getName()));
        }
      }
      else {
        if (JAD_NAME == null || JAD_NAME.length() == 0) {
          throw new RuntimeConfigurationException(J2MEBundle.message("run.configuration.no.file.specified", mobileApplicationType.getExtension()));
        }
        if (!JAD_NAME.endsWith(mobileApplicationType.getExtension())) {
          throw new RuntimeConfigurationException(J2MEBundle.message("run.configuration.mistyped.descriptor", JAD_NAME, mobileApplicationType.getName()));
        }
      }
    }
    else {
      if (TO_START == null || TO_START.length() == 0) {
        throw new RuntimeConfigurationException(J2MEBundle.message("run.configuration.no.file.specified", mobileApplicationType.getExtension()));
      }
    }

  }

  public Sdk getProjectJdk() {
    Sdk jdk = null;
    if (getModule() != null) {
      jdk = ModuleRootManager.getInstance(getModule()).getSdk();
    }
    /*  else {
        jdk = ProjectRootManager.getInstance(getProject()).getProjectJdk();
      }*/
    return jdk;
  }

  @Override
  @NotNull
  public Module[] getModules() {
    List<Module> modules = new ArrayList<>();
    Module[] allModules = ModuleManager.getInstance(getProject()).getModules();
    for (Module module : allModules) {
      if (ModuleType.get(module).equals(J2MEModuleType.getInstance())) {
        modules.add(module);
      }
    }
    return modules.toArray(Module.EMPTY_ARRAY);
  }

  public void setModule(final Module module) {
    myModule = module;
  }
}

