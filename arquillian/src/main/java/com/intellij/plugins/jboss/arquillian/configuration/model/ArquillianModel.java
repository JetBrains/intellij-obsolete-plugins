package com.intellij.plugins.jboss.arquillian.configuration.model;

import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianState;

public abstract class ArquillianModel<State extends ArquillianState, Model extends ArquillianModel<State, Model>> {
  private final ArquillianListenersHolder<ArquillianModel.Listener<State, Model>> listenersHolder
    = new ArquillianListenersHolder<>();

  private int stateVersion = 0;

  public int getStateVersion() {
    return stateVersion;
  }

  public abstract boolean hasChanges(State state);

  public abstract State getCurrentState();

  public ArquillianListenersHolder.ListenerRemover addChangeListener(ArquillianModel.Listener<State, Model> listener) {
    return listenersHolder.addListener(listener);
  }

  protected void notifyMeChanged() {
    ++stateVersion;
    listenersHolder.notifyListeners(listener -> {
      //noinspection unchecked
      listener.itemChanged((Model)this);
    });
  }

  public interface Listener<State extends ArquillianState, Model extends ArquillianModel<State, Model>> {
    void itemChanged(Model model);
  }
}
