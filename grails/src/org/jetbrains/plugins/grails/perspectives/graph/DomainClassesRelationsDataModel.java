// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.perspectives.graph;

import com.intellij.CommonBundle;
import com.intellij.openapi.graph.builder.GraphDataModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.perspectives.create.CreateNewRelation;
import org.jetbrains.plugins.grails.references.domain.DomainClassUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DomainClassesRelationsDataModel extends GraphDataModel<DomainClassNode, DomainClassRelationsInfo> {
  private Set<DomainClassNode> myNodes = new HashSet<>();
  private Set<DomainClassRelationsInfo> myEdges = new HashSet<>();

  private final Project myProject;
  private final VirtualFile myDomainDirectory;

  private Map<DomainClassNode, List<DomainClassRelationsInfo>> myNodesToOutsMap;

  public DomainClassesRelationsDataModel(@Nullable VirtualFile domainDirectory, Project project) {
    myDomainDirectory = domainDirectory;
    myProject = project;
  }

  public Map<DomainClassNode, List<DomainClassRelationsInfo>> getNodesToOutsMap() {
    return myNodesToOutsMap;
  }

  @Override
  public @NotNull Collection<DomainClassNode> getNodes() {
    refreshDataModel();
    return myNodes;
  }

  public void refreshDataModel() {
    clearAll();

    updateDataModel();
  }

  private void updateDataModel() {
    myNodesToOutsMap = DomainClassUtils.buildNodesAndEdges(myProject, myDomainDirectory);
    myNodes = myNodesToOutsMap.keySet();
    myEdges = new HashSet<>(ContainerUtil.flatten(myNodesToOutsMap.values()));
  }

  private void clearAll() {
    myNodes.clear();
    myEdges.clear();
  }

  @Override
  public @NotNull Collection<DomainClassRelationsInfo> getEdges() {
    refreshDataModel();

    return myEdges;
  }

  @Override
  public @NotNull DomainClassNode getSourceNode(DomainClassRelationsInfo domainClassRelationsInfo) {
    return domainClassRelationsInfo.getSource();
  }

  @Override
  public @NotNull DomainClassNode getTargetNode(DomainClassRelationsInfo domainClassRelationsInfo) {
    return domainClassRelationsInfo.getTarget();
  }

  @Override
  public @NotNull String getNodeName(DomainClassNode domainClassNode) {
    return domainClassNode.getUniqueName();
  }

  @Override
  public @NotNull String getEdgeName(DomainClassRelationsInfo domainClassRelationsInfo) {
    return domainClassRelationsInfo.getEdgeLabel();
  }

  @Override
  public DomainClassRelationsInfo createEdge(@NotNull DomainClassNode source, @NotNull DomainClassNode target) {
    final PsiClass targetClass = target.getTypeDefinition();
    final String targetQualifiedName = targetClass.getQualifiedName();

    final PsiClass psiClass = source.getTypeDefinition();
    final String sourceQualifiedName = psiClass.getQualifiedName();

    if (sourceQualifiedName != null && sourceQualifiedName.contains(".") && targetQualifiedName != null && !targetQualifiedName.contains(".")) {
      final int exitCode = Messages.showDialog(myProject,
                                               GrailsBundle.message("destination.class.cannot.be.resolved"),
                                               GrailsBundle.message("Warning"),
                                               new String[]{
                                                 CommonBundle.message("button.ok"),
                                                 CommonBundle.message("button.cancel")
                                               },
                                               1,
                                               Messages.getWarningIcon());

      if (exitCode == DialogWrapper.CANCEL_EXIT_CODE) return null;
    }

    CreateNewRelation dialogWrapper = new CreateNewRelation(source, target, myProject);
    dialogWrapper.show();

    DomainClassRelationsInfo.Relation relationType = dialogWrapper.getEdgeRelationType();
    if (relationType != null && DomainClassRelationsInfo.Relation.UNKNOWN != relationType) {
      myEdges.add(new DomainClassRelationsInfo(source, target, relationType));
    }

    return new DomainClassRelationsInfo(source, target, relationType);
  }

  @Override
  public void dispose() {
  }

  public Project getProject() {
    return myProject;
  }
}
