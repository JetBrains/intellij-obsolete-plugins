// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.ConcurrencyUtil;
import org.jetbrains.annotations.NotNull;

public abstract class GrailsCachedValue<E extends UserDataHolder, R> {

  private final Key<CachedValue<R>> key = Key.create(toString());

  private final Key dependency;

  protected GrailsCachedValue() {
    this(PsiModificationTracker.MODIFICATION_COUNT);
  }

  protected GrailsCachedValue(Key dependency) {
    this.dependency = dependency;
  }

  public R get(final @NotNull E element) {
    CachedValue<R> cachedValue = ConcurrencyUtil.computeIfAbsent(element, key, () ->
    CachedValuesManager.getManager(getProject(element))
        .createCachedValue(() -> CachedValueProvider.Result.create(calculate(element), dependency), false));

    return cachedValue.getValue();
  }

  protected abstract Project getProject(E element);

  protected abstract R calculate(E element);

}
