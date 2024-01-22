package com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers;

import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModelManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;


public class BpmnDefinitionsWrapper extends BpmnElementWrapper<XmlFile> {

  private static final String PATH = "flowPath";

  public BpmnDefinitionsWrapper(@NotNull XmlFile file) {
    super(file);
  }

  @NotNull
  @Override
  public String getName() {
    return myElement.getName();
  }

  @Override
  public String getFqn() {
    final VirtualFile virtualFile = myElement.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    return PATH + VALUE_DELIMITER + virtualFile.getPath();
  }

  @NotNull
  @Override
  public List<BpmnDomModel> getBpmnModels() {
    if (!myElement.isValid()) {
      return Collections.emptyList();
    }

    final BpmnDomModel model = BpmnDomModelManager.getInstance(myElement.getProject()).getModel(myElement);
    assert model != null : myElement;
    return Collections.singletonList(model);
  }

  @Nullable
  public static BpmnElementWrapper resolveElementByFQN(String fqn, Project project) {
    final String path = split(fqn).get(PATH);
    final XmlFile file = findFile(project, path);
    if (file == null) {
      return null;
    }
    return new BpmnDefinitionsWrapper(file);
  }

  @Nullable
  private static XmlFile findFile(@NotNull Project project, @Nullable String path) {
    if (!StringUtil.isEmptyOrSpaces(path)) {
      final VirtualFile relativeFile = VfsUtilCore.findRelativeFile(path, null);
      if (relativeFile != null) {
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(relativeFile);
        if (psiFile instanceof XmlFile) {
          return (XmlFile)psiFile;
        }
      }
    }
    return null;
  }
}