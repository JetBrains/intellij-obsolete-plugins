package com.intellij.seam.pages.el;

import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.el.SeamElFileProvider;
import com.intellij.seam.pages.xml.PagesDomModelManager;
import com.intellij.seam.pages.xml.pages.Page;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author Serega.Vasiliev
 */
public class SeamPagesElFileProvider implements SeamElFileProvider {
  @Override
  public boolean isSeamElContainer(@NotNull XmlFile xmlFile) {
    Project project = xmlFile.getProject();
    return DomManager.getDomManager(project).getFileElement(xmlFile, Page.class) != null ||
           PagesDomModelManager.getInstance(project).isPages(xmlFile);
  }
}
