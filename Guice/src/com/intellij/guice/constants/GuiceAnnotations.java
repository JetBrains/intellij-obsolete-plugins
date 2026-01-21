// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.constants;

import java.util.Collection;
import java.util.List;


public final class GuiceAnnotations {

  private GuiceAnnotations() {
  }

  public static final String INJECT = "com.google.inject.Inject";
  public static final String JAVAX_INJECT = "javax.inject.Inject";
  public static final String JAKARTA_INJECT = "jakarta.inject.Inject";
  public static final String PROVIDES = "com.google.inject.Provides";
  public static final String BINDING_ANNOTATION = "com.google.inject.BindingAnnotation";
  public static final String PROVIDED_BY = "com.google.inject.ProvidedBy";
  public static final String IMPLEMENTED_BY = "com.google.inject.ImplementedBy";
  public static final String TRANSACTIONAL = "com.google.inject.persist.Transactional";

  public static final String NAMED = "com.google.inject.name.Named";
  public static final String ASSISTED = "com.google.inject.assistedinject.Assisted";
  public static final String ASSISTED_INJECT = "com.google.inject.assistedinject.AssistedInject";

  public static final Collection<String> INJECTS = List.of(INJECT, JAVAX_INJECT, JAKARTA_INJECT);
}
