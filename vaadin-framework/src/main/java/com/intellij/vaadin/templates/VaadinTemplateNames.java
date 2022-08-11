package com.intellij.vaadin.templates;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VaadinTemplateNames {
  private final String myApplication;
  private final String myWidget;
  private final ClientWidgetClassTemplate[] myClientWidgets;
  private final String myCustomComponent;
  private final String myWidgetSetModule;

  public VaadinTemplateNames(@NotNull String application,
                             @NotNull String customComponent, @NotNull String widgetSetModule, String widget,
                             ClientWidgetClassTemplate... clientWidgets) {
    myApplication = application;
    myWidget = widget;
    myClientWidgets = clientWidgets;
    myCustomComponent = customComponent;
    myWidgetSetModule = widgetSetModule;
  }

  public String getApplication() {
    return myApplication;
  }

  public String getWidget() {
    return myWidget;
  }

  public ClientWidgetClassTemplate[] getClientWidgetClasses() {
    return myClientWidgets;
  }

  public String getCustomComponent() {
    return myCustomComponent;
  }

  public String getWidgetSetModule() {
    return myWidgetSetModule;
  }

  public List<String> getAllTemplates() {
    List<String> result = new ArrayList<>();
    result.add(myApplication);
    result.add(myWidget);
    for (ClientWidgetClassTemplate widget : myClientWidgets) {
      result.add(widget.getTemplateName());
    }
    result.add(myCustomComponent);
    result.add(myWidgetSetModule);
    return result;
  }

  public static final class ClientWidgetClassTemplate {
    private final String myNameSuffix;
    private final String myTemplateName;

    private ClientWidgetClassTemplate(String nameSuffix, String templateName) {
      myNameSuffix = nameSuffix;
      myTemplateName = templateName;

    }

    public static ClientWidgetClassTemplate clientWidget(@NotNull String nameSuffix, @NotNull String templateName) {
      return new ClientWidgetClassTemplate(nameSuffix, templateName);
    }

    public String getNameSuffix() {
      return myNameSuffix;
    }

    public String getTemplateName() {
      return myTemplateName;
    }
  }
}
