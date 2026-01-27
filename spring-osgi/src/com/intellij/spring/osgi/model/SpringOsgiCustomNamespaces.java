// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model;

import com.intellij.spring.dom.CustomNamespaceRegistrar;
import com.intellij.spring.dom.SpringCustomNamespaces;
import com.intellij.spring.osgi.constants.SpringOsgiConstants;
import com.intellij.spring.osgi.model.xml.*;
import com.intellij.spring.osgi.model.xml.compendium.PropertyPlaceholder;
import com.intellij.util.xml.reflect.DomExtensionsRegistrar;

public class SpringOsgiCustomNamespaces extends SpringCustomNamespaces {

  @Override
  public NamespacePolicies getNamespacePolicies() {
    return new NamespacePolicies()
      .add(SpringOsgiConstants.OSGI_NAMESPACE_KEY,
           SpringOsgiConstants.OSGI_NAMESPACE)
      .add(SpringOsgiConstants.OSGI_COMPENDIUM_NAMESPACE_KEY,
           SpringOsgiConstants.OSGI_COMPENDIUM_NAMESPACE);
  }

  @Override
  public void registerExtensions(DomExtensionsRegistrar registrar) {
    CustomNamespaceRegistrar.create(registrar, SpringOsgiConstants.OSGI_NAMESPACE_KEY)
      .add("service", Service.class)
      .add("reference", Reference.class)
      .add("list", List.class)
      .add("set", Set.class)
      .add("bundle", Bundle.class);

    //registrar.registerAttributeChildExtension(new XmlName("default-timeout", SpringOsgiConstants.OSGI_NAMESPACE_KEY), DefaultTimeout.class  );

    //todo register all spring-osgi-compendium.xsd extensions
    CustomNamespaceRegistrar.create(registrar, SpringOsgiConstants.OSGI_COMPENDIUM_NAMESPACE_KEY)
      .add("property-placeholder", PropertyPlaceholder.class);
  }
}
