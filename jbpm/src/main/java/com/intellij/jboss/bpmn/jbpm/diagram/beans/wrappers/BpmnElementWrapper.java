package com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers;

import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class BpmnElementWrapper<T> implements UserDataHolder {

  protected static final String DELIMITER = ";";
  protected static final String VALUE_DELIMITER = "=";
  private final UserDataHolderBase userDataHolderBase = new UserDataHolderBase();
  protected T myElement;

  protected BpmnElementWrapper(@NotNull T element) {
    myElement = element;
  }

  public T getElement() {
    return myElement;
  }

  @NotNull
  public abstract @Nls String getName();

  @Nullable
  public abstract String getFqn();

  @NotNull
  public abstract List<BpmnDomModel> getBpmnModels();

  @Nullable
  public Icon getIcon() {
    return null;
  }

  public boolean isValid() {
    return true;
  }

  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    return userDataHolderBase.getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    userDataHolderBase.putUserData(key, value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BpmnElementWrapper wrapper = (BpmnElementWrapper)o;

    if (!isValid() || !wrapper.isValid()) {
      return false;
    }

    if (!myElement.equals(wrapper.myElement)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return isValid() ? myElement.hashCode() : 0;
  }

  protected static Map<String, String> split(String fqn) {
    final List<String> strings = StringUtil.split(fqn, DELIMITER);
    Map<String, String> map = new HashMap<>();

    for (String string : strings) {
      final Pair<String, String> pair = getPair(string);
      if (pair != null) {
        map.put(pair.first, pair.second);
      }
    }
    return map;
  }

  @Nullable
  private static Pair<String, String> getPair(String str) {
    final int i = str.indexOf("=");
    if (i > 0 && i < str.length() - 1) {
      String key = str.substring(0, i).trim();
      String value = str.substring(i + 1).trim();
      if (!StringUtil.isEmptyOrSpaces(key) && !StringUtil.isEmptyOrSpaces(value)) {
        return Pair.create(key, value);
      }
    }
    return null;
  }
}
