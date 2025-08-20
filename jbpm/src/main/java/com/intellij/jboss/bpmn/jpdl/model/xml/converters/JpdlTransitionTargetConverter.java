package com.intellij.jboss.bpmn.jpdl.model.xml.converters;

import com.intellij.jboss.bpmn.jpdl.model.JpdlDomModelManager;
import com.intellij.jboss.bpmn.jpdl.model.JpdlModel;
import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlNamedActivity;
import com.intellij.jboss.bpmn.jpdl.model.xml.ProcessDefinition;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiElement;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomTarget;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JpdlTransitionTargetConverter extends ResolvingConverter<JpdlNamedActivity> {

  @Override
  @NotNull
  public Collection<? extends JpdlNamedActivity> getVariants(final ConvertContext context) {
    return getAllNamedActivities(getProcessDefinition(context));
  }

  @Override
  public PsiElement getPsiElement(@Nullable JpdlNamedActivity resolvedValue) {
    return PomService.convertToPsi(DomTarget.getTarget(resolvedValue));
  }

  @Override
  public JpdlNamedActivity fromString(@Nullable @NonNls final String s, final ConvertContext context) {
    if (s == null) return null;

    final ProcessDefinition processDefinition = getProcessDefinition(context);

    for (JpdlNamedActivity namedElement : getAllNamedActivities(processDefinition)) {
      if (s.equals(namedElement.getName().getStringValue())) {
        return namedElement;
      }
    }

    return null;
  }

  @Override
  public String toString(@Nullable final JpdlNamedActivity namedActivity, final ConvertContext context) {
    return namedActivity == null ? null : namedActivity.getName().getStringValue();
  }

  @Nullable
  private static ProcessDefinition getProcessDefinition(final ConvertContext context) {
    final JpdlModel model = JpdlDomModelManager.getInstance(context.getFile().getProject()).getJpdlModel(context.getFile());

    if (model == null || model.getRoots().size() != 1) return null;

    return model.getRoots().get(0).getRootElement();
  }

  private static List<JpdlNamedActivity> getAllNamedActivities(@Nullable final ProcessDefinition processDefinition) {
    List<JpdlNamedActivity> elements = new ArrayList<>();

    if (processDefinition != null) {
      // don't use DomElementVisitor, - performance
      elements.addAll(processDefinition.getCustoms());
      elements.addAll(processDefinition.getDecisions());
      elements.addAll(processDefinition.getEnds());
      elements.addAll(processDefinition.getEndCancels());
      elements.addAll(processDefinition.getEndErrors());
      elements.addAll(processDefinition.getForks());
      elements.addAll(processDefinition.getGroups());
      elements.addAll(processDefinition.getHqls());
      elements.addAll(processDefinition.getSqls());
      elements.addAll(processDefinition.getJavas());
      elements.addAll(processDefinition.getJmses());
      elements.addAll(processDefinition.getJoins());
      elements.addAll(processDefinition.getMails());
      elements.addAll(processDefinition.getRules());
      elements.addAll(processDefinition.getRulesDecisions());
      elements.addAll(processDefinition.getScripts());
      elements.addAll(processDefinition.getSqls());
      elements.addAll(processDefinition.getStarts());
      elements.addAll(processDefinition.getStates());
      elements.addAll(processDefinition.getSubProcesses());
      elements.addAll(processDefinition.getTasks());
      elements.addAll(processDefinition.getCustoms());
    }

    return elements;
  }
}
