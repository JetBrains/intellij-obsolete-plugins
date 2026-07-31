/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.gwt.run;

import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.CantRunException;
import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GwtRunConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule, Element>
    implements CommonProgramRunConfigurationParameters {
  private static final SkipDefaultValuesSerializationFilters SERIALIZATION_FILTERS = new SkipDefaultValuesSerializationFilters();
  private static final @NonNls String MODULE = "module";
  private static final @NonNls String PAGE = "page";
  private GwtRunConfigurationState myState = new GwtRunConfigurationState();

  public GwtRunConfiguration(String name, Project project, @NotNull ConfigurationFactory configurationFactory) {
    super(name, new JavaRunConfigurationModule(project, true), configurationFactory);
  }

  public GwtRunConfiguration(Project project, @NotNull ConfigurationFactory configurationFactory) {
    this(GwtBundle.message("default.gwt.run.configuration.name"), project, configurationFactory);
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    SettingsEditorGroup<GwtRunConfiguration> group = new SettingsEditorGroup<>();
    group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"), new GwtRunConfigurationEditor(getProject()));
    group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel<>());

    JavaRunConfigurationExtensionManager.getInstance().appendEditors(this, group);
    return group;
  }

  @Override
  public RunProfileState getState(final @NotNull Executor executor, final @NotNull ExecutionEnvironment env) throws ExecutionException {
    final Module module = getModule();
    if (module == null) {
      throw CantRunException.noModuleConfigured(getConfigurationModule().getModuleName());
    }

    GwtFacet facet = FacetManager.getInstance(module).getFacetByType(GwtFacetType.ID);
    if (facet == null) {
      List<GwtFacet> facets = new SmartList<>();
      OrderEnumerator.orderEntries(module).forEachModule(m -> {
        GwtFacet f = FacetManager.getInstance(m).getFacetByType(GwtFacetType.ID);
        ContainerUtil.addIfNotNull(facets, f);
        return f == null;
      });
      if (facets.isEmpty()) {
        throw new ExecutionException(GwtBundle.message("error.text.gwt.facet.not.configured.in.module.0", module.getName()));
      }
      facet = facets.get(0);
    }


    final Sdk jdk = ModuleRootManager.getInstance(module).getSdk();
    if (jdk == null) {
      throw CantRunException.noJdkForModule(module);
    }

    if (facet.getSdkVersion().isModulesToLoadSpecifiedInDevMode()) {
      final Collection<GwtModule> gwtModules = GwtModulesManager.getInstance(module.getProject()).getCompilableGwtModules(facet.getModule(), false);
      if (gwtModules.isEmpty()) {
        throw new ExecutionException(GwtBundle.message("error.text.no.gwt.modules.in.module.0", facet.getModule().getName()));
      }
    }

    GwtSdk sdk = facet.getConfiguration().getSdk();
    if (!sdk.isValid()) {
      throw new ExecutionException(GwtBundle.message("error.text.gwt.sdk.is.not.specified.correctly", module.getName()));
    }

    GwtDevModeServer selectedServer = null;
    for (GwtDevModeServerProvider provider : GwtDevModeServerProvider.EP_NAME.getExtensions()) {
      for (GwtDevModeServer server : provider.getServers()) {
        if (server.getId().equals(myState.SERVER_ID)) {
          selectedServer = server;
          break;
        }
      }
    }
    if (selectedServer == null) {
      throw new ExecutionException(GwtBundle.message("error.message.unknown.gwt.dev.mode.server.0", myState.SERVER_ID));
    }

    return new GwtCommandLineState(module, facet, env, this, selectedServer, executor);
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    getConfigurationModule().checkForWarning();
  }

  public GwtRunConfigurationState getGwtState() {
    return myState;
  }

  @Override
  public Collection<Module> getValidModules() {
    return getAllModules();
  }

  public @Nullable Module getModule() {
    return getConfigurationModule().getModule();
  }

  @Override
  public boolean isModuleDirMacroSupported() {
    return true;
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    myState = new GwtRunConfigurationState();
    XmlSerializer.deserializeInto(myState, element);
    readModule(element);
    Element module = element.getChild(MODULE);
    if (module != null) {
      final String page = module.getAttributeValue(PAGE);
      if (!StringUtil.isEmpty(page)) {
        myState.RUN_PAGE = page;
      }
    }
    super.readExternal(element);
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    XmlSerializer.serializeInto(myState, element, SERIALIZATION_FILTERS);
    super.writeExternal(element);
  }

  public void setPage(String runPage) {
    if (runPage == null) {
      runPage = "";
    }
    myState.RUN_PAGE = runPage;
  }

  public @NlsSafe String getPage() {
    return myState.RUN_PAGE;
  }

  @Override
  public void setProgramParameters(@Nullable String value) {
    myState.SHELL_PARAMETERS = value;
  }

  @Override
  public @Nullable String getProgramParameters() {
    return myState.SHELL_PARAMETERS;
  }

  @Override
  public void setWorkingDirectory(@Nullable String value) {
    myState.WORKING_DIRECTORY = value;
  }

  @Override
  public @Nullable String getWorkingDirectory() {
    return myState.WORKING_DIRECTORY;
  }

  @Override
  public void setEnvs(@NotNull Map<String, String> envs) {
    myState.ENVIRONMENT_VARIABLES = envs;
  }

  @Override
  public @NotNull Map<String, String> getEnvs() {
    return myState.ENVIRONMENT_VARIABLES;
  }

  @Override
  public void setPassParentEnvs(boolean passParentEnvs) {
    myState.PASS_PARENT_ENVS = passParentEnvs;
  }

  @Override
  public boolean isPassParentEnvs() {
    return myState.PASS_PARENT_ENVS;
  }

  public static class GwtRunConfigurationState {
    public @NonNls String VM_PARAMETERS = "-Xmx512m";
    public String SHELL_PARAMETERS = "";
    public String RUN_PAGE = "";
    public String CUSTOM_WEB_XML;
    public String GWT_MODULE;
    public String SERVER_ID = DefaultDevModeServer.SERVER_ID;
    public boolean OPEN_IN_BROWSER = true;
    public boolean START_JAVASCRIPT_DEBUGGER = false;
    public String BROWSER;
    public boolean UPDATE_RESOURCES_ON_FRAME_DEACTIVATION;
    public boolean USE_SUPER_DEV_MODE = false;
    public boolean PASS_PARENT_ENVS = true;
    public String ALTERNATIVE_JRE_PATH;
    public String WORKING_DIRECTORY;

    @Tag("envs")
    @MapAnnotation(surroundWithTag = false, surroundKeyWithTag = false, surroundValueWithTag = false, entryTagName = "env", keyAttributeName = "name")
    public Map<String, String> ENVIRONMENT_VARIABLES = new HashMap<>();

    @XCollection(propertyElementName = "gwt-modules", elementName = "gwt-module", valueAttributeName = "name")
    public List<String> GWT_MODULES = new ArrayList<>();

    @Transient
    public @Nullable List<String> getGwtModules() {
      if (!GWT_MODULES.isEmpty()) {
        return GWT_MODULES;
      }
      if (GWT_MODULE != null) {
        return Collections.singletonList(GWT_MODULE);
      }
      return null;
    }

    @Transient
    public void setGwtModules(@Nullable List<String> gwtModules) {
      GWT_MODULES = Collections.emptyList();
      GWT_MODULE = null;
      if (gwtModules != null) {
        if (gwtModules.size() == 1) {
          GWT_MODULE = gwtModules.get(0);
        }
        else {
          GWT_MODULES = Collections.unmodifiableList(new ArrayList<>(gwtModules));
        }
      }
    }
  }

  @Override
  public void onNewConfigurationCreated() {
    List<GwtFacet> facets = ProjectFacetManager.getInstance(getProject()).getFacets(GwtFacetType.ID);
    for (GwtFacet facet : facets) {
      if (facet.getSdkVersion().isSuperDevModeUsedByDefault()) {
        getGwtState().USE_SUPER_DEV_MODE = true;
        break;
      }
    }
  }
}
