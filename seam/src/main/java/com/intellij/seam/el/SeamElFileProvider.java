package com.intellij.seam.el;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Serega.Vasiliev
 */
public interface SeamElFileProvider {
  ExtensionPointName<SeamElFileProvider> EP_NAME = new ExtensionPointName<>("com.intellij.seam.elFileProvider");

  boolean isSeamElContainer(@NotNull XmlFile file);
}
