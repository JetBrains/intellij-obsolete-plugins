/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.ui;

import com.intellij.ui.components.JBTabbedPane;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class SmartTabbedPane extends JBTabbedPane {

  private final HashMap tabs = new HashMap();

  public void addTab(String title, Icon icon, Component component, int virtualIndex) {
    tabs.put(new Integer(this.getComponentCount()), new Integer(virtualIndex));
    addTab(title, icon, component);
  }

  public int getVirtualIndex() {
    int i = getSelectedIndex();
    Integer v = (Integer)tabs.get(new Integer(i));
    return v == null ? i : v.intValue();
  }

  public void setVirtualIndex(int i) {
    Integer v = (Integer)tabs.get(new Integer(i));
    this.setSelectedIndex(v == null ? i : v.intValue());
  }
}
