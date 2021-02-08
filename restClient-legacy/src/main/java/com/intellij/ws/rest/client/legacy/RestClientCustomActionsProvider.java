package com.intellij.ws.rest.client.legacy;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface RestClientCustomActionsProvider {
  ExtensionPointName<RestClientCustomActionsProvider> EP_NAME = ExtensionPointName.create("com.intellij.restClient.legacy.customRequestActions");

  @NotNull
  List<AnAction> getCustomActions(@NotNull RESTClient restClient);
}