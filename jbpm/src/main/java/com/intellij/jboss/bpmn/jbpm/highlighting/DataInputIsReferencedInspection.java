package com.intellij.jboss.bpmn.jbpm.highlighting;

import com.intellij.jboss.bpmn.jbpm.BpmnBundle;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDataInput;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TInputOutputSpecification;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TInputSet;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomHighlightingHelper;

import java.util.List;

public class DataInputIsReferencedInspection {
  private final DomElement myElement;
  private final DomElementAnnotationHolder myHolder;
  private final DomHighlightingHelper myHelper;

  public DataInputIsReferencedInspection(DomElement element, DomElementAnnotationHolder holder, DomHighlightingHelper helper) {
    myElement = element;
    myHolder = holder;
    myHelper = helper;
  }

  public void check() {
    if (!myElement.isValid() || !((TDataInput)myElement).getId().isValid()) return;
    final DomElement ioSpecDomElement = myElement.getParent();
    if (ioSpecDomElement == null || !TInputOutputSpecification.class.equals(ioSpecDomElement.getDomElementType())) return;

    final List<TInputSet> sets = ((TInputOutputSpecification)ioSpecDomElement).getInputSets();
    for (TInputSet set : sets) {
      final List<GenericDomValue<TDataInput>> refses = set.getDataInputRefses();
      for (GenericDomValue<TDataInput> refse : refses) {
        if (!refse.isValid()) continue;
        final TDataInput input = refse.getValue();
        if (input != null && input.equals(myElement)) return;
      }
    }
    //no references
    myHolder.createProblem(myElement, HighlightSeverity.ERROR, BpmnBundle.message("should.be.referenced.by.any.datainputrefs"));
  }
}
