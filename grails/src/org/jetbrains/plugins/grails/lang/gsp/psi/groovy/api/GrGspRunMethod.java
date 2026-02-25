// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api;

import com.intellij.psi.PsiElement;

public interface GrGspRunMethod extends PsiElement {

  GrGspRunBlock getRunBlock();

}
