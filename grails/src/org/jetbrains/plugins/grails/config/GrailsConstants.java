// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureUtil;

public interface GrailsConstants {

  String GRAILS = "Grails";

  String GRAILS_SDK_KEY = "grails.sdk.home";
  String GRAILS_LAST_SELECTED_SDK = "grails.last.selected.sdk.home";

  String GRAILS_HOME = "GRAILS_HOME";

  @NonNls String APP_DIRECTORY = "grails-app";
  @NonNls String APPLICATION_PROPERTIES = MvcModuleStructureUtil.APPLICATION_PROPERTIES;
}
