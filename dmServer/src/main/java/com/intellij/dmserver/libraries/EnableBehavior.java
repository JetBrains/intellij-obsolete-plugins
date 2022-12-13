package com.intellij.dmserver.libraries;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class EnableBehavior {

  private final Map<JComponent, EnableStateEvaluator> myEnableStateEvaluators = new HashMap<>();

  public void addComponent(JComponent component, EnableStateEvaluator stateEvaluator) {
    myEnableStateEvaluators.put(component, stateEvaluator);
  }

  public void updateEnableState(JComponent component) {
    component.setEnabled(myEnableStateEvaluators.get(component).isEnabled());
  }

  public void updateAllEnableStates() {
    for (JComponent component : myEnableStateEvaluators.keySet()) {
      updateEnableState(component);
    }
  }

  public void setAllEnabled(boolean enabled) {
    for (JComponent component : myEnableStateEvaluators.keySet()) {
      component.setEnabled(enabled);
    }
  }
}
