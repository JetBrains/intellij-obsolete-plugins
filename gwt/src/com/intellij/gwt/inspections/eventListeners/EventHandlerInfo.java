package com.intellij.gwt.inspections.eventListeners;

class EventHandlerInfo {
  private final String myHandlerMethod;
  private final String myHandlerClass;
  private final String myAddHandlerMethod;

  EventHandlerInfo(String handlerMethod, String handlerClass, String addHandlerMethod) {
    myHandlerMethod = handlerMethod;
    myHandlerClass = handlerClass;
    myAddHandlerMethod = addHandlerMethod;
  }

  public String getHandlerMethod() {
    return myHandlerMethod;
  }

  public String getHandlerClass() {
    return myHandlerClass;
  }

  public String getAddHandlerMethod() {
    return myAddHandlerMethod;
  }
}
