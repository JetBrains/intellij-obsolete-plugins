package com.intellij.jboss.bpmn.jbpm.diagram.managers;

import com.intellij.diagram.AbstractDiagramElementManager;
import com.intellij.diagram.presentation.DiagramState;
import com.intellij.jboss.bpmn.jbpm.diagram.BpmnDiagramPresentationConstants;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.*;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModelManager;
import com.intellij.jboss.bpmn.jpdl.resources.messages.JpdlBundle;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiFile;
import com.intellij.psi.jsp.JspFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.SimpleColoredText;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

public final class BpmnDiagramElementManager extends AbstractDiagramElementManager<BpmnElementWrapper<?>> {
  @Override
  public @Nullable BpmnElementWrapper findInDataContext(@NotNull DataContext context) {
    final PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(context);
    if (psiFile instanceof XmlFile &&
        !(psiFile instanceof JspFile)) {
      if (BpmnDomModelManager.getInstance(psiFile.getProject()).isBpmnDomModel((XmlFile)psiFile)) {
        return new BpmnDefinitionsWrapper((XmlFile)psiFile);
      }
    }

    final Module module = PlatformCoreDataKeys.MODULE.getData(context);
    if (module != null && !DumbService.isDumb(module.getProject())) {
      final List<BpmnDomModel> models = BpmnDomModelManager.getInstance(module.getProject()).getAllModels(module);
      if (!models.isEmpty()) {
        return new BpmnModuleWrapper(module);
      }
    }
    return null;
  }

  @Override
  public boolean isAcceptableAsNode(@Nullable Object element) {
    return element instanceof BpmnElementWrapper;
  }

  @Override
  public @Nullable String getElementTitle(BpmnElementWrapper element) {
    return element.isValid() ? element.getName() : BpmnDiagramPresentationConstants.getLabelInvalid();
  }

  @Override
  public SimpleColoredText getItemName(@Nullable Object element, @NotNull DiagramState presentation) {
    if (element instanceof Bpmn20DomElementWrapper wrapper) {
      if (!wrapper.isValid()) {
        return BpmnDiagramPresentationConstants.getInvalidSimpleColoredText();
      }

      SimpleColoredText customText = createCustomNodePresentableName(wrapper);
      if (customText != null) {
        return customText;
      }

      final boolean isStartAction = wrapper.getUserData(Bpmn20DomElementWrapper.IS_START_STATE) == Boolean.TRUE;
      return new SimpleColoredText(wrapper.getName(),
                                   isStartAction
                                   ? SimpleTextAttributes.LINK_BOLD_ATTRIBUTES
                                   : SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }

    if (element instanceof DomElement domElement) {
      final String name = getDomElementPresentableName(domElement);
      return new SimpleColoredText(name, DEFAULT_TEXT_ATTR);
    }

    if (element instanceof SimpleColoredText) {
      return (SimpleColoredText)element;
    }

    if (element instanceof BpmnUnknownNodeElementWrapper) {
      return new SimpleColoredText(((BpmnUnknownNodeElementWrapper)element).getName(), SimpleTextAttributes.ERROR_ATTRIBUTES);
    }

    return new SimpleColoredText("???" + element + "???", SimpleTextAttributes.ERROR_ATTRIBUTES);
  }

  @Override
  public @Nullable @Nls String getNodeTooltip(BpmnElementWrapper element) {
    final Object o = element.getElement();
    if (o instanceof DomElement domElement) {
      if (!domElement.isValid()) {
        return BpmnDiagramPresentationConstants.getLabelInvalid();
      }
      return domElement.getPresentation().getTypeName();
    }
    return element.getName();
  }

  @Override
  public @Nullable Icon getItemIcon(@Nullable Object element, @NotNull DiagramState presentation) {
    if (element instanceof SimpleColoredText) {
      return EmptyIcon.ICON_0;
    }

    if (element instanceof DomElement domElement) {
      if (!domElement.isValid()) {
        return PlatformIcons.ERROR_INTRODUCTION_ICON;
      }
      final Icon icon = domElement.getPresentation().getIcon();
      return icon != null ? icon : EmptyIcon.ICON_16;
    }
    return super.getItemIcon(element, presentation);
  }

  @Override
  public @Nullable SimpleColoredText getItemType(@Nullable Object element) {
    if (element instanceof DomElement &&
        (!((DomElement)element).isValid())) {
      return BpmnDiagramPresentationConstants.getInvalidSimpleColoredText();
    }
    return super.getItemType(element);
  }

  @Nls
  @Override
  public String getEditorTitle(BpmnElementWrapper<?> element, @NotNull Collection<BpmnElementWrapper<?>> additionalElements) {
    return element.getName();
  }

  @Nullable
  private static SimpleColoredText createCustomNodePresentableName(Bpmn20DomElementWrapper wrapper) {
    return null;
  }

  @NotNull
  @Nls
  private static String getDomElementPresentableName(DomElement domElement) {
    if (!domElement.isValid()) {
      return BpmnDiagramPresentationConstants.getLabelInvalid();
    }

    String presentationName = domElement.getPresentation().getElementName();
    if (presentationName != null) {
      return presentationName;
    }

    return JpdlBundle.message("jpdl.label.unknown", domElement.getClass());
  }
}