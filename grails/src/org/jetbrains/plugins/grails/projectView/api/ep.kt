/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.projectView.api

import com.intellij.openapi.extensions.ExtensionPointName

val EP_NAME: ExtensionPointName<GrailsViewNodeProvider> = ExtensionPointName.create("org.intellij.grails.viewNodeProvider")