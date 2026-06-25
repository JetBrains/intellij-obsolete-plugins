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
  public static final String THROWING_INJECT = "com.google.inject.throwingproviders.ThrowingInject";
  public static final String PROVIDES = "com.google.inject.Provides";
  public static final String CHECKED_PROVIDES = "com.google.inject.throwingproviders.CheckedProvides";
  public static final String PROVIDES_INTO_SET = "com.google.inject.multibindings.ProvidesIntoSet";
  public static final String CHECKED_PROVIDES_INTO_SET = "com.google.inject.multibindings.CheckedProvidesIntoSet";
  public static final String PROVIDES_INTO_MAP = "com.google.inject.multibindings.ProvidesIntoMap";
  public static final String CHECKED_PROVIDES_INTO_MAP = "com.google.inject.multibindings.CheckedProvidesIntoMap";

  /** Base {@code @Provides} annotation (strategy EPs contribute additional variants). */
  public static final Collection<String> PROVIDES_ANNOTATIONS = List.of(PROVIDES);

  public static final String BINDING_ANNOTATION = "com.google.inject.BindingAnnotation";
  public static final String JAVAX_QUALIFIER = "javax.inject.Qualifier";
  public static final String JAKARTA_QUALIFIER = "jakarta.inject.Qualifier";
  public static final List<String> BINDING_ANNOTATIONS = List.of(BINDING_ANNOTATION, JAVAX_QUALIFIER, JAKARTA_QUALIFIER);

  public static final String PROVIDED_BY = "com.google.inject.ProvidedBy";
  public static final String IMPLEMENTED_BY = "com.google.inject.ImplementedBy";
  public static final String TRANSACTIONAL = "com.google.inject.persist.Transactional";

  public static final String NAMED = "com.google.inject.name.Named";
  public static final String JAVAX_NAMED = "javax.inject.Named";
  public static final String JAKARTA_NAMED = "jakarta.inject.Named";
  public static final Collection<String> NAMEDS = List.of(NAMED, JAVAX_NAMED, JAKARTA_NAMED);

  public static final String ASSISTED = "com.google.inject.assistedinject.Assisted";
  public static final String ASSISTED_INJECT = "com.google.inject.assistedinject.AssistedInject";

  // Scope annotations
  public static final String GUICE_SINGLETON = "com.google.inject.Singleton";
  public static final String JAVAX_SINGLETON = "javax.inject.Singleton";
  public static final String JAKARTA_SINGLETON = "jakarta.inject.Singleton";
  public static final Collection<String> SINGLETONS = List.of(GUICE_SINGLETON, JAVAX_SINGLETON, JAKARTA_SINGLETON);

  public static final String REQUEST_SCOPED = "com.google.inject.servlet.RequestScoped";
  public static final String SESSION_SCOPED = "com.google.inject.servlet.SessionScoped";

  /** All known scope annotations, grouped by equivalence. */
  public static final List<Collection<String>> SCOPE_GROUPS = List.of(
      SINGLETONS,
      List.of(REQUEST_SCOPED),
      List.of(SESSION_SCOPED)
  );

  public static final Collection<String> INJECTS = List.of(INJECT, JAVAX_INJECT, JAKARTA_INJECT, THROWING_INJECT);
}

