// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.sdk;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsConfigUtils;
import org.jetbrains.plugins.grails.config.GrailsConstants;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsSDKListener;
import org.jetbrains.plugins.grails.util.version.VersionImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@State(name = "GrailsSDKManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@Service(Service.Level.PROJECT)
public final class GrailsSDKManager implements PersistentStateComponent<GrailsSDKManager.StateHolder> {
  private final Map<String, String> myGrailsSDKs = new HashMap<>();

  public static @NotNull GrailsSDKManager getInstance(@NotNull Project project) {
    return project.getService(GrailsSDKManager.class);
  }

  public static @Nullable String getGrailsSdkPath(@Nullable GrailsApplication application) {
    if (application == null) return null;
    final String rootPath = application.getRoot().getCanonicalPath();
    return getInstance(application.getProject()).myGrailsSDKs.get(rootPath);
  }

  public static @Nullable GrailsSDK getGrailsSdk(@NotNull GrailsApplication application) {
    final String sdkPath = getGrailsSdkPath(application);
    if (sdkPath == null) return null;
    final String version = GrailsConfigUtils.getInstance().getSDKVersionOrNull(sdkPath);
    if (version == null) return null;
    return new GrailsSDK(sdkPath, new VersionImpl(version));
  }

  public static void setGrailsSDK(@NotNull GrailsApplication application, @Nullable String path) {
    if (StringUtil.isEmptyOrSpaces(path)) path = null;
    final String rootPath = application.getRoot().getCanonicalPath();
    if (rootPath != null) {
      String oldPath = getInstance(application.getProject()).setGrailsSDK(rootPath, path);
      if (!Objects.equals(path, oldPath)) {
        ApplicationManager.getApplication().executeOnPooledThread(
          () -> application.getProject().getMessageBus().syncPublisher(GrailsSDKListener.TOPIC).sdkChanged(application)
        );
      }
    }
  }

  public @Nullable String setGrailsSDK(@NotNull String rootPath, @Nullable String path) {
    PropertiesComponent.getInstance().setValue(GrailsConstants.GRAILS_LAST_SELECTED_SDK, path);
    return myGrailsSDKs.put(rootPath, path);
  }

  @Override
  public synchronized @NotNull StateHolder getState() {
    StateHolder holder = new StateHolder();
    holder.grailsSDKs.putAll(myGrailsSDKs);
    return holder;
  }

  @Override
  public void loadState(@NotNull StateHolder state) {
    myGrailsSDKs.clear();
    myGrailsSDKs.putAll(state.grailsSDKs);
  }

  public static final class StateHolder {
    public Map<String, String> grailsSDKs = new HashMap<>();
  }
}
