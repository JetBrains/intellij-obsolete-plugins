package com.intellij.restClient.php;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.restClient.RESTClient;
import com.intellij.restClient.RestClientCustomActionsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author andrey.sokolov
 */
public class PhpRestClientCustomActionsProvider implements RestClientCustomActionsProvider {

  @NotNull
  @Override
  public List<AnAction> getCustomActions(@NotNull RESTClient restClient) {
    List<AnAction> actions = new ArrayList<>();
    actions.add(new PhpSendRequestInDebugMode(restClient));
    return actions;
  }
}