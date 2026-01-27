// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.constants;

import org.jetbrains.annotations.NonNls;

public interface SpringOsgiConstants {
  @NonNls String OSGI_NAMESPACE_KEY = "Spring Osgi namespace key";
  @NonNls String OSGI_NAMESPACE = "http://www.springframework.org/schema/osgi";

  @NonNls String OSGI_COMPENDIUM_NAMESPACE_KEY = "Spring Osgi Compendium namespace key";
  @NonNls String OSGI_COMPENDIUM_NAMESPACE = "http://www.springframework.org/schema/osgi-compendium";

  @NonNls String OSGI_SERVICE_FACTORY_BEAN_CLASSNAME = "org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean";
  @NonNls String OSGI_SERVICE_REGISTRATION_CLASSNAME = "org.osgi.framework.ServiceRegistration";

  @NonNls String OSGI_SERVICE_REFERENCE_CLASSNAME = "org.osgi.framework.ServiceReference";

  @NonNls String OSGI_FRAMEWORK_BUNDLE_CLASSNAME = "org.osgi.framework.Bundle";
  @NonNls String OSGI_FRAMEWORK_BUNDLE_CONTEXT = "org.osgi.framework.BundleContext";
  @NonNls String OSGI_SERVICE_LIFECYCLE_LISTENER_CLASSNAME = "org.springframework.osgi.service.importer.OsgiServiceLifecycleListener";
}
