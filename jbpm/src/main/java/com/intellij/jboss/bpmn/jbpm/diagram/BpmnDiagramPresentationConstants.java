package com.intellij.jboss.bpmn.jbpm.diagram;

import com.intellij.jboss.bpmn.jpdl.resources.messages.JpdlBundle;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.Gray;
import com.intellij.ui.LightColors;
import com.intellij.ui.SimpleColoredText;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.Nls;

import java.awt.*;

public final class BpmnDiagramPresentationConstants {

  public static final String LABEL_EXTENDS = "<<extends>>";
  public static final String LABEL_SUBFLOW_START = "<<Subflow Start>>";
  public static final String LABEL_ASSIGNMENT_OPERATOR = SystemInfo.isMac ? " \u21d2 " : " => ";
  public static final String LABEL_VIEW = "View";
  public static final String LABEL_MODEL = "Model";
  public static final String LABEL_SECURED = "Secured";
  public static final String LABEL_POPUP = "Popup";
  public static final String LABEL_COMMIT = "Commit";
  public static final Color EDGE_GLOBAL_TRANSITIONS_COLOR = Gray._80;
  public static final Color EDGE_EVENT_COLOR = LightColors.GREEN.darker();
  public static final Color EDGE_EXCEPTION_COLOR = new Color(220, 50, 50);
  public static final Color EDGE_PARENT_COLOR = LightColors.BLUE.darker();
  public static final Color EDGE_SUBFLOW_COLOR = new Color(180, 100, 50);
  public static final Color NODE_EVENT_COLOR = EDGE_EVENT_COLOR;
  public static final Color NODE_GLOBAL_TRANSITIONS_COLOR = Gray._220;

  private BpmnDiagramPresentationConstants() {
  }

  public static @Nls String getLabelInvalid() {
    return JpdlBundle.message("jpdl.label.invalid");
  }

  public static SimpleColoredText getInvalidSimpleColoredText() {
    return new SimpleColoredText(getLabelInvalid(), SimpleTextAttributes.ERROR_ATTRIBUTES);
  }
}
