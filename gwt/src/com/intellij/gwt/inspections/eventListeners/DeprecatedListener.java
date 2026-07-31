package com.intellij.gwt.inspections.eventListeners;

import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class DeprecatedListener {
  private static final @NonNls String DOM_PACKAGE = "com.google.gwt.event.dom.client.";
  private static final @NonNls String UI_PACKAGE = "com.google.gwt.user.client.ui.";
  private final @NonNls String myListenerClass;
  private final @NonNls String myAdapterClass;
  private @NonNls String myAddListenerMethod;
  private @NonNls String myRemoveListenerMethod;
  private @NonNls String myListenersHolderClass;
  private final Map<String, EventHandlerInfo> myHandlers = new HashMap<>();

  DeprecatedListener(@NonNls String eventName) {
    this(eventName, null);
  }

  DeprecatedListener(@NonNls String eventName, @NonNls String adapterClass) {
    myListenerClass = UI_PACKAGE + eventName + "Listener";
    myAdapterClass = adapterClass;
    listenersHolder(UI_PACKAGE + "Sources" + eventName + "Events", "add" + eventName + "Listener", "remove" + eventName + "Listener");
  }

  public DeprecatedListener handler(@NonNls String handlerName) {
    return handler("on" + handlerName, DOM_PACKAGE + handlerName + "Handler", "add" + handlerName + "Handler");
  }

  public DeprecatedListener handler(@NonNls String methodName, @NonNls String handlerClass, @NonNls String addHandlerMethod) {
    return handler(methodName, methodName, handlerClass, addHandlerMethod);
  }

  public DeprecatedListener handler(@NonNls String listenerMethodName, @NonNls String handlerMethodName, @NonNls String handlerClass,
                                    @NonNls String addHandlerMethod) {
    myHandlers.put(listenerMethodName, new EventHandlerInfo(handlerMethodName, handlerClass, addHandlerMethod));
    return this;
  }

  public DeprecatedListener listenersHolder(@NonNls String listenersHolderClass, @NonNls String addMethodName, @NonNls String removeMethodName) {
    myListenersHolderClass = listenersHolderClass;
    myAddListenerMethod = addMethodName;
    myRemoveListenerMethod = removeMethodName;
    return this;
  }

  public String getListenerClass() {
    return myListenerClass;
  }

  public String getAdapterClass() {
    return myAdapterClass;
  }

  public @Nullable String getHandlerClassIfSingle() {
    return myHandlers.size() == 1 ? myHandlers.values().iterator().next().getHandlerClass() : null;
  }

  public String getAddListenerMethod() {
    return myAddListenerMethod;
  }

  public String getRemoveListenerMethod() {
    return myRemoveListenerMethod;
  }

  public String getListenersHolderClass() {
    return myListenersHolderClass;
  }

  public Collection<String> getMethodNames() {
    return myHandlers.keySet();
  }

  public EventHandlerInfo getHandler(String methodName) {
    return myHandlers.get(methodName);
  }

  public boolean isListenerOrAdapterType(final PsiType type) {
    return type.equalsToText(myListenerClass) || myAdapterClass != null && type.equalsToText(myAdapterClass);
  }
}
