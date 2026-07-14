// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.constants;

public final class GuiceClasses {

  private GuiceClasses() {
  }

  public static final String ABSTRACT_MODULE = "com.google.inject.AbstractModule";

  public static final String PROVIDER = "com.google.inject.Provider";
  public static final String JAVAX_PROVIDER = "javax.inject.Provider";
  public static final String JAKARTA_PROVIDER = "jakarta.inject.Provider";
  public static final String CHECKED_PROVIDER = "com.google.inject.throwingproviders.CheckedProvider";
  public static final java.util.Collection<String> PROVIDERS = java.util.List.of(PROVIDER, JAVAX_PROVIDER, JAKARTA_PROVIDER, CHECKED_PROVIDER);

  public static final String SCOPED_BINDING_BUILDER = "com.google.inject.binder.ScopedBindingBuilder";
  public static final String LINKED_BINDING_BUILDER = "com.google.inject.binder.LinkedBindingBuilder";
}
