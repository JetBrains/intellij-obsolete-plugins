// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizer;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.JdomKt;
import org.jdom.Element;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public abstract class LocatableRunConfigurationWithCommonParameters extends LocatableConfigurationBase {

  private String myProgramParameters;
  private String myVMParameters;
  private final Map<String, String> myEnvs = new HashMap<>();
  private boolean myPassParentEnv = true;

  protected LocatableRunConfigurationWithCommonParameters(@NotNull Project project,
                                                          @NotNull ConfigurationFactory factory,
                                                          String name) {
    super(project, factory, name);
  }

  public @Nullable String getProgramParameters() {
    return myProgramParameters;
  }

  public void setProgramParameters(@Nullable String value) {
    myProgramParameters = value;
  }

  public String getVMParameters() {
    return myVMParameters;
  }

  public void setVMParameters(String vmParams) {
    this.myVMParameters = vmParams;
  }

  public @NotNull Map<String, String> getEnvs() {
    return myEnvs;
  }

  public void setEnvs(@NotNull Map<String, String> envs) {
    this.myEnvs.clear();
    this.myEnvs.putAll(envs);
  }

  public boolean isPassParentEnvs() {
    return myPassParentEnv;
  }

  public void setPassParentEnvs(boolean passParentEnv) {
    this.myPassParentEnv = passParentEnv;
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    myVMParameters = JDOMExternalizer.readString(element, "vmparams");
    myProgramParameters = JDOMExternalizer.readString(element, "cmdLine");

    String sPassParentEnvironment = JDOMExternalizer.readString(element, "passParentEnv");
    myPassParentEnv = StringUtil.isEmpty(sPassParentEnvironment) || Boolean.parseBoolean(sPassParentEnvironment);

    myEnvs.clear();
    JDOMExternalizer.readMap(element, myEnvs, null, "env");
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    JDOMExternalizer.write(element, "vmparams", myVMParameters);
    JDOMExternalizer.write(element, "cmdLine", myProgramParameters);
    JDOMExternalizer.writeMap(element, myEnvs, null, "env");
    JdomKt.addOptionTag(element, "passParentEnv", Boolean.toString(myPassParentEnv), "setting");
  }
}
