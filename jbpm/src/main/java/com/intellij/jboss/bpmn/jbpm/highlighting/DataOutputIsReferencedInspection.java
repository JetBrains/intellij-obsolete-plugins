package com.intellij.jboss.bpmn.jbpm.highlighting;

import com.intellij.jboss.bpmn.jbpm.BpmnBundle;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDataOutput;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TInputOutputSpecification;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TOutputSet;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomHighlightingHelper;

import java.util.List;

public class DataOutputIsReferencedInspection {
  private final DomElement myElement;
  private final DomElementAnnotationHolder myHolder;
  private final DomHighlightingHelper myHelper;

  public DataOutputIsReferencedInspection(DomElement element, DomElementAnnotationHolder holder, DomHighlightingHelper helper) {
    myElement = element;
    myHolder = holder;
    myHelper = helper;
  }

  public void check() {
    if (!myElement.isValid() || !((TDataOutput)myElement).getId().isValid()) return;
    final DomElement ioSpecDomElement = myElement.getParent();
    if (ioSpecDomElement == null || !TInputOutputSpecification.class.equals(ioSpecDomElement.getDomElementType())) return;

    final List<TOutputSet> sets = ((TInputOutputSpecification)ioSpecDomElement).getOutputSets();
    for (TOutputSet set : sets) {
      final List<GenericDomValue<TDataOutput>> refses = set.getDataOutputRefses();
      for (GenericDomValue<TDataOutput> refse : refses) {
        if (!refse.isValid()) continue;
        final TDataOutput input = refse.getValue();
        if (input != null && input.equals(myElement)) return;
      }
    }
    //no references
    myHolder.createProblem(myElement, HighlightSeverity.ERROR, BpmnBundle.message("should.be.referenced.by.any.dataoutputrefs"));
  }
}
