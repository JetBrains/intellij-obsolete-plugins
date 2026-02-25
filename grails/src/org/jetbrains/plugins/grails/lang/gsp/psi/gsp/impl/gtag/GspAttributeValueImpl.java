// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.psi.impl.source.xml.XmlAttributeValueImpl;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspAttributeValue;

public class GspAttributeValueImpl extends XmlAttributeValueImpl implements GspAttributeValue {

  @Override
  public @NotNull IElementType getElementType() {
    return GspElementTypes.GSP_ATTRIBUTE_VALUE;
  }

}
