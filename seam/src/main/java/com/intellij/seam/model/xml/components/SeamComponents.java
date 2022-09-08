package com.intellij.seam.model.xml.components;

import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.seam.model.xml.SeamDomElement;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Namespace(SeamNamespaceConstants.COMPONENTS_NAMESPACE_KEY)
//@DefinesXml
public interface SeamComponents extends SeamDomElement {

  @NotNull
  List<SeamDomComponent> getComponents();

  SeamDomComponent addComponent();

  @NotNull
  List<SeamDomFactory> getFactories();

  SeamDomFactory addFactory();

  @NotNull
  List<SeamEvent> getEvents();

  SeamEvent addEvent();

  @NotNull
  @SubTagList("import")
  List<SeamImport> getImports();

  SeamImport addImport();
}
