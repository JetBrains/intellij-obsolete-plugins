/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.jboss.bpmn.jbpm.highlighting;

import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDataInput;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDataOutput;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDefinitions;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomHighlightingHelper;
import org.jetbrains.annotations.NotNull;

public class BpmnConfigDomInspection extends BasicDomElementsInspection<TDefinitions> {

  public BpmnConfigDomInspection() {
    super(TDefinitions.class);
  }

  @Override
  protected void checkDomElement(@NotNull DomElement element, @NotNull DomElementAnnotationHolder holder, @NotNull DomHighlightingHelper helper) {
    super.checkDomElement(element, holder, helper);
    if (TDataInput.class.equals(element.getDomElementType())) {
      new DataInputIsReferencedInspection(element, holder, helper).check();
    }
    if (TDataOutput.class.equals(element.getDomElementType())) {
      new DataOutputIsReferencedInspection(element, holder, helper).check();
    }
  }
}
