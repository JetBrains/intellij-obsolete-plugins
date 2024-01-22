package com.intellij.jboss.bpmn.jpdl.graph;

import com.intellij.jboss.bpmn.jpdl.graph.nodes.*;
import com.intellij.jboss.bpmn.jpdl.model.JpdlDomModelManager;
import com.intellij.jboss.bpmn.jpdl.model.JpdlModel;
import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlNamedActivity;
import com.intellij.jboss.bpmn.jpdl.model.xml.ProcessDefinition;
import com.intellij.jboss.bpmn.jpdl.model.xml.Transition;
import com.intellij.jboss.bpmn.jpdl.model.xml.TransitionOwner;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.graph.builder.GraphDataModel;
import com.intellij.openapi.graph.builder.NodesGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.Function;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JpdlDataModel extends GraphDataModel<JpdlNode, JpdlEdge> {
  @NonNls
  private static final String UNDEFINED_NAME = "Undefined";
  protected final Map<PsiFile, NodesGroup> myGroups = new HashMap<>();
  private final Collection<JpdlNode> myNodes = new HashSet<>();
  private final Collection<JpdlEdge> myEdges = new HashSet<>();
  private final Project myProject;
  private final XmlFile myFile;

  public JpdlDataModel(final XmlFile file) {
    myFile = file;
    myProject = file.getProject();
  }

  public Project getProject() {
    return myProject;
  }

  @Override
  @NotNull
  public Collection<JpdlNode> getNodes() {
    return getNodes(true);
  }

  @NotNull
  public Collection<JpdlNode> getNodes(boolean refresh) {
    if (refresh) refreshDataModel();

    return myNodes;
  }

  @Override
  @NotNull
  public Collection<JpdlEdge> getEdges() {
    return myEdges;
  }

  @Override
  @NotNull
  public JpdlNode getSourceNode(final JpdlEdge jpdlBasicEdge) {
    return jpdlBasicEdge.getSource();
  }

  @Override
  @NotNull
  public JpdlNode getTargetNode(final JpdlEdge jpdlBasicEdge) {
    return jpdlBasicEdge.getTarget();
  }

  @Override
  @NotNull
  public String getNodeName(final JpdlNode jpdlBasicNode) {
    return "";//jpdlBasicNode.getName();
  }

  @Override
  @NotNull
  public String getEdgeName(final JpdlEdge jpdlBasicEdge) {
    return jpdlBasicEdge.getName();
  }

  @Override
  public JpdlEdge createEdge(@NotNull final JpdlNode from, @NotNull final JpdlNode to) {
    final String toName = to.getName();
    final DomElement element = from.getIdentifyingElement();
    if (element instanceof TransitionOwner) {
      return WriteCommandAction.writeCommandAction(myProject).compute(() -> {
        final Transition transition = ((TransitionOwner)element).addTransition();
        transition.getTo().setStringValue(toName);
        return new JpdlBasicEdge(from, to, transition.createStableCopy(), false);
      });
    }
    return null;
  }

  @Override
  public void dispose() {
  }


  private void refreshDataModel() {
    clearAll();

    updateDataModel();
  }

  private void clearAll() {
    myNodes.clear();
    myEdges.clear();
  }

  public void updateDataModel() {
    final ProcessDefinition processDefinition = getProcessDefinition();

    if (processDefinition == null) return;

    Map<String, List<JpdlNode>> allNodes = new HashMap<>();

    addActivityNodes(processDefinition.getStarts(), start -> new StartNode(start), allNodes);

    addActivityNodes(processDefinition.getStates(), state -> new StateNode(state), allNodes);

    addActivityNodes(processDefinition.getDecisions(), decision -> new DecisionNode(decision), allNodes);

    addActivityNodes(processDefinition.getTasks(), task -> new TaskNode(task), allNodes);

    addActivityNodes(processDefinition.getForks(), fork -> new ForkNode(fork), allNodes);

    addActivityNodes(processDefinition.getJoins(), join -> new JoinNode(join), allNodes);

    addActivityNodes(processDefinition.getEnds(), end -> new EndActivityNode.EndNode(end), allNodes);

    addActivityNodes(processDefinition.getEndCancels(), end -> new EndActivityNode.EndCancelNode(end), allNodes);

    addActivityNodes(processDefinition.getEndErrors(), endError -> new EndActivityNode.EndErrorNode(endError), allNodes);

    addActivityNodes(processDefinition.getSubProcesses(), subProcess -> new SubProcessNode(subProcess), allNodes);

    addActivityNodes(processDefinition.getJavas(), javaActivity -> new JavaNode(javaActivity), allNodes);

    addActivityNodes(processDefinition.getCustoms(), custom -> new CustomNode(custom), allNodes);

    addActivityNodes(processDefinition.getSqls(), sql -> new SqlNode(sql), allNodes);

    addActivityNodes(processDefinition.getHqls(), hql -> new HqlNode(hql), allNodes);

    addActivityNodes(processDefinition.getRules(), rules -> new RulesNode(rules), allNodes);

    addActivityNodes(processDefinition.getRulesDecisions(), rulesDecision -> new RulesDecisionNode(rulesDecision), allNodes);

    addActivityNodes(processDefinition.getScripts(), script -> new ScriptNode(script), allNodes);

    addActivityNodes(processDefinition.getMails(), mail -> new MailNode(mail), allNodes);

    addActivityNodes(processDefinition.getGroups(), group -> new GroupNode(group), allNodes);


    for (List<? extends JpdlNode> sourceNodes : allNodes.values()) {
      for (JpdlNode sourceNode : sourceNodes) {
        final DomElement identifyingElement = sourceNode.getIdentifyingElement();
        if (identifyingElement instanceof TransitionOwner) {
          final List<Transition> transitions = ((TransitionOwner)identifyingElement).getTransitions();

          addTransitions(sourceNode, transitions, allNodes);
        }
      }
    }
  }

  private void addTransitions(final JpdlNode sourceNode,
                              final List<Transition> transitions,
                              final Map<String, List<JpdlNode>> allNodes) {
    for (Transition transition : transitions) {
      final String targetNodeName = transition.getTo().getStringValue();
      if (StringUtil.isEmptyOrSpaces(targetNodeName)) continue;

      final List<? extends JpdlNode> targetNodes = allNodes.get(targetNodeName);
      if (targetNodes != null) {
        for (JpdlNode targetNode : targetNodes) {
          addTransition(new JpdlBasicEdge(sourceNode, targetNode, transition.createStableCopy(), (targetNodes.size() > 1)));
        }
      }
    }
  }

  private void addNode(final JpdlNode node) {
    myNodes.add(node);
  }

  private void addTransition(final JpdlEdge edge) {
    myEdges.add(edge);
  }

  private <jpdlNode extends JpdlNode, jpdlActivity extends JpdlNamedActivity> void addActivityNodes(final List<jpdlActivity> activities,
                                                                                                    Function<jpdlActivity, jpdlNode> function,
                                                                                                    Map<String, List<JpdlNode>> nodes) {
    for (jpdlActivity activity : activities) {
      if (!DomUtil.hasXml(activity)) continue;

      String name = activity.getName().getStringValue();

      if (StringUtil.isEmptyOrSpaces(name)) name = UNDEFINED_NAME;
      if (!nodes.containsKey(name)) {
        nodes.put(name, new ArrayList<>());
      }
      JpdlNode node = function.fun(activity.createStableCopy());

      nodes.get(name).add(node);

      addNode(node);
    }
  }

  @Nullable
  public ProcessDefinition getProcessDefinition() {
    final JpdlModel model = getModel();
    if (model == null || model.getRoots().size() != 1) return null;

    return model.getRoots().get(0).getRootElement();
  }

  public JpdlModel getModel() {
    return JpdlDomModelManager.getInstance(myProject).getJpdlModel(myFile);
  }
}
