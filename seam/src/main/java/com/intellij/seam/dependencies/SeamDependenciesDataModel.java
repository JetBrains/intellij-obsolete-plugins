package com.intellij.seam.dependencies;

import com.intellij.openapi.graph.builder.GraphDataModel;
import com.intellij.openapi.graph.builder.NodesGroup;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.intellij.seam.dependencies.beans.*;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.SeamJamModel;
import com.intellij.seam.model.jam.SeamJamRole;
import com.intellij.seam.model.jam.bijection.SeamJamBijection;
import com.intellij.seam.model.jam.bijection.SeamJamInjection;
import com.intellij.seam.model.jam.bijection.SeamJamOutjection;
import com.intellij.seam.model.xml.SeamDomModelManager;
import com.intellij.seam.model.xml.components.SeamComponents;
import com.intellij.seam.model.xml.components.SeamDomComponent;
import com.intellij.seam.model.xml.components.SeamDomFactory;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.seam.utils.beans.ContextVariable;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SeamDependenciesDataModel extends GraphDataModel<SeamComponentNodeInfo, SeamDependencyInfo> {
  private final Module myModule;

  public SeamDependenciesDataModel(final Module module) {
    myModule = module;
  }

  private final Collection<SeamComponentNodeInfo> myNodes = new HashSet<>();
  private final Collection<SeamDependencyInfo> myEdges = new HashSet<>();
  protected final Map<PsiFile, NodesGroup> myGroups = new HashMap<>();

  @Override
  @NotNull
  public Collection<SeamComponentNodeInfo> getNodes() {
    return getNodes(true);
  }

  @NotNull
  public Collection<SeamComponentNodeInfo> getNodes(boolean refresh) {
    if (refresh) refreshDataModel();

    return myNodes;
  }

  @Override
  @NotNull
  public Collection<SeamDependencyInfo> getEdges() {
    return myEdges;
  }

  @Override
  @NotNull
  public SeamComponentNodeInfo getSourceNode(final SeamDependencyInfo dependencyInfo) {
    return dependencyInfo.getSource();
  }

  @Override
  @NotNull
  public SeamComponentNodeInfo getTargetNode(final SeamDependencyInfo dependencyInfo) {
    return dependencyInfo.getTarget();
  }

  @Override
  @NotNull
  public String getNodeName(final SeamComponentNodeInfo pageflowBasicNode) {
    return "";
  }

  @Override
  @NotNull
  public String getEdgeName(final SeamDependencyInfo pageflowBasicEdge) {
    return pageflowBasicEdge.getName();
  }

  @Override
  public SeamDependencyInfo createEdge(@NotNull final SeamComponentNodeInfo from, @NotNull final SeamComponentNodeInfo to) {
    return null;
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
    final Set<ContextVariable> contextVariables = SeamCommonUtils.getSeamContextVariablesWithDependencies(myModule);

    final Set<SeamJamComponent> seamJamComponents = SeamJamModel.getModel(myModule).getSeamComponents(true);
    seamJamComponents.addAll(SeamJamModel.getModel(myModule).getMergedComponents(true));

    Map<String, SeamJamComponentNodeInfo> annotatedNodesMap = new HashMap<>();
    Map<String, SeamJamComponentNodeInfo> compiledNodesMap = new HashMap<>();
    Map<String, SeamDomComponentNodeInfo> domNodesMap = new HashMap<>();
    Map<String, UnknownBijectionNodeInfo> unknownNodesMap = new HashMap<>();

    List<Pair<String, PsiType>> jamComponentsInfo = new ArrayList<>();

    for (SeamJamComponent component : seamJamComponents) {
      final String componentName = component.getComponentName();
      final PsiClass psiClass = component.getPsiElement();

      if (!StringUtil.isEmptyOrSpaces(componentName)) {
        final SeamJamComponentNodeInfo nodeInfo = new SeamJamComponentNodeInfo(component);

        if (psiClass instanceof PsiCompiledElement) {
          compiledNodesMap.put(componentName, nodeInfo);
        }
        else {
          annotatedNodesMap.put(componentName, nodeInfo);
          myNodes.add(nodeInfo);
        }

        jamComponentsInfo.add(Pair.create(componentName, component.getComponentType()));
      }
    }

    Map<String, SeamDomFactory> domFactoriesMap = new HashMap<>();

    for (SeamComponents model : SeamDomModelManager.getInstance(myModule.getProject()).getAllModels(myModule)) {
      for (SeamDomComponent seamDomComponent : DomUtil.getDefinedChildrenOfType(model, SeamDomComponent.class)) {
        final String name = seamDomComponent.getComponentName();
        final PsiType type = seamDomComponent.getComponentType();

        if (!StringUtil.isEmptyOrSpaces(name) && !jamComponentsInfo.contains(Pair.create(name, type))) {
          SeamDomComponentNodeInfo nodeInfo = new SeamDomComponentNodeInfo(seamDomComponent);
          domNodesMap.put(name, nodeInfo);
          myNodes.add(nodeInfo);
        }
      }

      for (SeamDomFactory domFactory : model.getFactories()) {
        String factoryName = domFactory.getFactoryName();
        if (!StringUtil.isEmptyOrSpaces(factoryName)) {
          domFactoriesMap.put(factoryName, domFactory);
        }
      }
    }


    for (SeamJamComponentNodeInfo nodeInfo : annotatedNodesMap.values()) {
      for (SeamJamInjection injection : nodeInfo.getIdentifyingElement().getInjections()) {
        SeamComponentNodeInfo source =
          getNodeByComponentName(annotatedNodesMap, compiledNodesMap, domNodesMap, unknownNodesMap, injection, domFactoriesMap);

        if (source != null) {
          myEdges.add(new BasicSeamDependencyInfo(source, nodeInfo, "@In", injection));
        }
      }
      for (SeamJamOutjection outjection : nodeInfo.getIdentifyingElement().getOutjections()) {
        SeamComponentNodeInfo target =
          getNodeByComponentName(annotatedNodesMap, compiledNodesMap, domNodesMap, unknownNodesMap, outjection, domFactoriesMap);

        if (target != null) {
          myEdges.add(new BasicSeamDependencyInfo(nodeInfo, target, "@Out", outjection));
        }
      }
    }
  }

  @Nullable
  private SeamComponentNodeInfo getNodeByComponentName(final Map<String, SeamJamComponentNodeInfo> nodesMap,
                                                       final Map<String, SeamJamComponentNodeInfo> compiledNodesMap,
                                                       final Map<String, SeamDomComponentNodeInfo> domNodesMap,
                                                       final Map<String, UnknownBijectionNodeInfo> unknownNodesMap,
                                                       final SeamJamBijection bijection,
                                                       Map<String, SeamDomFactory> domFactoriesMap) {
    String name = bijection.getName();
    if (StringUtil.isEmptyOrSpaces(name)) return null;

    SeamComponentNodeInfo componentNodeInfo = findNodeByName(name, nodesMap, domNodesMap, compiledNodesMap, domFactoriesMap);

    return componentNodeInfo == null ? getOrCreateUnknownNode(bijection, name, unknownNodesMap) : componentNodeInfo;
  }

  @Nullable
  private SeamComponentNodeInfo findNodeByName(@NotNull String name,
                                               Map<String, SeamJamComponentNodeInfo> nodesMap,
                                               Map<String, SeamDomComponentNodeInfo> domNodesMap,
                                               Map<String, SeamJamComponentNodeInfo> compiledNodesMap,
                                               Map<String, SeamDomFactory> domFactoriesMap) {
       return findNodeByName(name, nodesMap, domNodesMap, compiledNodesMap,domFactoriesMap, new HashSet<>());
  }
  @Nullable
  private SeamComponentNodeInfo findNodeByName(@NotNull String name,
                                               Map<String, SeamJamComponentNodeInfo> nodesMap,
                                               Map<String, SeamDomComponentNodeInfo> domNodesMap,
                                               Map<String, SeamJamComponentNodeInfo> compiledNodesMap,
                                               Map<String, SeamDomFactory> domFactoriesMap, Set<String> processedNames) {
    if (nodesMap.containsKey(name)) return nodesMap.get(name);

    for (SeamJamComponentNodeInfo info : nodesMap.values()) {
      for (SeamJamRole role : info.getIdentifyingElement().getRoles()) {
        if (name.equals(role.getName())) return info;
      }
    }

    if (domNodesMap.containsKey(name)) return domNodesMap.get(name);

    if (compiledNodesMap.containsKey(name)) {
      SeamJamComponentNodeInfo source = compiledNodesMap.get(name);
      myNodes.add(source); // compiled components are hidden by default
      return source;
    }

    if (domFactoriesMap.containsKey(name)) {
      String aliasedVarName = SeamCommonUtils.getFactoryAliasedVarName(domFactoriesMap.get(name));
      if (!StringUtil.isEmptyOrSpaces(aliasedVarName) && !processedNames.contains(aliasedVarName)) {
        processedNames.add(name);
        return findNodeByName(aliasedVarName, nodesMap, domNodesMap, compiledNodesMap, domFactoriesMap, processedNames);
      }
    }

    return null;
  }

  private SeamComponentNodeInfo getOrCreateUnknownNode(final SeamJamBijection bijection,
                                                       final String name,
                                                       final Map<String, UnknownBijectionNodeInfo> unknownNodesMap) {
    if (unknownNodesMap.containsKey(name)) return unknownNodesMap.get(name);

    final UnknownBijectionNodeInfo unknownBijectionNodeInfo = new UnknownBijectionNodeInfo(bijection);

    unknownNodesMap.put(name, unknownBijectionNodeInfo);

    myNodes.add(unknownBijectionNodeInfo);

    return unknownBijectionNodeInfo;
  }

  @Override
  public void dispose() {

  }
}
