// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspTag;

public interface GspGrailsTag extends GspTag, GspTaggedElement, XmlTag {

  boolean endsByError();

  PsiElement getNameElement();
}
