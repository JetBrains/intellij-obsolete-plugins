// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.structure;

import com.intellij.util.messages.Topic;

public interface GrailsApplicationListener {

  Topic<GrailsApplicationListener> TOPIC = Topic.create("Grails Applications Events", GrailsApplicationListener.class);

  void applicationsRecomputed();
}
