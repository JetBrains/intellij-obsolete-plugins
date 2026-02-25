// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.structure;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface GrailsSDKListener {

  Topic<GrailsSDKListener> TOPIC = Topic.create("Grails SDK Events", GrailsSDKListener.class);

  void sdkChanged(@NotNull GrailsApplication application);
}
