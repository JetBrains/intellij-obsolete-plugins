package com.intellij.jboss.bpmn.jbpm.model.converters;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.jboss.bpmn.jbpm.BpmnUtils;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModelManager;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TBaseElement;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDefinitions;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowNode;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TRootElement;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ElementPresentation;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FlowElementConverter extends ResolvingConverter<TBaseElement> {

  @Override
  public TBaseElement fromString(@Nullable @NonNls final String s, final ConvertContext context) {
    if (s == null) return null;

    BpmnDomModel domModel = getDomModel(context);
    if (domModel == null) return null;

    final TDefinitions definitions = domModel.getDefinitions();
    List<TBaseElement> identifiedList = getAllFlowElements(definitions);
    for (TBaseElement identified : identifiedList) {
      if (s.equals(identified.getId().getStringValue())) {
        return identified;
      }
    }
    return null;
  }

  @Override
  public LookupElement createLookupElement(TBaseElement identified) {
    return createLookupElementImpl(identified);
  }

  @Override
  public String toString(@Nullable final TBaseElement identified, final ConvertContext context) {
    if (identified == null) return null;
    return identified.getId().getStringValue();
  }

  @Override
  @NotNull
  public Collection<TBaseElement> getVariants(final ConvertContext context) {
    final BpmnDomModel model = getDomModel(context);
    return model == null ? Collections.emptyList() : getAllFlowElements(model.getDefinitions());
  }

  public static LookupElement createLookupElementImpl(TBaseElement identified) {
    ElementPresentation elementPresentation = identified.getPresentation();
    final String id = identified.getId().getValue();
    final LookupElementBuilder builder = LookupElementBuilder.create(identified.getXmlTag(), StringUtil.notNullize(id))
      .withIcon(elementPresentation.getIcon());
    final String name = elementPresentation.getElementName();
    if (!StringUtil.isEmptyOrSpaces(name)) {
      return builder
        .withTailText(" " + name + " (" + identified.getXmlTag().getContainingFile().getName() + ")", true);
    }
    else {
      return builder
        .withTailText(" (" + identified.getXmlTag().getContainingFile().getName() + ")", true);
    }
  }

  @Nullable
  public static BpmnDomModel getDomModel(final ConvertContext context) {
    final XmlFile xmlFile = context.getFile();
    return BpmnDomModelManager.getInstance(xmlFile.getProject()).getModel(xmlFile);
  }

  @NotNull
  private static List<TBaseElement> getAllFlowElements(@Nullable final TDefinitions definitions) {
    if (definitions == null) return Collections.emptyList();

    List<TBaseElement> identifiedList = new ArrayList<>();

    addFlowElement(definitions, identifiedList);

    return identifiedList;
  }

  private static void addFlowElement(final TDefinitions definitions, final List<TBaseElement> identifiedList) {
    List<TRootElement> elements = definitions.getRootElements();
    BpmnUtils.processAllElements(elements, node -> {
      identifiedList.add(node);
      return true;
    }, TFlowNode.class);
    /*for (TProcess process : definitions.getProcesses()) {
      identifiedList.addAll(process.getFlowNodes());
    }*/
  }
}
