package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.JavaActivity;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JavaNode extends JpdlBasicNode<JavaActivity> {

  public JavaNode(JavaActivity identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.JAVA;
  }

  @Override
  public Icon getIcon() {
    return IconManager.getInstance().getPlatformIcon(com.intellij.ui.PlatformIcons.Class);
  }
}
