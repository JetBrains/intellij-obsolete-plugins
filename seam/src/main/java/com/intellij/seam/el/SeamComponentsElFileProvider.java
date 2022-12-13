package com.intellij.seam.el;

import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.model.xml.SeamDomModelManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author Serega.Vasiliev
 */
public class SeamComponentsElFileProvider implements SeamElFileProvider {
  @Override
  public boolean isSeamElContainer(@NotNull XmlFile file) {
    return SeamDomModelManager.getInstance(file.getProject()).isSeamComponents(file);
  }
}
