// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner.ui;

import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;

public abstract class GrailsRunConfigurationEditorWithListener
  extends SettingsEditor<GrailsRunConfiguration>
  implements GrailsRunConfigurationEditorListener {
}
