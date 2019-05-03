/*
 * Copyright (c) 2005 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.designer.inspector;

/**
 * @author spleaner
 */
public interface PropertyValidator<P extends Property> {

  boolean accepts(P property);

}
