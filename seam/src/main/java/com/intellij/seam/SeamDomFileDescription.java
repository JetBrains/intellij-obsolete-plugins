package com.intellij.seam;

import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.seam.model.xml.components.SeamComponents;
import com.intellij.util.xml.DomFileDescription;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

final class SeamDomFileDescription extends DomFileDescription<SeamComponents> {
  public SeamDomFileDescription() {
    super(SeamComponents.class, "components");
  }

  @Override
  protected void initializeFileDescription() {
    registerNamespacePolicy(SeamNamespaceConstants.COMPONENTS_NAMESPACE_KEY, SeamNamespaceConstants.COMPONENTS_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.COMPONENTS_NAMESPACE_KEY, SeamNamespaceConstants.COMPONENTS_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.CORE_NAMESPACE_KEY, SeamNamespaceConstants.CORE_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.PERSISTENCE_NAMESPACE_KEY, SeamNamespaceConstants.PERSISTENCE_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.SECURITY_NAMESPACE_KEY, SeamNamespaceConstants.SECURITY_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.THEME_NAMESPACE_KEY, SeamNamespaceConstants.THEME_NAMESPACE);

    registerNamespacePolicy(SeamNamespaceConstants.DROOLS_NAMESPACE_KEY, SeamNamespaceConstants.DROOLS_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.FRAMEWORK_NAMESPACE_KEY, SeamNamespaceConstants.FRAMEWORK_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.JMS_NAMESPACE_KEY, SeamNamespaceConstants.JMS_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.MAIL_NAMESPACE_KEY, SeamNamespaceConstants.MAIL_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.PDF_NAMESPACE_KEY, SeamNamespaceConstants.PDF_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.REMOTING_NAMESPACE_KEY, SeamNamespaceConstants.REMOTING_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.SPRING_NAMESPACE_KEY, SeamNamespaceConstants.SPRING_NAMESPACE);
    registerNamespacePolicy(SeamNamespaceConstants.WEB_NAMESPACE_KEY, SeamNamespaceConstants.WEB_NAMESPACE);
  }

  @NotNull
  @Override
  public List<String> getAllowedNamespaces(@NotNull final String namespaceKey, @NotNull final XmlFile file) {
    final List<String> stringList = super.getAllowedNamespaces(namespaceKey, file);
    return stringList.isEmpty() ? Collections.singletonList(namespaceKey) : stringList;
  }

}
