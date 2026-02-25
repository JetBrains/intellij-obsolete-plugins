// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

public final class DataSourcesSupport extends AbstractConfigSupport {

  @Override
  public PropertiesProvider getProvider(@NotNull GroovyFile file) {
    if (GrailsUtils.isConfigFile(file, "DataSource.groovy")) {
      return this;
    }
    return null;
  }

  @Override
  protected String @NotNull [] getFinalProperties() {
    return new String[]{
      "dataSource.pooled",
      "dataSource.driverClassName",
      "dataSource.username",
      "dataSource.password",
      "dataSource.passwordEncryptionCodec",
      "dataSource.dbCreate",
      "dataSource.url",
      "dataSource.loggingSql",
      "dataSource.dialect",
      "dataSource.jndiName"
    };
  }
}
