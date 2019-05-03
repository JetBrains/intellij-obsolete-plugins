/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.JavaNameStrategy;
import com.intellij.util.xml.NameStrategyForAttributes;

/**
 * @author Dmitry Avdeev
 */
@NameStrategyForAttributes(JavaNameStrategy.class)
public interface StrutsRootElement extends DomElement {
}
