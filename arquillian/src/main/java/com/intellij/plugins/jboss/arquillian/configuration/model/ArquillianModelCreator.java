package com.intellij.plugins.jboss.arquillian.configuration.model;

import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianState;

public interface ArquillianModelCreator<State extends ArquillianState, Model extends ArquillianModel<State, Model>> {
  Model createModel(State state);
}
