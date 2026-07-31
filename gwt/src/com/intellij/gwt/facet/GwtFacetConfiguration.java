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

package com.intellij.gwt.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkManager;
import com.intellij.gwt.sdk.GwtSdkType;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.model.GwtJavaScriptOutputStyle;
import org.jetbrains.jps.gwt.model.impl.GwtModuleExtensionProperties;
import org.jetbrains.jps.gwt.model.impl.GwtModulePackagingProperties;
import org.jetbrains.jps.gwt.model.impl.JpsGwtModuleExtensionImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GwtFacetConfiguration implements FacetConfiguration, PersistentStateComponent<GwtModuleExtensionProperties> {
  private GwtModuleExtensionProperties myState = new GwtModuleExtensionProperties();
  private final Map<String, GwtModulePackagingProperties> myPackagingStates = new HashMap<>();

  @Override
  public FacetEditorTab[] createEditorTabs(final FacetEditorContext editorContext, final FacetValidatorsManager validatorsManager) {
    return new FacetEditorTab[]{new GwtFacetEditor(editorContext, validatorsManager, this)};
  }

  @Override
  public void loadState(final @NotNull GwtModuleExtensionProperties state) {
    myState = state;
    myPackagingStates.clear();
    for (GwtModulePackagingProperties packagingState : state.myPackagingStates) {
      myPackagingStates.put(packagingState.myName, packagingState);
    }
  }

  @Override
  public GwtModuleExtensionProperties getState() {
    myState.myPackagingStates.clear();
    String[] names = ArrayUtilRt.toStringArray(myPackagingStates.keySet());
    Arrays.sort(names);
    for (String name : names) {
      GwtModulePackagingProperties packagingState = myPackagingStates.get(name);
      if (packagingState.myPath != null || !packagingState.myEnabled) {
        myState.myPackagingStates.add(packagingState);
      }
    }
    return myState;
  }

  public String getGwtSdkPath() {
    return FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(myState.mySdkUrl));
  }

  public void setGwtSdkUrlAndType(String gwtUrl) {
    setGwtSdkUrl(gwtUrl);
    GwtSdkType type = GwtSdkManager.getInstance().detectSdkType(myState.mySdkUrl);
    setGwtSdkType(type != null ? type.getId() : null);
  }

  public void setGwtSdkUrl(final String gwtUrl) {
    myState.mySdkUrl = gwtUrl;
  }

  public @NotNull GwtSdk getSdk() {
    return GwtSdkManager.getInstance().getGwtSdk(getGwtSdkUrl(), getGwtSdkType());
  }

  public String getGwtSdkUrl() {
    return myState.mySdkUrl;
  }

  public String getGwtSdkType() {
    return myState.mySdkType;
  }

  public void setGwtSdkType(String gwtSdkType) {
    myState.mySdkType = gwtSdkType;
  }

  public String getCompilerParameters() {
    return myState.myCompilerParameters;
  }

  public void setCompilerParameters(String compilerParameters) {
    myState.myCompilerParameters = compilerParameters;
  }

  public String getAdditionalCompilerVMParameters() {
    return myState.myAdditionalCompilerVMParameters;
  }

  public void setAdditionalCompilerVMParameters(final String additionalCompilerVMParameters) {
    myState.myAdditionalCompilerVMParameters = additionalCompilerVMParameters;
  }

  public int getCompilerMaxHeapSize() {
    return myState.myCompilerMaxHeapSize > 0 ? myState.myCompilerMaxHeapSize : JpsGwtModuleExtensionImpl.DEFAULT_COMPILER_HEAP_SIZE;
  }

  public void setCompilerMaxHeapSize(final int compilerMaxHeapSize) {
    myState.myCompilerMaxHeapSize = compilerMaxHeapSize;
  }

  public @NotNull GwtJavaScriptOutputStyle getOutputStyle() {
    return myState.myOutputStyle != null ? myState.myOutputStyle : GwtJavaScriptOutputStyle.DETAILED;
  }

  public void setOutputStyle(final GwtJavaScriptOutputStyle outputStyle) {
    myState.myOutputStyle = outputStyle;
  }

  public void setWebFacetName(final String webFacetName) {
    myState.myWebFacetName = webFacetName;
  }

  public @Nullable @NlsSafe String getWebFacetName() {
    return myState.myWebFacetName;
  }

  public @NotNull String getPackagingRelativePath(@NotNull GwtModule module) {
    GwtModulePackagingProperties state = myPackagingStates.get(module.getQualifiedName());
    if (state != null && state.myPath != null) {
      return state.myPath;
    }
    return getDefaultPackagingPath(module);
  }

  public boolean isModuleCompilationEnabled(@NotNull GwtModule module) {
    GwtModulePackagingProperties state = myPackagingStates.get(module.getQualifiedName());
    return state == null || state.myEnabled;
  }

  public static String getDefaultPackagingPath(GwtModule module) {
    return "/" + module.getOutputName();
  }

  public void setPackagingRelativePath(@NotNull String moduleName, @Nullable String path) {
    getOrCreateState(moduleName).myPath = path;
  }

  public void setModuleCompilationEnabled(@NotNull String moduleName, boolean enable) {
    getOrCreateState(moduleName).myEnabled = enable;
  }

  private GwtModulePackagingProperties getOrCreateState(String moduleName) {
    GwtModulePackagingProperties state = myPackagingStates.get(moduleName);
    if (state == null) {
      state = new GwtModulePackagingProperties();
      state.myName = moduleName;
      myPackagingStates.put(moduleName, state);
    }
    return state;
  }

  public @Nullable String getClientLanguageLevelString() {
    return myState.getClientLanguageLevel();
  }

  public @NotNull LanguageLevel getClientLanguageLevel() {
    String sourceLevelStr = getClientLanguageLevelString();
    LanguageLevel languageLevel = null;
    if (sourceLevelStr != null) {
      languageLevel = LanguageLevel.parse(sourceLevelStr);
    }
    return languageLevel == null ? getSdk().getVersion().getHighestSupportedLanguageLevel() : languageLevel;
  }
}
