package com.intellij.seam.pageflow.graph;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.graph.builder.GraphDataModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.pageflow.graph.impl.*;
import com.intellij.seam.pageflow.model.xml.PageflowDomModelManager;
import com.intellij.seam.pageflow.model.xml.PageflowModel;
import com.intellij.seam.pageflow.model.xml.pageflow.*;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PageflowDataModel extends GraphDataModel<PageflowNode, PageflowEdge> {
  private final Collection<PageflowNode> myNodes = new HashSet<>();
  private final Collection<PageflowEdge> myEdges = new HashSet<>();

  private final Project myProject;
  private final XmlFile myFile;

  public PageflowDataModel(final XmlFile file) {
    myFile = file;
    myProject = file.getProject();
  }

  public Project getProject() {
    return myProject;
  }

  @Override
  @NotNull
  public Collection<PageflowNode> getNodes() {
    return getNodes(true);
  }

  @NotNull
  public Collection<PageflowNode> getNodes(boolean refresh) {
    if (refresh) refreshDataModel();

    return myNodes;
  }

  @Override
  @NotNull
  public Collection<PageflowEdge> getEdges() {
    return myEdges;
  }

  @Override
  @NotNull
  public PageflowNode getSourceNode(final PageflowEdge pageflowBasicEdge) {
    return pageflowBasicEdge.getSource();
  }

  @Override
  @NotNull
  public PageflowNode getTargetNode(final PageflowEdge pageflowBasicEdge) {
    return pageflowBasicEdge.getTarget();
  }

  @Override
  @NotNull
  public String getNodeName(final PageflowNode pageflowBasicNode) {
    return "";//pageflowBasicNode.getName();
  }

  @Override
  @NotNull
  public String getEdgeName(final PageflowEdge pageflowBasicEdge) {
    return pageflowBasicEdge.getName();
  }

  @Override
  public PageflowEdge createEdge(@NotNull final PageflowNode from, @NotNull final PageflowNode to) {
    final String toName = to.getName();
    final DomElement element = from.getIdentifyingElement();
    if (element instanceof PageflowTransitionHolder) {
      return WriteCommandAction.runWriteCommandAction(myProject, (Computable<PageflowBasicEdge>)() -> {
        final Transition transition = ((PageflowTransitionHolder)element).addTransition();
        transition.getTo().setStringValue(toName);
        return (new PageflowBasicEdge(from, to, transition.createStableCopy(), false));
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
    final PageflowDefinition pageflowDefinition = getPageflowDefinition();

    if (pageflowDefinition == null) return;

    Map<String, List<PageNode>> allPageNodes = getPageNodes(pageflowDefinition);
    Map<String, List<DecisionNode>> allDecisionNodes = getDecisionNodes(pageflowDefinition);
    Map<String, List<EndStateNode>> allEndStates = getEndStateNodes(pageflowDefinition);
    Map<String, List<ProcessStateNode>> allProcessStates = getProcessStateNodes(pageflowDefinition);

    final StartState startState = pageflowDefinition.getStartState();
    if (DomUtil.hasXml(startState)) {
      @NlsSafe String stringValue = startState.getName().getStringValue();
      StartStateNode startStateNode = new StartStateNode(stringValue, startState.createStableCopy());
      addNode(startStateNode);

      addTransitions(startStateNode, startState.getTransitions(), allPageNodes, allDecisionNodes, allEndStates, allProcessStates);
    }

    for (List<PageNode> pageNodes : allPageNodes.values()) {
      for (PageNode pageNode : pageNodes) {
        addTransitions(pageNode, pageNode.getIdentifyingElement().getTransitions(), allPageNodes, allDecisionNodes,  allEndStates, allProcessStates);
      }
    }

    for (List<DecisionNode> decisionNodes : allDecisionNodes.values()) {
      for (DecisionNode decisionNode : decisionNodes) {
        addTransitions(decisionNode, decisionNode.getIdentifyingElement().getTransitions(), allPageNodes, allDecisionNodes, allEndStates,
                       allProcessStates);
      }
    }

    for (List<ProcessStateNode> processStateNodes : allProcessStates.values()) {
      for (ProcessStateNode processStateNode : processStateNodes) {
        addTransitions(processStateNode, processStateNode.getIdentifyingElement().getTransitions(), allPageNodes, allDecisionNodes, allEndStates,
                       allProcessStates);
      }
    }
  }

  private Map<String, List<ProcessStateNode>> getProcessStateNodes(final PageflowDefinition pageflowDefinition) {
    Map<String, List<ProcessStateNode>> processStateNodes = new HashMap<>();

    for (ProcessState processState : pageflowDefinition.getProcessStates()) {
      @NlsSafe String name = processState.getName().getStringValue();

      if (StringUtil.isEmptyOrSpaces(name)) name = SeamBundle.message("seam.page.undefined");
      if (!processStateNodes.containsKey(name)) {
        processStateNodes.put(name, new ArrayList<>());
      }
      final ProcessStateNode node = new ProcessStateNode(name, processState.createStableCopy());

      processStateNodes.get(name).add(node);
      addNode(node);
    }

    return processStateNodes;
  }

  private Map<String, List<EndStateNode>> getEndStateNodes(final PageflowDefinition pageflowDefinition) {
    Map<String, List<EndStateNode>> endStateNodes = new HashMap<>();

    for (EndState endState : pageflowDefinition.getEndStates()) {
      @NlsSafe String name = endState.getName().getStringValue();

      if (StringUtil.isEmptyOrSpaces(name)) name = SeamBundle.message("seam.page.undefined");
      if (!endStateNodes.containsKey(name)) {
        endStateNodes.put(name, new ArrayList<>());
      }
      final EndStateNode node = new EndStateNode(name, endState.createStableCopy());

      endStateNodes.get(name).add(node);
      addNode(node);
    }

    return endStateNodes;
  }

  private void addTransitions(final PageflowNode<? extends DomElement> sourceNode,
                              final List<Transition> transitions,
                              final Map<String, List<PageNode>> allPageNodes,
                              final Map<String, List<DecisionNode>> allDecisionNodes,
                              final Map<String, List<EndStateNode>> allEndStates,
                              final Map<String, List<ProcessStateNode>> allProcessStates) {
    for (Transition transition : transitions) {
      final String targetNodeName = transition.getTo().getStringValue();
      if (StringUtil.isEmptyOrSpaces(targetNodeName)) continue;

      final List<PageNode> targetPageNodes = allPageNodes.get(targetNodeName);
      if (targetPageNodes != null) {
        for (PageNode targetNode : targetPageNodes) {
          addTransition(new PageflowBasicEdge(sourceNode, targetNode, transition.createStableCopy(), (targetPageNodes.size() > 1 ||
                                                                                                      allDecisionNodes.containsKey(targetNodeName) ||
                                                                                                      allEndStates.containsKey(targetNodeName) ||
                                                                                                      allProcessStates.containsKey(targetNodeName) )));
        }
      }

      final List<DecisionNode> targetDecisionNodes = allDecisionNodes.get(targetNodeName);
      if (targetDecisionNodes != null) {
        for (DecisionNode targetNode : targetDecisionNodes) {
          addTransition(new PageflowBasicEdge(sourceNode, targetNode, transition.createStableCopy(), (targetDecisionNodes.size() > 1 ||
                                                                                                      allPageNodes.containsKey(targetNodeName) ||
                                                                                                      allEndStates.containsKey(targetNodeName) ||
                                                                                                      allProcessStates.containsKey(targetNodeName))));
        }
      }

      final List<EndStateNode> targetEndStateNodes = allEndStates.get(targetNodeName);
      if (targetEndStateNodes != null) {
        for (EndStateNode targetNode : targetEndStateNodes) {
          addTransition(new PageflowBasicEdge(sourceNode, targetNode, transition.createStableCopy(), (targetEndStateNodes.size() > 1 ||
                                                                                                      allPageNodes.containsKey(targetNodeName) ||
                                                                                                      allDecisionNodes.containsKey(targetNodeName) ||
                                                                                                      allProcessStates.containsKey(targetNodeName))));
        }
      }

      final List<ProcessStateNode> targetProcessStateNodes = allProcessStates.get(targetNodeName);
      if (targetProcessStateNodes != null) {
        for (ProcessStateNode targetNode : targetProcessStateNodes) {
          addTransition(new PageflowBasicEdge(sourceNode, targetNode, transition.createStableCopy(), (targetProcessStateNodes.size() > 1 ||
                                                                                                      allPageNodes.containsKey(targetNodeName) ||
                                                                                                      allDecisionNodes.containsKey(targetNodeName) ||
                                                                                                      allEndStates.containsKey(targetNodeName))));
        }
      }
    }
  }

  private void addNode(final PageflowNode node) {
    myNodes.add(node);
  }

  private void addTransition(final PageflowEdge edge) {
    myEdges.add(edge);
  }

  private Map<String, List<DecisionNode>> getDecisionNodes(final PageflowDefinition pageflowDefinition) {
    Map<String, List<DecisionNode>> decisions = new HashMap<>();

    for (Decision decision : pageflowDefinition.getDecisions()) {
      @NlsSafe String name = decision.getName().getStringValue();

      if (StringUtil.isEmptyOrSpaces(name)) name = SeamBundle.message("seam.page.undefined");
      if (!decisions.containsKey(name)) {
        decisions.put(name, new ArrayList<>());
      }
      final DecisionNode decisionNode = new DecisionNode(name, decision.createStableCopy());
      addNode(decisionNode);

      decisions.get(name).add(decisionNode);
    }

    return decisions;
  }

  private Map<String, List<PageNode>> getPageNodes(final PageflowDefinition pageflowDefinition) {
    Map<String, List<PageNode>> pages = new HashMap<>();

    for (Page page : pageflowDefinition.getPages()) {
      addPageNode(pages, page);
    }

    addPageNode(pages, pageflowDefinition.getStartPage());

    return pages;
  }

  private void addPageNode(final Map<String, List<PageNode>> pages, final PageElements page) {
    if (!DomUtil.hasXml(page)) return;

    @NlsSafe String name = page.getName().getStringValue();

    if (StringUtil.isEmptyOrSpaces(name)) name = SeamBundle.message("seam.page.undefined");
    if (!pages.containsKey(name)) {
      pages.put(name, new ArrayList<>());
    }
    PageNode pageNode = new PageNode(name, page.createStableCopy());
    pages.get(name).add(pageNode);

    addNode(pageNode);
  }

  @Nullable
  public PageflowDefinition getPageflowDefinition() {
    final PageflowModel model = getModel();
    if (model == null || model.getRoots().size() != 1) return null;

    return model.getRoots().get(0).getRootElement();
  }

  public PageflowModel getModel() {
    return PageflowDomModelManager.getInstance(myProject).getPageflowModel(myFile);
  }
}
