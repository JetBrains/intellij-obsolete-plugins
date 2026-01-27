// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.util.xml.NamedEnum;

public enum BundleAction implements NamedEnum {
	INSTALL ("install"),
	START ("start"),
	STOP ("stop"),
	UNINSTALL ("uninstall"),
	UPDATE ("update");

	private final String value;
	BundleAction(String value) { this.value = value; }

  @Override
  public String getValue() { return value; }

}
