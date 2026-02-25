// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner.ui;

import org.jetbrains.plugins.grails.structure.GrailsApplication;

public interface GrailsRunConfigurationEditorListener {

  default void applicationChanged(GrailsApplication application) {}
  default void commandLineChanged(String commandLine) {}

}
