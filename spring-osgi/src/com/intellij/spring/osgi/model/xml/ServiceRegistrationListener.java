// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.psi.PsiMethod;
import com.intellij.spring.osgi.model.converters.RegistrationListenerMethodConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

public interface ServiceRegistrationListener extends BasicListener {

  @NotNull
  @Convert(RegistrationListenerMethodConverter.class)
  GenericAttributeValue<PsiMethod> getRegistrationMethod();

  @NotNull
  @Convert(RegistrationListenerMethodConverter.class)
  GenericAttributeValue<PsiMethod> getUnregistrationMethod();

}
