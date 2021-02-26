package com.intellij.ws.rest.client.legacy;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.httpClient.execution.RestClientRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
@State(name = "RestClientLegacySettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE), reportStatistic = false)
public class RestClientSettings implements PersistentStateComponent<RestClientSettings> {
  private static final int REQUEST_HISTORY_SIZE = 50;

  public static RestClientSettings getInstance(Project project) {
    return ServiceManager.getService(project, RestClientSettings.class);
  }

  public List<RestClientRequest> REQUEST_HISTORY = new ArrayList<>();

  @Nullable
  @Override
  public RestClientSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull RestClientSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public void addToHistory(RestClientRequest request) {
    REQUEST_HISTORY.remove(request);
    while (REQUEST_HISTORY.size() > REQUEST_HISTORY_SIZE) {
      REQUEST_HISTORY.remove(REQUEST_HISTORY_SIZE);
    }
    REQUEST_HISTORY.add(0, request);
  }
}