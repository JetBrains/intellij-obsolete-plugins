package com.intellij.gwt.inspections.eventListeners;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.inspections.BaseGwtInspection;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class GwtDeprecatedEventListenersInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (file.getFileType() != JavaFileType.INSTANCE) return null;

    final GwtFacet gwtFacet = getFacet(file);
    if (gwtFacet == null || !gwtFacet.getSdkVersion().isEventHandlersSupported()) {
      return null;
    }

    final EventListenersUsagesCollector collector = new EventListenersUsagesCollector();
    file.accept(collector);
    final DeprecatedListenerUsages usages = collector.getListenerUsages();

    List<ProblemDescriptor> problems = new ArrayList<>();
    for (ListenerUsageInfo usageInfo : usages.getUsages()) {
      if (usageInfo.canBeFixed(usages)) {
        problems.add(usageInfo.createProblemDescriptor(manager, isOnTheFly));
      }
    }

    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  @Override
  public @NotNull HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }
}
