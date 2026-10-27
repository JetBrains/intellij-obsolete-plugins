package com.intellij.guice.model;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.GuiceTestBase;
import com.intellij.guice.model.GuiceProjectModel.DiscoveryKind;
import com.intellij.guice.model.GuiceProjectModel.DiscoveryStep;
import com.intellij.openapi.application.ReadAction;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

/**
 * Checks the progress text of the discovery: every step carries a kind with a label, so the population
 * names its current phase instead of showing the text of the last index search.
 */
public class GuiceDiscoveryStepsTest extends GuiceTestBase {

  public void testEveryKindHasALabelForTheProgressText() {
    for (DiscoveryKind kind : DiscoveryKind.values()) {
      assertThat(kind.label()).isNotEmpty();
    }
    assertThat(GuiceBundle.message("progress.looking.for.kind", DiscoveryKind.MODULES.label()))
      .isEqualTo("Looking for modules\u2026");
    assertThat(GuiceBundle.message("progress.extracting.bindings", 1)).isEqualTo("Extracting bindings from 1 file\u2026");
    assertThat(GuiceBundle.message("progress.extracting.bindings", 350)).isEqualTo("Extracting bindings from 350 files\u2026");
  }

  public void testDiscoveryStepsCarryTheirKind() {
    GuiceProjectModel model = GuiceProjectModel.getInstance(getProject());
    Set<DiscoveryKind> kinds = EnumSet.noneOf(DiscoveryKind.class);
    List<DiscoveryStep> steps = ReadAction.compute(() -> model.createDiscoverySteps(getModule()));
    for (DiscoveryStep step : steps) {
      kinds.add(step.kind());
    }
    // Without binder-helper contributors every other kind has at least one step.
    assertThat(kinds).containsAtLeast(DiscoveryKind.INJECT_MEMBERS, DiscoveryKind.PROVIDES_METHODS,
                                      DiscoveryKind.JIT_CLASSES, DiscoveryKind.MODULES, DiscoveryKind.MODULE_REFERENCES);
  }
}
