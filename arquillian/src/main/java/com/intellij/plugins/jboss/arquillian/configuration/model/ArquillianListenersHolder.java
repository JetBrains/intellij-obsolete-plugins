package com.intellij.plugins.jboss.arquillian.configuration.model;

import com.intellij.util.Consumer;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;

public class ArquillianListenersHolder<Listener> {
  private final Set<Listener> listeners = new HashSet<>();

  public ListenerRemover addListener(final Listener listener) {
    listeners.add(listener);
    return new ListenerRemover() {
      @Override
      public void close() {
        listeners.remove(listener);
      }
    };
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  public void removeAllListeners() {
    listeners.clear();
  }

  public void notifyListeners(Consumer<Listener> action) {
    for (Listener listener : listeners) {
      action.consume(listener);
    }
  }

  interface ListenerRemover extends Closeable {
    @Override
    void close();
  }
}
