package com.intellij.plugins.jboss.arquillian.configuration.persistent;

import java.util.List;

public interface ArquillianListState<State extends ArquillianState> extends ArquillianState {
  List<State> getChildren();
}
