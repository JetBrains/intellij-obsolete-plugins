package com.intellij.jboss.bpmn.jbpm.layout;

import com.intellij.openapi.command.undo.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MapUndoableWrapper<K, V> implements Map<K, V> {
  @NotNull private final UndoManager undoManager;
  private final DocumentReference @NotNull [] documentReferences;
  @NotNull private final Map<K, V> underlyingMap;

  public MapUndoableWrapper(@NotNull UndoManager manager, DocumentReference @NotNull [] references, @NotNull Map<K, V> map) {
    undoManager = manager;
    documentReferences = references;
    underlyingMap = map;
  }

  @Override
  public int size() {
    return underlyingMap.size();
  }

  @Override
  public boolean isEmpty() {
    return underlyingMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return underlyingMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return underlyingMap.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return underlyingMap.get(key);
  }

  @Override
  public V put(final K key, final V value) {
    final V oldValue = underlyingMap.put(key, value);
    undoManager.undoableActionPerformed(new BasicUndoableAction() {
      @Override
      public void undo() throws UnexpectedUndoException {
        if (oldValue != null) {
          underlyingMap.put(key, oldValue);
        }
        else {
          underlyingMap.remove(key);
        }
      }

      @Override
      public void redo() throws UnexpectedUndoException {
        underlyingMap.put(key, value);
      }
    });
    return oldValue;
  }

  @SuppressWarnings("SuspiciousMethodCalls")
  @Override
  public V remove(final Object key) {
    final V oldValue = underlyingMap.remove(key);
    undoManager.undoableActionPerformed(new Undoable() {
      @Override
      public void undo() throws UnexpectedUndoException {
        if (oldValue != null) {
          //noinspection unchecked
          underlyingMap.put((K)key, oldValue);
        }
      }

      @Override
      public void redo() throws UnexpectedUndoException {
        underlyingMap.remove(key);
      }
    });
    return oldValue;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    ArrayList<K> keys = new ArrayList<>(underlyingMap.keySet());
    for (K key : keys) {
      remove(key);
    }
  }

  @NotNull
  @Override
  public Set<K> keySet() {
    return underlyingMap.keySet();
  }

  @NotNull
  @Override
  public Collection<V> values() {
    return underlyingMap.values();
  }

  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return underlyingMap.entrySet();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MapUndoableWrapper<?, ?> wrapper = (MapUndoableWrapper<?, ?>)o;

    return underlyingMap.equals(wrapper.underlyingMap);
  }

  @Override
  public int hashCode() {
    return underlyingMap.hashCode();
  }

  private abstract class Undoable extends BasicUndoableAction implements UndoableAction {
    private long performedTimestamp = -1L;

    @Override
    public final DocumentReference @Nullable [] getAffectedDocuments() {
      return documentReferences;
    }

    @Override
    public final boolean isGlobal() {
      return false;
    }
  }
}
