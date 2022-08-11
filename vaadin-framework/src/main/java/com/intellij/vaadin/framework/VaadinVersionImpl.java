package com.intellij.vaadin.framework;

import com.intellij.vaadin.templates.VaadinTemplateNames;
import org.jetbrains.annotations.NotNull;

import static com.intellij.vaadin.templates.VaadinTemplateNames.ClientWidgetClassTemplate.clientWidget;

public enum VaadinVersionImpl implements VaadinVersion {
  V6("Vaadin 6", new VaadinTemplateNames("Vaadin6Application.java", "VaadinCustomComponent.java", "Vaadin6WidgetSet.gwt.xml", "Vaadin6Widget.java",
                                         clientWidget("Widget", "Vaadin6ClientWidget.java")),
     "com.vaadin.terminal.gwt.DefaultWidgetSet",
     "com.vaadin.terminal.gwt.server.ApplicationServlet", "application"),

  V7_OR_LATER("Vaadin 7, 8", new VaadinTemplateNames("Vaadin7UI.java", "VaadinCustomComponent.java", "Vaadin7WidgetSet.gwt.xml", "Vaadin7Widget.java",
                                                     clientWidget("Widget", "Vaadin7ClientWidget.java"),
                                                     clientWidget("ClientRpc", "Vaadin7WidgetClientRpc.java"),
                                                     clientWidget("ServerRpc", "Vaadin7WidgetServerRpc.java"),
                                                     clientWidget("Connector", "Vaadin7WidgetConnector.java"),
                                                     clientWidget("State", "Vaadin7WidgetState.java")),
              "com.vaadin.DefaultWidgetSet",
              "com.vaadin.server.VaadinServlet", "UI");

  private final VaadinTemplateNames myTemplateNames;
  private final String myVersionName;
  private final String myWidgetSetModuleName;
  private final String myServletClass;
  private final String myApplicationServletParameterName;

  VaadinVersionImpl(String versionName, VaadinTemplateNames templateNames,
                    String widgetSetModuleName,
                    String servletClass, final String applicationServletParameterName) {
    myTemplateNames = templateNames;
    myVersionName = versionName;
    myWidgetSetModuleName = widgetSetModuleName;
    myServletClass = servletClass;
    myApplicationServletParameterName = applicationServletParameterName;
  }

  @Override
  @NotNull
  public String getApplicationServletParameterName() {
    return myApplicationServletParameterName;
  }

  @NotNull
  @Override
  public String getVersionName() {
    return myVersionName;
  }

  @NotNull
  @Override
  public String getWidgetSetModuleName() {
    return myWidgetSetModuleName;
  }

  @Override
  public boolean isFullDistributionRequired() {
    return this == V7_OR_LATER;
  }

  @NotNull
  @Override
  public VaadinTemplateNames getTemplateNames() {
    return myTemplateNames;
  }

  @Override
  @NotNull
  public String getServletClass() {
    return myServletClass;
  }
}
