// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.psi.PsiElement;

public interface PsiElementPredicate{
    boolean satisfiedBy(PsiElement element);
}
