package com.intellij.restClient;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xml.NanoXmlUtil;
import com.intellij.util.xml.XmlFileHeader;
import com.intellij.httpClient.execution.RestClientSerializer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author yole
 */
public class RunRequestAction extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile vFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (project != null && vFile != null) {
      RESTClient client = CreateRestClientAction.openRestClient(project);
      client.importRequest(vFile);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    e.getPresentation().setEnabledAndVisible(isSingleRestClientRequest(files));
  }

  private static boolean isSingleRestClientRequest(VirtualFile[] files) {
    if (files == null || files.length != 1) {
      return false;
    }
    VirtualFile file = files[0];
    if (file.getFileType() != XmlFileType.INSTANCE) {
      return false;
    }
    try {
      XmlFileHeader header = NanoXmlUtil.parseHeaderWithException(file);
      return RestClientSerializer.REST_CLIENT_REQUEST_TAG.equals(header.getRootTagLocalName());
    }
    catch (IOException e) {
      return false;
    }
  }
}
