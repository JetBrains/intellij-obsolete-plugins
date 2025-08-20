package com.intellij.jboss.bpmn.jbpm.model.converters;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.jboss.bpmn.jbpm.BpmnUtils;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TBaseElement;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDefinitions;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class TBaseElementConverter extends ResolvingConverter<TBaseElement> {
  /**
   * @param context context
   * @return reference completion variants
   */
  @NotNull
  @Override
  public Collection<? extends TBaseElement> getVariants(ConvertContext context) {
    final BpmnDomModel model = FlowElementConverter.getDomModel(context);
    return model == null ? Collections.emptyList() : getBaseElements(model);
  }

  @Override
  public TBaseElement fromString(@Nullable @NonNls String s, ConvertContext context) {
    if (s == null) return null;

    BpmnDomModel domModel = FlowElementConverter.getDomModel(context);
    if (domModel == null) return null;

    final List<TBaseElement> baseElements = getBaseElements(domModel);
    for (TBaseElement element : baseElements) {
      if (s.equals(element.getId().getStringValue())) {
        return element;
      }
    }
    return null;
  }

  protected abstract Set<String> possiblyReferencedTypes();

  private List<TBaseElement> getBaseElements(BpmnDomModel domModel) {
    final Set<String> tags = possiblyReferencedTypes();
    final TDefinitions definitions = domModel.getDefinitions();
    final List<TBaseElement> result = new ArrayList<>();
    BpmnUtils.processAllElements(Collections.singletonList(definitions), child -> {
      final XmlTag tag = child.getXmlTag();
      final String localName = tag.getLocalName();
      if (tags == null || tags.contains(localName)) {
        result.add(child);
      }
      return true;
    });
    return result;
  }

  @Override
  public String toString(@Nullable TBaseElement element, ConvertContext context) {
    if (element == null) return null;
    return element.getId().getStringValue();
  }

  /**
   * Override to provide custom lookup elements in completion.
   * <p/>
   * Default is {@code null} which will create lookup via
   * {@link com.intellij.util.xml.ElementPresentationManager#createVariant(Object, String, com.intellij.psi.PsiElement)}.
   *
   * @param identified DOM to create lookup element for.
   * @return Lookup element.
   */
  @Override
  public LookupElement createLookupElement(TBaseElement identified) {
    return FlowElementConverter.createLookupElementImpl(identified);
  }

  public static class AnyBaseElementConverter extends TBaseElementConverter {
    @Override
    protected Set<String> possiblyReferencedTypes() {
      return null;
    }
  }
}
