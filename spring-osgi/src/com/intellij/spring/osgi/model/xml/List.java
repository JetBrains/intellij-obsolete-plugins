// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.psi.CommonClassNames;
import com.intellij.spring.model.xml.BeanType;

@BeanType(CommonClassNames.JAVA_UTIL_LIST)
public interface List extends BaseReferenceCollection {
}
