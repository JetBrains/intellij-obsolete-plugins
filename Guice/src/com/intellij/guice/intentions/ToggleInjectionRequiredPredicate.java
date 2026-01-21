// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;

public class ToggleInjectionRequiredPredicate implements PsiElementPredicate{
    @Override
    public boolean satisfiedBy(PsiElement element){
        if(!(element instanceof PsiAnnotation)){
            return false;
        }
        final String name = ((PsiAnnotation) element).getQualifiedName();
        return name != null && GuiceAnnotations.INJECTS.contains(name);
    }
}
