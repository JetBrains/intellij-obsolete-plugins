package com.intellij.gwt.inspections.eventListeners;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DeprecatedEventListenersRegistry {
  private final Map<String, DeprecatedListener> myListenersClass2Info;
  private final Map<String, DeprecatedListener> myListenerHolder2Info;
  private final Set<String> myAddRemoveMethodNames;
  private final Set<String> myEventMethodNames;

  public static DeprecatedEventListenersRegistry getInstance() {
    return ApplicationManager.getApplication().getService(DeprecatedEventListenersRegistry.class);
  }

  public DeprecatedEventListenersRegistry() {
    DeprecatedListener[] deprecatedListeners = {
        new DeprecatedListener("Click").handler("Click"),
        new DeprecatedListener("Scroll").handler("Scroll"),
        new DeprecatedListener("MouseWheel").handler("MouseWheel"),

        new DeprecatedListener("Focus", "com.google.gwt.user.client.ui.FocusListenerAdapter")
            .handler("Focus")
            .handler("onLostFocus", "onBlur", "com.google.gwt.event.dom.client.BlurHandler", "addBlurHandler"),

        new DeprecatedListener("Keyboard", "com.google.gwt.user.client.ui.KeyboardListenerAdapter")
            .handler("KeyDown").handler("KeyPress").handler("KeyUp"),

        new DeprecatedListener("Mouse", "com.google.gwt.user.client.ui.MouseListenerAdapter")
            .handler("MouseDown").handler("MouseUp").handler("MouseMove").handler("MouseEnter").handler("MouseMove")
    };

    myListenersClass2Info = new HashMap<>();
    myListenerHolder2Info = new HashMap<>();
    myAddRemoveMethodNames = new HashSet<>();
    myEventMethodNames = new HashSet<>();
    for (DeprecatedListener info : deprecatedListeners) {
      myListenersClass2Info.put(info.getListenerClass(), info);
      final String adapterClass = info.getAdapterClass();
      if (adapterClass != null) {
        myListenersClass2Info.put(adapterClass, info);
      }
      myListenerHolder2Info.put(info.getListenersHolderClass(), info);
      myAddRemoveMethodNames.add(info.getAddListenerMethod());
      myAddRemoveMethodNames.add(info.getRemoveListenerMethod());
      myEventMethodNames.addAll(info.getMethodNames());
    }
  }

  public boolean isDeprecatedListener(@Nullable String className) {
    return className != null && myListenersClass2Info.containsKey(className);
  }

  public @Nullable DeprecatedListener findByListenerClass(@Nullable String className) {
    return className != null ? myListenersClass2Info.get(className) : null;
  }

  public boolean isAddOrRemoveListenerMethodName(String name) {
    return name != null && myAddRemoveMethodNames.contains(name);
  }

  public @Nullable DeprecatedListener findByHolderClass(String className) {
    return myListenerHolder2Info.get(className);
  }

  public boolean isEventMethodName(@Nullable String name) {
    return name != null && myEventMethodNames.contains(name);
  }
}
