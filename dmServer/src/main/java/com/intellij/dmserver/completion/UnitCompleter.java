package com.intellij.dmserver.completion;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.dmserver.editor.AvailableBundlesProvider;
import com.intellij.dmserver.editor.ExportedUnit;
import com.intellij.dmserver.editor.UnitsCollector;
import com.intellij.dmserver.manifest.HeaderValuePartProcessor;
import org.jetbrains.lang.manifest.psi.HeaderValuePart;

public abstract class UnitCompleter implements HeaderValuePartProcessor<CompletionResultSet> {

  @Override
  public void process(HeaderValuePart headerValue, CompletionResultSet completionResultSet) {
    UnitsCollector unitsCollector = getUnitsCollector(AvailableBundlesProvider.getInstance(headerValue.getProject()));
    for (ExportedUnit availableUnit : unitsCollector.getAvailableUnits()) {
      completionResultSet.addElement(LookupElementBuilder.create(availableUnit.getSymbolicName()));
    }
    completionResultSet.stopHere();
  }

  protected abstract UnitsCollector getUnitsCollector(AvailableBundlesProvider provider);
}
