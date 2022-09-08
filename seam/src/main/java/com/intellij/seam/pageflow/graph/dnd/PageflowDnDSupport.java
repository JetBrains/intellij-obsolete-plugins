package com.intellij.seam.pageflow.graph.dnd;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.graph.builder.dnd.GraphDnDSupport;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.seam.pageflow.SeamPageflowIcons;
import com.intellij.seam.pageflow.graph.PageflowDataModel;
import com.intellij.seam.pageflow.graph.PageflowNode;
import com.intellij.seam.pageflow.graph.PageflowNodeType;
import com.intellij.seam.pageflow.graph.impl.*;
import com.intellij.seam.pageflow.model.xml.pageflow.*;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.util.Function;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PageflowDnDSupport implements GraphDnDSupport<PageflowNode, PageflowNodeType> {
  private final PageflowDataModel myDataModel;

  public PageflowDnDSupport(PageflowDataModel dataModel) {
    myDataModel = dataModel;
  }

  @Override
  public Map<PageflowNodeType, Pair<String, Icon>> getDnDActions() {
    LinkedHashMap<PageflowNodeType, Pair<String, Icon>> nodes = new LinkedHashMap<>();

    nodes.put(PageflowNodeType.START_STATE, Pair.create("Start State", SeamPageflowIcons.Start));
    nodes.put(PageflowNodeType.START_PAGE, Pair.create("Start Page", SeamPageflowIcons.StartPage));
    nodes.put(PageflowNodeType.PAGE, Pair.create("Page", SeamPageflowIcons.Page));
    nodes.put(PageflowNodeType.DECISIION, Pair.create("Decision", SeamPageflowIcons.Decision));
    nodes.put(PageflowNodeType.PROCESS_STATE, Pair.create("Process State", SeamPageflowIcons.ProcessState));
    nodes.put(PageflowNodeType.END_STATE, Pair.create("End State", SeamPageflowIcons.End));

    return nodes;
  }

  @Override
  public boolean canStartDragging(final PageflowNodeType pageflowNodeType) {
    final PageflowDefinition pageflowDefinition = getDataModel().getPageflowDefinition();
    if (pageflowDefinition != null) {
      if (pageflowNodeType == PageflowNodeType.START_STATE) {
        return !DomUtil.hasXml(pageflowDefinition.getStartState());
      } else if (pageflowNodeType == PageflowNodeType.START_PAGE) {
        return !DomUtil.hasXml(pageflowDefinition.getStartPage());
      }
    }
    return true;
  }

  @Override
  public PageflowNode drop(final PageflowNodeType pageflowNodeType) {
    final PageflowDefinition pageflowDefinition = getDataModel().getPageflowDefinition();

    switch (pageflowNodeType) {
      case PAGE:
        return startInWCA(getDataModel().getProject(), pageflowDefinition, getDropPageFunction());
      case DECISIION:
        return startInWCA(getDataModel().getProject(), pageflowDefinition, getDropDecisionFunction());
      case START_PAGE:
        return startInWCA(getDataModel().getProject(), pageflowDefinition, getDropStartPageFunction());
      case START_STATE:
        return startInWCA(getDataModel().getProject(), pageflowDefinition, getDropStartStateFunction());
      case PROCESS_STATE:
        return startInWCA(getDataModel().getProject(), pageflowDefinition, getDropProcessStateFunction());
      case END_STATE:
        return startInWCA(getDataModel().getProject(), pageflowDefinition, getDropEndStateFunction());
      default:
        return null;
    }
  }

  private static Function<PageflowDefinition, PageflowNode> getDropEndStateFunction() {
    return pageflowDefinition -> {
      final EndState endState = pageflowDefinition.addEndState();
      endState.getName().setStringValue(getUnknownState());
      return new EndStateNode(getUnknownState(), endState.createStableCopy());
    };
  }

  @NotNull
  @Nls
  private static String getUnknownState() {
    return SeamBundle.message("seam.state.unknown");
  }

  private static Function<PageflowDefinition, PageflowNode> getDropStartStateFunction() {
    return pageflowDefinition -> {
      final StartState startState = pageflowDefinition.getStartState();
      startState.getName().setStringValue(getUnknownState());
      return new StartStateNode(getUnknownState(), startState.createStableCopy());
    };
  }

  private static Function<PageflowDefinition, PageflowNode> getDropStartPageFunction() {
    return pageflowDefinition -> {
      final StartPage startPage = pageflowDefinition.getStartPage();
      startPage.getName().setStringValue(getUnknownState());
      return new PageNode(getUnknownState(), startPage.createStableCopy());
    };
  }

  private static Function<PageflowDefinition, PageflowNode> getDropProcessStateFunction() {
    return pageflowDefinition -> {
      final ProcessState processState = pageflowDefinition.addProcessState();
      processState.getName().setStringValue(getUnknownState());
      return new ProcessStateNode(getUnknownState(), processState.createStableCopy());
    };
  }

  private static Function<PageflowDefinition, PageflowNode> getDropPageFunction() {
    return pageflowDefinition -> {
      final Page page = pageflowDefinition.addPage();
      page.getName().setStringValue(getUnknownState());
      return new PageNode(getUnknownState(), page.createStableCopy());
    };
  }

  private static Function<PageflowDefinition, PageflowNode> getDropDecisionFunction() {
    return pageflowDefinition -> {
      final Decision decision = pageflowDefinition.addDecision();
      decision.getName().setStringValue(getUnknownState());
      return new DecisionNode(getUnknownState(), decision.createStableCopy());
    };
  }

  private static PageflowNode startInWCA(final Project project,
                                         final PageflowDefinition pageflowDefinition,
                                         final Function<? super PageflowDefinition, ? extends PageflowNode> function) {
    return WriteCommandAction.writeCommandAction(project, DomUtil.getFile(pageflowDefinition))
                             .compute(() -> function.fun(pageflowDefinition));

  }

  public PageflowDataModel getDataModel() {
    return myDataModel;
  }


  public List<String> getExistedNodesNames() {
    return myDataModel.getNodes(false).stream()
      .map(PageflowNode::getName)
      .filter(s -> !StringUtil.isEmptyOrSpaces(s))
      .collect(Collectors.toList());
  }
}
