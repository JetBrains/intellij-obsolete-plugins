package com.intellij.gwt.run;

import com.intellij.gwt.run.GwtRunConfiguration.GwtRunConfigurationState;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

public class GwtRunConfigurationStateTest extends TestCase {

  public void testGwtModules() {
    GwtRunConfigurationState state = new GwtRunConfigurationState();

    List<String> gwtModules = Arrays.asList("Module1", "Module2");
    state.GWT_MODULES.addAll(gwtModules);
    assertEquals(gwtModules, state.getGwtModules());

    state.setGwtModules(gwtModules);

    List<String> stateGwtModules = state.getGwtModules();
    assertEquals(gwtModules, stateGwtModules);

    state.setGwtModules(stateGwtModules);
    assertEquals(gwtModules, state.getGwtModules());
  }
}